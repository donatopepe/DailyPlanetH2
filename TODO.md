# TODO — DailyPlanetH2: da sorgenti legacy a progetto pubblico funzionante

> File unico di pianificazione (regola globale di progetto). Una sola fonte di verità:
> ogni avanzamento aggiorna **Stato**, **Evidenza** e **Blocker** della voce interessata.
> Ultimo aggiornamento: 2026-10-16 (analisi S0).

## Obiettivo (una frase)
Rendere DailyPlanetH2 un'applicazione Spring Boot moderna che compila, si avvia, esegue
i flussi utente/admin end-to-end e si può distribuire pubblicamente (build, guida
completa, README, licenza MIT, test verdi).

## Metrica + soglia
- **Metrica primaria**: criteri di rubrica soddisfatti / criteri totali (rubrica §Rubrica, 12 criteri).
- **Soglia**: 12/12, e nessun criterio parziale.
- **Metrica di supporto (gate dura)**: `./gradlew clean test` verde, 0 failure; avvio applicazione
  con profilo `dev` senza errori; matrice MockMvc §Casi tutta verde.

## Stato attuale (S0 completato — fatti verificati)
| Fatto | Evidenza |
|---|---|
| 24 file Java, 3043 LOC, layout piatto (stile NetBeans) fuori standard Maven/Gradle | `find`, `git show --stat 1aeafc7` |
| Nessun build file, nessun wrapper, nessuna risorsa | `ls`, `find` |
| Nessun template Thymeleaf: 36 rotte e 22 viste/frammenti referenziati ma assenti | `grep -rhoE 'return "[^"]+"' controller/*.java` |
| Codice Spring Boot 2.x: `javax.persistence/validation/servlet`; `WebSecurityConfigurerAdapter`; `new Sort(...)` | `grep -rhoE "import (javax\|jakarta)\..*"` |
| Nessun test, nessuna LICENSE, README di 2 righe | `git show --stat`, `ls` |
| Autenticazione rotta: `codepasswordindb=false` → doppio encode in login, salvataggio in chiaro | `service/MyUserDetailsService.java:34,51-54,166-170` |
| 137 stampe di debug, incluse password in chiaro nei log | `grep -rn "System.out" --include=*.java .` |
| `ChangePwForm` usato in modo statico dal controller (stato condiviso, non thread-safe) | `controller/UserController.java:107-110` |
| Nessun segreto/credenziale hardcoded versionato | grep su `password\|secret\|api_key\|token\|jdbc` |
| Ambiente: Java 17.0.20.1, Gradle 9.8.1, Maven assente, rete verso Maven Central OK | `java -version`, `gradle -v`, `curl -sI` |
| CodeGraph 1.5.0: indice creato (639 nodi, 1122 edge, 24 file) | `codegraph init/index/status` |
| Remote pubblico `github.com/donatopepe/DailyPlanetH2`, identity `Donato Pepe <dnt.ppe@gmail.com>` | `git remote -v`, `git config` |

**Assunzione dichiarata**: il progetto da sistemare è questo repository (cwd
`/home/public/DailyPlanetH2`); il testo della richiesta indicava genericamente
"il progetto vecchio", e questo repo ha README minimo, nessuna licenza e sorgenti del 2021.

## Vincoli
1. Un solo checkout: `main`, nessun worktree nuovo (regola globale).
2. Ogni iterazione del loop: **una sola variabile nominata** modificata + test eseguiti + TODO aggiornato + commit locale.
3. Nessun push/rilascio pubblico prima del gate S9 (azione irreversibile, richiede approvazione esplicita del proprietario).
4. Nessun segreto in repo, log, vault o indice CodeGraph.
5. Prima di ogni modifica non banale: `codegraph sync .` e verifica impatto (`query`/`explore`).
6. Tree sempre verde a fine iterazione; se un passo rompe la build, si annulla il passo.

