# TODO — DailyPlanetH2: da sorgenti legacy a progetto pubblico funzionante

> File unico di pianificazione (regola globale di progetto). Una sola fonte di verità:
> ogni avanzamento aggiorna **Stato**, **Evidenza** e **Blocker** della voce interessata.
> Ultimo aggiornamento: 2026-10-16 (analisi S0).

## Obiettivo (una frase)
Rendere DailyPlanetH2 un'applicazione Spring Boot moderna che compila, si avvia, esegue
i flussi utente/admin end-to-end e si può distribuire pubblicamente (build, guida
completa, README, licenza MIT, test verdi).

## Metrica + soglia
- **Metrica primaria**: criteri di rubrica soddisfatti / criteri totali (rubrica §Rubrica, 13 criteri).
- **Soglia**: 13/13, e nessun criterio parziale.
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

## Rubrica di accettazione (13 criteri, 0/1 ciascuno)
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
| C13 | **Cross-platform**: build e test verdi su Linux, macOS e Windows; nessun path assoluto o separatore di path hardcoded; UTF-8 forzato in compilazione/test/runtime; wrapper Gradle eseguibile su tutti e tre i sistemi; immagine Docker multi-arch | CI matrix 3 OS verde (S8.2) + `git ls-files --eol` normalizzato + grep su path assoluti + `docker buildx` multi-arch |

## Fasi, dipendenze, ordine, criteri di completamento

### S1 — Scheletro di build (dipende da: nessuno) → C1
- [x] S1.1 Generare wrapper Gradle 8.14 (`gradle wrapper --gradle-version 8.14`) e `settings.gradle`.
- [x] S1.2 `build.gradle` con Spring Boot 3.5.16 (ultima 3.x stabile su Maven Central al 2026-10-16; il piano iniziale diceva 3.4.x), Java 17, dipendenze: web, thymeleaf, `thymeleaf-extras-springsecurity6`, data-jpa, security, validation, h2 (runtime dev), postgresql (runtime prod), actuator, test (junit5, spring-security-test, mockito).
- [x] S1.3 Spostare i 24 sorgenti in `src/main/java/com/pepedonato/DailyPlanetH2/...` con `git mv`; creare `src/main/resources`, `src/test/java`.
- [x] S1.4 `.gitignore` per Gradle (`build/`, `.gradle/`, `*.h2.db`, `.codegraph/`, IDE).
- **AC (corretta 2026-10-16)**: `./gradlew help` e risoluzione dipendenze (`./gradlew dependencies --configuration compileClasspath`) verdi; layout standard presente; commit S1 fatto. `./gradlew compileJava` resta **rosso per progetto** finché S2 non completa la migrazione `javax.*` → `jakarta.*`: la sua AC appartiene a S2, non a S1.
- **Stato**: COMPLETATO (2026-10-16).
- **Evidenza**: `gradle wrapper --gradle-version 8.14` → BUILD SUCCESSFUL, `gradle-wrapper.properties` con `gradle-8.14-bin.zip`; `./gradlew help` → BUILD SUCCESSFUL (16s); `./gradlew dependencies --configuration compileClasspath` → BUILD SUCCESSFUL, `spring-boot-starter-{web,thymeleaf,security} -> 3.5.16`, `thymeleaf-extras-springsecurity6 -> 3.1.5.RELEASE`; `find src -type f` → 24 sorgenti in `src/main/java/com/pepedonato/DailyPlanetH2/**`; `./gradlew compileJava` → FAILED con 142 errori `package javax.*` (atteso, AC di S2).
- **Blocker**: nessuno.

### S1.5 — Cross-platform hardening del build (dipende da S1) → C13 (parziale)
- [x] S1.5.1 `.gitattributes`: repo sempre LF, `*.bat`/`*.cmd`/`*.ps1` CRLF, binari esplicitamente binari.
- [x] S1.5.2 UTF-8 forzato su `JavaCompile`, `JavaExec`, `Javadoc` e `Test` (`file.encoding=UTF-8`).
- [x] S1.5.3 Toolchain auto-provisioning con `org.gradle.toolchains.foojay-resolver-convention` 0.10.0 (JDK 17 scaricato automaticamente su Linux/macOS/Windows se assente).
- [x] S1.5.4 EOL normalizzati su tutto il repo (`git add --renormalize .`); nessuna modifica di contenuto oltre ai fine riga.
- **Stato**: COMPLETATO (2026-10-16).
- **Evidenza**: `./gradlew help` → BUILD SUCCESSFUL (20s) con plugin foojay; `./gradlew -q javaToolchains` → "Auto-detection: Enabled / Auto-download: Enabled", JDK 17 rilevato; `git check-attr -a gradlew gradlew.bat` → `gradlew: eol=lf`, `gradlew.bat: eol=crlf`; `git diff --cached --ignore-cr-at-eol --stat` → solo `build.gradle` e `settings.gradle` come modifiche reali (23 file solo normalizzazione EOL); `git ls-files -s gradlew` → mode `100755`.
- **Nota**: la normalizzazione EOL dei 23 sorgenti elimina il rischio di diff rumorosi e problemi di fine riga tra Windows (NetBeans 2021) e Unix.

