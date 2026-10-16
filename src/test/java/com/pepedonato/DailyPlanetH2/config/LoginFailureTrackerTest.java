package com.pepedonato.DailyPlanetH2.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Verifica soglia, decadimento, reset e limite di memoria del rate limiter. */
class LoginFailureTrackerTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private static final class MutableClock extends Clock {
        private Instant now;
        private MutableClock(Instant now) { this.now = now; }
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    @DisplayName("La chiave viene bloccata solo dopo la soglia e sbloccata alla scadenza")
    void sogliaEScadenza() {
        MutableClock clock = new MutableClock(START);
        LoginFailureTracker tracker = new LoginFailureTracker(clock, 3, 100, Duration.ofMinutes(10), Duration.ofMinutes(5));
        String key = LoginRateLimitFilter.key("192.0.2.10", "Admin");

        assertFalse(tracker.registerFailure(key));
        assertFalse(tracker.registerFailure(key));
        assertTrue(tracker.registerFailure(key));
        assertTrue(tracker.isBlocked(key));
        assertTrue(tracker.retryAfterSeconds(key) > 0);

        clock.advance(Duration.ofMinutes(6));
        assertFalse(tracker.isBlocked(key));
        assertEquals(0, tracker.retryAfterSeconds(key));
    }

    @Test
    @DisplayName("Reset dopo login valido rimuove la coppia IP+username")
    void reset() {
        MutableClock clock = new MutableClock(START);
        LoginFailureTracker tracker = new LoginFailureTracker(clock, 1, 100, Duration.ofMinutes(10), Duration.ofMinutes(5));
        String key = LoginRateLimitFilter.key("192.0.2.11", "utente");

        assertTrue(tracker.registerFailure(key));
        tracker.reset(key);
        assertFalse(tracker.isBlocked(key));
        assertEquals(0, tracker.size());
    }

    @Test
    @DisplayName("Username è normalizzato; indirizzi IP diversi mantengono bucket distinti")
    void chiaveIsolataPerClient() {
        assertEquals(LoginRateLimitFilter.key("192.0.2.12", "ADMIN"),
                LoginRateLimitFilter.key("192.0.2.12", " admin "));
        assertFalse(LoginRateLimitFilter.key("192.0.2.12", "admin")
                .equals(LoginRateLimitFilter.key("192.0.2.13", "admin")));
    }

    @Test
    @DisplayName("Numero di chiavi in memoria limitato")
    void limiteMemoria() {
        MutableClock clock = new MutableClock(START);
        LoginFailureTracker tracker = new LoginFailureTracker(clock, 10, 3, Duration.ofMinutes(10), Duration.ofMinutes(5));
        for (int i = 0; i < 12; i++) {
            tracker.registerFailure("192.0.2." + i + "|user" + i);
        }
        assertTrue(tracker.size() <= 3);
    }
}