## Rubrica di accettazione (12 criteri, 0/1 ciascuno)
| # | Criterio | Come si verifica |
|---|---|---|
| C1 | Build riproducibile con wrapper (`./gradlew clean build`) | comando verde da clone pulito |
| C2 | Applicazione avviabile con profilo `dev` (H2) senza errori | boot log + `/actuator/health` o `/home` 200 |
| C3 | Migrazione `javax.*` → `jakarta.*` completa, zero residui | `grep -rn "javax\." src/` vuoto (eccezioni documentate) |
| C4 | Security su Spring Security 6 (`SecurityFilterChain`), login/logout/403 funzionanti | test MockMvc login OK/KO, 403 per ruolo errato |
| C5 | Persistenza corretta: hash BCrypt salvato in DB, login verifica l'hash, nessun doppio encode | test unit `MyUserDetailsService` + test integrazione login |
| C6 | Tutte le 22 viste/frammenti Thymeleaf presenti e renderizzabili | test MockMvc per ogni rotta + `thymeleaf` render |
| C7 | Logging professionale: zero `System.out`, zero password nei log | `grep -rn "System.out" src/` vuoto; test su log |
| C8 | Suite di test automatica (unit + JPA + MockMvc + 1 E2E) verde | `./gradlew test`, rapporto HTML |
| C9 | README completo (EN + IT): feature, stack, requisiti, setup, run, test, ruoli, deploy, troubleshooting | revisione file + comandi copiati ed eseguiti |
| C10 | Licenza MIT + file di progetto pubblici (LICENSE, CHANGELOG, CONTRIBUTING, .gitignore corretto) | `ls`, intestazione licenza |
| C11 | Distribuzione: Dockerfile + compose (app + Postgres) e CI GitHub Actions build+test | `docker build` (o lint se Docker assente) + workflow presente |
| C12 | Rilascio: versione `1.0.0`, tag e note di rilascio; push solo dopo approvazione | tag locale, gate S9 |

## Fasi, dipendenze, ordine, criteri di completamento

### S1 — Scheletro di build (dipende da: nessuno) → C1, C3(parziale)
- [ ] S1.1 Generare wrapper Gradle 8.14 (`gradle wrapper --gradle-version 8.14`) e `settings.gradle`.
- [ ] S1.2 `build.gradle` con Spring Boot 3.4.x, Java 17, dipendenze: web, thymeleaf, data-jpa, security, validation, h2 (runtime dev), postgresql (runtime prod), actuator, test (junit5, spring-security-test, mockito).
- [ ] S1.3 Spostare i 24 sorgenti in `src/main/java/com/pepedonato/DailyPlanetH2/...` con `git mv`; creare `src/main/resources`, `src/test/java`.
- [ ] S1.4 `.gitignore` per Gradle (`build/`, `.gradle/`, `*.h2.db`, `.codegraph/`, IDE).
- **AC**: `./gradlew compileJava` verde; `git status` pulito dopo commit S1.

### S2 — Migrazione framework (dipende da S1) → C3, C4
- [ ] S2.1 Sostituire import `javax.*` → `jakarta.*` (persistence, validation, servlet).
- [ ] S2.2 Riscrivere `SpringSecurityConfig` con bean `SecurityFilterChain` + `authorizeHttpRequests`, `requestMatchers`, `formLogin`, `logout`, `accessDeniedHandler` (Spring Security 6).
- [ ] S2.3 Adeguare repository/service alle API correnti (`Sort.by`, `Optional`).
- [ ] S2.4 Verificare impatto con `codegraph sync .` + `codegraph explore "security login"`.
- **AC**: `./gradlew compileJava` verde; `grep -rn "javax\." src/` vuoto; test di contesto (`@SpringBootTest`) carica il contesto.

### S3 — Persistenza e credenziali (dipende da S2) → C5
- [ ] S3.1 `application.yml` (+ `application-dev.yml`, `application-prod.yml`): H2 file in dev, PostgreSQL in prod via env var.
- [ ] S3.2 Rimuovere `codepasswordindb`: hash BCrypt in salvataggio, confronto via `PasswordEncoder.matches` in login e cambio password.
- [ ] S3.3 Seed iniziale `data.sql`/`DataInitializer`: admin con password da variabile d'ambiente, primo accesso con cambio password forzato.
- [ ] S3.4 Test: `@DataJpaTest` repository + unit `MyUserDetailsService` (login OK, password errata, utente disabilitato, cambio password).
- **AC**: test S3 verdi; nessuna password in chiaro in DB (verifica su dump H2).

