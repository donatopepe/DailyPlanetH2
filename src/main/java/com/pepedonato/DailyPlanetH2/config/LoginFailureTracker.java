package com.pepedonato.DailyPlanetH2.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limitatore locale e bounded dei tentativi di login.
 *
 * <p>La chiave è la coppia indirizzo remoto + username normalizzato, così non si blocca un
 * account globale e non si rivela se il nome utente esiste. Al superamento della soglia la
 * chiave è temporaneamente sospesa. Le entry scadute vengono eliminate e il numero massimo
 * di entry è limitato per evitare crescita incontrollata della memoria.</p>
 *
 * <p>Per installazioni multi-replica sostituire questo componente in-memory con uno store
 * condiviso (Redis/API gateway); la protezione non è un sostituto del rate limiting a livello
 * di reverse proxy/WAF.</p>
 */
@Component
public class LoginFailureTracker {

    public static final int DEFAULT_MAX_FAILURES = 8;
    public static final int DEFAULT_MAX_ENTRIES = 10_000;
    public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);
    public static final Duration DEFAULT_BLOCK = Duration.ofMinutes(15);

    private final Map<String, FailureState> failures = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxFailures;
    private final int maxEntries;
    private final Duration window;
    private final Duration blockDuration;

    @org.springframework.beans.factory.annotation.Autowired
    public LoginFailureTracker(
            @Value("${dailyplanet.security.login-rate-limit.max-failures:8}") int maxFailures,
            @Value("${dailyplanet.security.login-rate-limit.max-entries:10000}") int maxEntries,
            @Value("${dailyplanet.security.login-rate-limit.window:PT15M}") Duration window,
            @Value("${dailyplanet.security.login-rate-limit.block-duration:PT15M}") Duration blockDuration) {
        this(Clock.systemUTC(), maxFailures, maxEntries, window, blockDuration);
    }

    LoginFailureTracker(Clock clock, int maxFailures, int maxEntries, Duration window, Duration blockDuration) {
        if (maxFailures < 1 || maxEntries < 1 || window.isNegative() || window.isZero()
                || blockDuration.isNegative() || blockDuration.isZero()) {
            throw new IllegalArgumentException("Parametri rate limit non validi");
        }
        this.clock = clock;
        this.maxFailures = maxFailures;
        this.maxEntries = maxEntries;
        this.window = window;
        this.blockDuration = blockDuration;
    }

    /** Registra un fallimento; ritorna true se la chiave è stata (o era già) bloccata. */
    public boolean registerFailure(String key) {
        Instant now = clock.instant();
        cleanup(now);
        FailureState state = failures.compute(key, (ignored, current) -> {
            if (current == null || current.windowStarted.plus(window).isBefore(now)) {
                current = new FailureState(now);
            }
            if (current.blockedUntil != null && current.blockedUntil.isAfter(now)) {
                return current;
            }
            current.failures++;
            if (current.failures >= maxFailures) {
                current.blockedUntil = now.plus(blockDuration);
            }
            current.lastAttempt = now;
            return current;
        });
        trimIfNeeded();
        return state.blockedUntil != null && state.blockedUntil.isAfter(now);
    }

    public boolean isBlocked(String key) {
        Instant now = clock.instant();
        FailureState state = failures.get(key);
        return state != null && state.blockedUntil != null && state.blockedUntil.isAfter(now);
    }

    /** Secondi interi da attendere, oppure 0 se la chiave non è bloccata. */
    public long retryAfterSeconds(String key) {
        FailureState state = failures.get(key);
        if (state == null || state.blockedUntil == null) {
            return 0;
        }
        long seconds = Duration.between(clock.instant(), state.blockedUntil).toSeconds();
        return Math.max(0, seconds + 1);
    }

    /** Un login riuscito azzera soltanto la coppia IP+username corrispondente. */
    public void reset(String key) {
        failures.remove(key);
    }

    int size() {
        return failures.size();
    }

    private void cleanup(Instant now) {
        Instant staleBefore = now.minus(window).minus(blockDuration);
        failures.entrySet().removeIf(entry -> entry.getValue().lastAttempt.isBefore(staleBefore));
    }

    private void trimIfNeeded() {
        if (failures.size() <= maxEntries) {
            return;
        }
        synchronized (failures) {
            if (failures.size() <= maxEntries) {
                return;
            }
            Iterator<Map.Entry<String, FailureState>> iterator = failures.entrySet().iterator();
            while (failures.size() > maxEntries && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    private static final class FailureState {
        private final Instant windowStarted;
        private volatile Instant lastAttempt;
        private volatile Instant blockedUntil;
        private volatile int failures;

        private FailureState(Instant now) {
            this.windowStarted = now;
            this.lastAttempt = now;
        }
    }
}