### S2 — Migrazione framework (dipende da S1) → C3, C4
- [x] S2.1 Sostituire import `javax.*` → `jakarta.*` (persistence, validation, servlet).
- [x] S2.2 Riscrivere `SpringSecurityConfig` con bean `SecurityFilterChain` + `authorizeHttpRequests`, `requestMatchers`, `formLogin`, `logout`, `accessDeniedHandler` (Spring Security 6).
- [x] S2.3 Adeguare repository/service alle API correnti (`Sort.by`, `Optional`).
- [x] S2.4 Verificare impatto con `codegraph sync .` + `codegraph explore "security login"`.
- [x] S2.5 Difetti emersi dal test di contesto e corretti: dipendenza circolare `CategoriaServiceImpl` ↔ `DomandaServiceImpl` (rimossa iniettando `DomandaLavoroRepository`), tabella `user` → `users` (parola riservata in H2 2.x e PostgreSQL: fix necessario anche per C13).
- **Stato**: COMPLETATO (2026-10-16).
- **Evidenza**: `sed` su tutti gli import → `grep -rn "javax\." src/` → nessun residuo (43 import `jakarta.persistence`, 21 `jakarta.validation`, 7 `jakarta.servlet`); `./gradlew compileJava` → BUILD SUCCESSFUL (prima 142 errori, poi 3 errori `new Sort(...)` risolti in `Sort.by(...)`); `./gradlew test` → BUILD SUCCESSFUL, `tests=1 failures=0 errors=0` (`DailyPlanetH2ApplicationTests.contextLoads`); `codegraph sync .` → Done.
- **Decisione tecnica (S2.2)**: CSRF **attivo** (default Spring Security 6) al posto del `csrf().disable()` originale; i template di S4 devono usare `th:action` sui form e il meta tag del token per le chiamate AJAX.
- **Blocker**: nessuno.

### S3 — Persistenza e credenziali (dipende da S2) → C5
- [x] S3.1 `application.yml` (+ `application-dev.yml`, `application-prod.yml`): H2 file in dev, PostgreSQL in prod via env var.
- [x] S3.2 Rimuovere `codepasswordindb`: hash BCrypt in salvataggio, confronto via `PasswordEncoder.matches` in login e cambio password.
- [x] S3.3 Seed iniziale `data.sql`/`DataInitializer`: admin con password da variabile d'ambiente, primo accesso con cambio password forzato.
- [x] S3.4 Test: `@DataJpaTest` repository + unit `MyUserDetailsService` (login OK, password errata, utente disabilitato, cambio password).
- [x] S3.5 **Cross-platform**: nessun path assoluto o separatore hardcoded; H2 in posizione relativa alla working directory; `spring.mandatory-file-encoding=UTF-8`; `data.sql` in UTF-8 senza BOM; date/ora con `ZoneId` esplicito.
- **Stato**: COMPLETATO (2026-10-16).
- **Evidenza**: `./gradlew clean test` → BUILD SUCCESSFUL, **tests=15 skipped=0 failures=0 errors=0** (`AdminAccountInitializerTest`, `DailyPlanetH2ApplicationTests`, `UserRepositoryTest`, `MyUserDetailsServiceTest`); H2 dev in `./data/dailyplanet` (path relativo, `AUTO_SERVER=TRUE`); profilo `prod` senza default per `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`; seed fail-fast verificato da test su profilo `prod` simulato; `grep -rn "javax\." src/` vuoto.
- **Difetti corretti**: doppio encode all'autenticazione; password salvate in chiaro; `findByUsernameAndPassword` (confronto in chiaro in SQL) rimosso; credenziale di default scritta nel codice rimossa; `save()` su utente già hashato in `doEditUser` → `saveNoPw`; `System.out` con password eliminati (restano 105 print non sensibili, target S5.1).
- **Cross-check residuo**: `grep -rnE '([A-Za-z]:\\\\|/home/|/Users/|/tmp/)' src/main` → nessun path assoluto (da rieseguire in S7/S9).
- **Blocker**: nessuno.