### S4 — Viste Thymeleaf (dipende da S3) → C6
Viste/frammenti da creare (da `return "..."`
): `layouts/main`, `about`, `login`, `error/403`,
`admin`, `account`, `newaccount`, `newaccountDo :: results`, `editaccount`, `editaccountDo :: results`,
`adminchangepw`, `adminchangepwDo :: resultschangepw`,
`user`, `edituser`, `edituserDo :: results`, `newdomanda`, `newdomandaDo :: resultsnewdomanda`,
`domanda`, `listdomande :: resultsList`, `newcommentoDo :: resultsnewcommento`, `listcommentDo :: results`,
`editcommento`, `editrisposta`.
- [ ] S4.1 Layout + frammenti condivisi + CSS/JS statici minimi (nessun asset di terze parti non licenziato).
- [ ] S4.2 Pagine admin (5) e utente (8) con form validati e messaggi di errore.
- [ ] S4.3 Frammenti AJAX coerenti con i `return` dei controller (nomi frammento esatti).
- **AC**: test MockMvc per ogni rotta GET con utente autorizzato → 200 e view attesa; frammenti resi.

### S5 — Pulizia dominio/servizi (dipende da S2) → C7
- [ ] S5.1 Rimuovere tutti i 137 `System.out.println`, introdurre SLF4J senza dati sensibili.
- [ ] S5.2 Eliminare lo stato statico di `ChangePwForm` (istanza per richiesta).
- [ ] S5.3 Gestione errori centralizzata (`@ControllerAdvice`) e messaggi in italiano coerenti.
- **AC**: `grep -rn "System.out" src/` vuoto; test unit servizi verdi.

### S6 — Suite di test completa (dipende da S3, S4, S5) → C8
- [ ] S6.1 Unit: `CategoriaServiceImpl`, `CommentoServiceImpl`, `DomandaServiceImpl`, `MyUserDetailsService`.
- [ ] S6.2 `@DataJpaTest`: repository, query custom, relazioni.
- [ ] S6.3 `@WebMvcTest`/MockMvc: matrice rotte × ruoli (anonimo 302→login, user 200/403, admin 200).
- [ ] S6.4 `@SpringBootTest` E2E: login → crea domanda → commento → cambio stato → logout.
- [ ] S6.5 Report: `build/reports/tests/test/index.html`, riepilogo in TODO.
- **AC**: `./gradlew clean test` verde, 0 failure, matrice S6.3 completa.

### S7 — Documentazione (dipende da S4, S6) → C9, C10
- [ ] S7.1 `README.md` (EN) e `README.it.md` (IT): descrizione, feature, stack, requisiti, installazione, configurazione, esecuzione dev/prod, test, ruoli e flussi, deploy Docker, troubleshooting, struttura repo, screenshot.
- [ ] S7.2 `LICENSE` MIT (© Donato Pepe), `CHANGELOG.md` (Keep a Changelog), `CONTRIBUTING.md`, `.editorconfig`.
- [ ] S7.3 `docs/`: architettura, modello dati (ER), guida utente per ruolo, note di migrazione da Spring Boot 2 a 3.
- **AC**: ogni comando del README eseguito per davvero e verificato; link interni validi.

### S8 — Distribuzione (dipende da S7) → C11
- [ ] S8.1 `Dockerfile` multi-stage + `docker-compose.yml` (app + PostgreSQL con volume).
- [ ] S8.2 GitHub Actions `.github/workflows/ci.yml`: build + test su push/PR (Java 17).
- [ ] S8.3 Profilo `prod` documentato: variabili d'ambiente per DB e credenziali, nessun default segreto.
- **AC**: build immagine verde o, se Docker assente, `hadolint`/validazione equivalente + workflow con sintassi valida.

### S9 — Rilascio pubblico (dipende da S8) → C12 — GATE UMANO
- [ ] S9.1 `version = '1.0.0'`, tag annotato `v1.0.0`, note di rilascio.
- [ ] S9.2 Revisione finale anti-segreto (`gitleaks` o grep mirato) e controllo file pubblicati.
- [ ] S9.3 **Approvazione esplicita del proprietario** registrata qui, poi `git push` di `main` + tag e creazione release GitHub.
- **AC**: approvazione scritta in TODO; push riuscito; release pubblicata.

## Blocker
- (S0) Maven non installato → si usa Gradle + wrapper (nessun blocker reale).
- (S0) `codegraph` MCP non esposto tra i tool; CLI `/usr/local/bin/codegraph` disponibile e usata come da preferenza globale.
- (aperto) Il proprietario non ha ancora indicato un DB di produzione: in S3 si assume PostgreSQL via env var, H2 in dev.

## Prossimo passo immediato
S1.1 → S1.4 (scheletro Gradle + spostamento sorgenti + commit), poi S2.