### S3.6 — Scrub dei segreti dalla storia git (dipende da S3) → C10 addendum — approvato dal proprietario [D6]
- [x] S3.6.1 Regole di sostituzione per la credenziale di default dei commit 2021 (`/tmp/scrub-rules.txt` per i blob, `/tmp/scrub-messages.txt` per i messaggi; valori non riportati in questo file).
- [x] S3.6.2 Riscrittura completa con `git filter-repo --replace-text ... --replace-message ... --force`: 9 commit riscritti in 0,22 s, repack e clean automatici; `origin` rimosso dal tool e **ripristinato** a mano.
- [x] S3.6.3 Verifica esaustiva: **0** occorrenze del segreto in tutti i 184 oggetti (`git cat-file --batch-all-objects --batch | grep -c`), **0** in `git grep` su tutti i commit, **0** nei messaggi di commit, **0** byte-level in `.git/objects/`; 5 occorrenze del segnaposto `REDACTED_default_credential` provano che la sostituzione è avvenuta anche nei blob storici.
- [x] S3.6.4 Nessun hash da rimappare in `TODO.md`/`docs/` (verificato con grep); bundle di backup contenente il segreto **eliminato**; reflog azzerato e `git repack -a -d -f` + `git gc --prune=now` eseguiti.
- **Stato**: COMPLETATO (2026-10-16).
- **Conseguenze operative**: (1) push eseguito il 2026-10-16 con `--force-with-lease` (S9.3a): il branch pubblicato è ora la storia riscritta, quindi il push finale di S9 sarà un fast-forward; (2) GitHub serve ancora gli oggetti non più raggiungibili per SHA → azione umana S9.4 con testo pronto in `docs/github-history-purge.md`; (3) nessun impatto su utenti reali (progetto locale senza utenti).
- **Blocker**: nessuno.

### S4 — Viste Thymeleaf (dipende da S3) → C6
Viste/frammenti da creare (da `return "..."`
): `layouts/main`, `about`, `login`, `error/403`,
`admin`, `account`, `newaccount`, `newaccountDo :: results`, `editaccount`, `editaccountDo :: results`,
`adminchangepw`, `adminchangepwDo :: resultschangepw`,
`user`, `edituser`, `edituserDo :: results`, `newdomanda`, `newdomandaDo :: resultsnewdomanda`,
`domanda`, `listdomande :: resultsList`, `newcommentoDo :: resultsnewcommento`, `listcommentDo :: results`,
`editcommento`, `editrisposta`.
- [x] S4.1 Layout + frammenti condivisi + CSS/JS statici minimi (nessun asset di terze parti non licenziato).
- [x] S4.2 Pagine admin (5) e utente (8) con form validati e messaggi di errore.
- [x] S4.3 Frammenti AJAX coerenti con i `return` dei controller (nomi frammento esatti).
- [x] S4.4 CSRF: form con `th:action` (token automatico) e meta tag `<meta name="_csrf">` + header nelle chiamate AJAX (vedi S2.2).
- **Stato**: COMPLETATO (2026-10-16).
- **Evidenza**: 20 file `.html` in `src/main/resources/templates` + `static/css/app.css`; `./gradlew clean test` → **tests=26 skipped=0 failures=0 errors=0** (incluse 11 prove di rendering reale in `ViewRenderingTest`); avvio reale con profilo `dev` su porta 18080 → `Tomcat started`, `GET /login` 200, `GET /about` 200, `GET /user` anonimo 302 → `/login`, `GET /css/app.css` 200, pagina login renderizzata con meta `_csrf`; seed amministratore idempotente (`nessuna azione.` al secondo avvio).
- **Difetti corretti (e relative cause)**: (1) `Commento.domanda`/`Commento.commento` erano `@OneToOne` → vincolo unique impediva più commenti per domanda: ora `@ManyToOne`; (2) `LazyInitializationException` su `DomandaLavoro.categorie` con `open-in-view: false` → `@EntityGraph` sui repository di domande e commenti; (3) `Optional.get()` in `DomandaServiceImpl` e `CommentoServiceImpl` → `orElse(null)` (era errore 500 su id inesistente); (4) view ritornate `"redirectuser"`/`"redirectaccount"` (non esistenti) → `redirect:/user` e `redirect:/admin/account`; (5) endpoint AJAX che rendevano frammenti HTML dichiarando `produces=application/json` → `text/html`; (6) `allUser` è una lista di username (non di entità): template adeguato.
- **Nota per S5**: la rotta `/user/listcommenti/{id}` riceve in realtà un id di commento (il nome del parametro è fuorviante) — da chiarire nella pulizia dei servizi.
- **Blocker**: nessuno.

### S5 — Pulizia dominio/servizi (dipende da S2) → C7
- [x] S5.1 Rimuovere tutti i `System.out.println` e usare SLF4J senza dati sensibili.
- [x] S5.2 Eliminare lo stato condiviso nei controller (i campi di istanza `allCategorie`/`allUser` mutati per richiesta sono ora variabili locali + helper `categorieDisponibili()`); logger del `UserController` corretto (era `CommandLineRunnerBean.class`).
- [x] S5.3 Gestione errori centralizzata (`@ControllerAdvice` → `GlobalExceptionHandler`) e messaggi in italiano coerenti.
- **Stato**: COMPLETATO (2026-10-16).
- **Evidenza**: `grep -rn "System.out\|System.err\|printStackTrace" src/main` → **zero** (rimosse 101 righe di stampa + 4 commenti con stampe; restano solo i `log` SLF4J); `grep -n "allCategorie = new\|allUser = new"` → nessun campo condiviso; `./gradlew clean test` → BUILD SUCCESSFUL, tests=26 failures=0 errors=0; `GlobalExceptionHandler` con `@ExceptionHandler` per `NoSuchElementException`/`IllegalArgumentException` (404), `DataIntegrityViolationException` (409) e fallback 500, tutti registrati su log e mostrati in `error/error`.
- **Nota**: `MyUserDetailsService` aveva già logging SLF4J senza password da S3.
- **Blocker**: nessuno.

### S6 — Suite di test completa (dipende da S3, S4, S5) → C8
- [ ] S6.1 Unit: `CategoriaServiceImpl`, `CommentoServiceImpl`, `DomandaServiceImpl`, `MyUserDetailsService`.
- [ ] S6.2 `@DataJpaTest`: repository, query custom, relazioni.
- [ ] S6.3 `@WebMvcTest`/MockMvc: matrice rotte × ruoli (anonimo 302→login, user 200/403, admin 200).
- [ ] S6.4 `@SpringBootTest` E2E: login → crea domanda → commento → cambio stato → logout.
- [ ] S6.5 Report: `build/reports/tests/test/index.html`, riepilogo in TODO.
- [ ] S6.6 **Cross-platform**: nessun test dipende da separatori di path, fuso orario locale, codifica di piattaforma o permessi POSIX; casi parametrici su Linux/macOS/Windows (CI matrix S8.2).
- **AC**: `./gradlew clean test` verde, 0 failure, matrice S6.3 completa; nessuna assunzione di piattaforma nei test (verifica: nessun `/tmp`, `\\`, `File.separator` o `TimeZone.getDefault()` nei test).

### S7 — Documentazione (dipende da S4, S6) → C9, C10
- [ ] S7.1 `README.md` (EN) e `README.it.md` (IT): descrizione, feature, stack, requisiti, installazione, configurazione, esecuzione dev/prod, test, ruoli e flussi, deploy Docker, troubleshooting, struttura repo, screenshot.
- [ ] S7.2 `LICENSE` MIT (© Donato Pepe), `CHANGELOG.md` (Keep a Changelog), `CONTRIBUTING.md`, `.editorconfig`.
- [ ] S7.3 `docs/`: architettura, modello dati (ER), guida utente per ruolo, note di migrazione da Spring Boot 2 a 3.
- [ ] S7.4 **Istruzioni per sistema operativo** nel README (EN e IT): Linux/macOS (`./gradlew`), Windows PowerShell/cmd (`gradlew.bat`, `$env:VAR` vs `set VAR`), tabella di comandi equivalenti, nota su UTF-8 e normalizzazione dei fine riga (`.gitattributes`), opzione Docker come percorso uniforme.
- **AC**: ogni comando del README eseguito per davvero e verificato; link interni validi; per ogni OS esiste un percorso di avvio completo (dev e test).

### S8 — Distribuzione (dipende da S7) → C11, C13 (parziale)
- [ ] S8.1 `Dockerfile` multi-stage **multi-architettura** (linux/amd64 + linux/arm64, buildx) + `docker-compose.yml` (app + PostgreSQL con volume).
- [ ] S8.2 GitHub Actions `.github/workflows/ci.yml`: **matrix OS** `ubuntu-latest` / `windows-latest` / `macos-latest` con Java 17 e `shell: bash` per i passi Gradle (parità col comando `./gradlew`), build + test su push/PR.
- [ ] S8.3 Profilo `prod` documentato: variabili d'ambiente per DB e credenziali, nessun default segreto; `.env.example` senza valori reali.
- **AC**: `docker buildx build --platform linux/amd64,linux/arm64` verde o, se Docker assente, validazione equivalente; workflow con sintassi YAML valida e 3 job coerenti con `./gradlew clean test`.

### S9 — Rilascio pubblico (dipende da S8) → C12
- [ ] S9.1 `version = '1.0.0'`, tag annotato `v1.0.0`, note di rilascio.
- [ ] S9.2 Revisione finale anti-segreto (`gitleaks` o grep mirato) e controllo file pubblicati.
- [x] S9.3a **Push intermediario di sicurezza** (eseguito 2026-10-16): il branch `main` già pubblicato conteneva la credenziale di default del 2021; il push è stato fatto con `git push --force-with-lease=main:<sha-vecchio> origin main` (D6, mai `--force`). Evidenza: `1aeafc7...aecf705 main -> main (forced update)`; clone di verifica → `git grep` sul branch remoto: nessuna occorrenza del segreto.
- [ ] S9.3b Push finale di `main` + tag `v1.0.0` + release GitHub: ora è un **fast-forward** (il remoto ha già la storia riscritta), quindi nessun `--force` è più necessario.
- [ ] S9.4 **Azione umana residua**: inviare la richiesta a GitHub Support per la rimozione degli oggetti *dangling* (il vecchio commit è ancora servito per SHA: verificato `raw.githubusercontent.com/.../<sha-vecchio>/controller/DefaultController.java` → HTTP 200). Testo pronto in [`docs/github-history-purge.md`](docs/github-history-purge.md); a purge avvenuta, ripetere la verifica "commit non più scaricabile" e chiudere qui.
- **AC**: rubrica 13/13; `./gradlew clean test` verde; scansione anti-segreto pulita; push del fast-forward e tag riusciti; richiesta di purge inviata (o rischio documentato come accettato dal proprietario: la credenziale era un default di un'app locale senza utenti reali).

## Decisioni approvate dal proprietario (2026-10-16)
| # | Decisione | Stato |
|---|---|---|
| D1 | Progetto target = `/home/public/DailyPlanetH2` | **confermato** dal proprietario |
| D2 | DB produzione = PostgreSQL via variabili d'ambiente, H2 file in dev | **confermato** |
| D3 | Build = Gradle 8.14 con wrapper committato, Spring Boot 3.5.16, Java 17 | **confermato** |
| D4 | Rilascio pubblico (push `main` + tag `v1.0.0` + release GitHub) in S9 | **pre-approvato**: eseguibile senza ulteriore richiesta una volta che la rubrica è 13/13, `./gradlew clean test` è verde e la scansione anti-segreto è pulita (S9.2) |
| D5 | Licenza MIT con copyright "Donato Pepe" | **confermato** |
| D6 | Scrub dei segreti dalla **storia git** (non solo dall'albero corrente) e conseguente riscrittura degli hash | **approvato ed eseguito** il 2026-10-16: `git filter-repo` (S3.6) e push con `git push --force-with-lease=<sha atteso>` (S9.3a). Resta solo la richiesta a GitHub Support per gli oggetti dangling (S9.4, azione umana). |

## Blocker
- (S0) Maven non installato → si usa Gradle + wrapper (nessun blocker reale).
- (S0) `codegraph` MCP non esposto tra i tool; CLI `/usr/local/bin/codegraph` disponibile e usata come da preferenza globale.
- (aperto) Push/release S9 restano subordinati ai gate automatici D4 (rubrica 13/13, test verdi, nessun segreto).

## Prossimo passo immediato
S6.1 → S6.6 (suite di test completa: unit servizi, `@DataJpaTest`, matrice rotte × ruoli, E2E login→domanda→commento→logout, report).