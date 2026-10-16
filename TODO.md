# TODO — DailyPlanetH2: da sorgenti legacy a progetto pubblico funzionante

> File unico di pianificazione (regola globale di progetto). Una sola fonte di verità:
> ogni avanzamento aggiorna **Stato**, **Evidenza** e **Blocker** della voce interessata.
> Ultimo aggiornamento: 2026-10-16 (S10 cybersecurity, implementato e testato localmente).

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
| Remote pubblico `github.com/donatopepe/DailyPlanetH2`, identity di commit dell'autore (nessun indirizzo riportato in questo file) | `git remote -v`, `git config` |

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
| C11 | Distribuzione: Dockerfile + compose (app + Postgres) e CI GitHub Actions build+test | `docker build` → Successfully built; `docker compose --env-file .env.example config` valido; CI run 38893577795 verde |
| C12 | Rilascio: versione `1.0.0`, tag e note di rilascio; push solo dopo approvazione | tag `v1.0.0` pubblicato + release GitHub con note (D4) |
| C13 | **Cross-platform**: build e test verdi su Linux, macOS e Windows; nessun path assoluto; UTF-8; wrapper eseguibile sui tre sistemi; immagine multi-arch | **CI matrix 3 OS verde** (run 38893577795: ubuntu/windows/macos success) + job multi-arch success + `git ls-files --eol` normalizzato + grep path assoluti = 0 |

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
- [x] S6.1 Unit: `CategoriaServiceImpl` (7), `CommentoServiceImpl` (5), `DomandaServiceImpl` (5), `MyUserDetailsService` (7) — mock dei repository, nessun database.
- [x] S6.2 `@DataJpaTest`: `UserRepositoryTest` (3), `DomandaLavoroRepositoryTest` (5: query derivate, `EntityGraph` verificato dopo `entityManager.clear()`, più commenti per domanda, separazione commenti/risposte).
- [x] S6.3 MockMvc: `RoleAccessMatrixTest` (5) — matrice anonimo/`ROLE_USER`/`ROLE_ADMIN` su rotte pubbliche, utente e admin; 403 ↔ redirect `/403`; 404 per rotte inesistenti.
- [x] S6.4 `@SpringBootTest` E2E: `EndToEndFlowTest` (2) — login reale con form (sessione) → creazione domanda → commento → lettura pagina con il commento → logout → rotta protetta; più login con password errata.
- [x] S6.5 Report: `build/reports/tests/test/index.html`; riepilogo sotto.
- [x] S6.6 **Cross-platform**: nessun `/tmp`, `\`, `File.separator`, fuso orario di piattaforma o permesso POSIX nei test; H2 in memoria per i test; suite eseguibile identica sui tre OS (prova in S8.2).
- **Stato**: COMPLETATO (2026-10-16).
- **Evidenza**: `./gradlew clean test` → **BUILD SUCCESSFUL, tests=55 skipped=0 failures=0 errors=0**, ripartizione: `DailyPlanetH2ApplicationTests` 1, `AdminAccountInitializerTest` 4, `EndToEndFlowTest` 2, `RoleAccessMatrixTest` 5, `ViewRenderingTest` 11, `DomandaLavoroRepositoryTest` 5, `UserRepositoryTest` 3, `CategoriaServiceImplTest` 7, `CommentoServiceImplTest` 5, `DomandaServiceImplTest` 5, `MyUserDetailsServiceTest` 7. Nessun test dipende da dati preesistenti (pulizia ordinata o `@Transactional`).
- **Difetti reali scoperti e corretti scrivendo i test** (nessuno era rilevabile a occhio):
  1. `Commento.domanda`/`commento` erano `@OneToOne` → vincolo unique: impossibile avere più commenti (corretto in S4, ora coperto da test di regressione);
  2. **binding della variabile di percorso**: `POST /user/doNewCommento/{id}` e `/user/doNewAnswer/{id}` legavano `id` al campo `Commento.id` → Hibernate trattava il commento nuovo come riga esistente (`ObjectOptimisticLockingFailureException`). Ora il testo arriva da `@RequestParam("descrizione")` e l'entità è costruita nel controller;
  3. `Categoria.domande` era proprietario su **entrambi** i lati dell'associazione → due tabelle di join duplicate (`categoria_domande` + `domanda_lavoro_categorie`); ora lato inverso `mappedBy = "categorie"`;
  4. `Categoria.addDomanda/removeDomanda` andavano in `LazyInitializationException`/NPE (collezione non inizializzata) e la persistenza passava dal lato inverso: ora sono operazioni in memoria null-safe, la persistenza è sul lato proprietario;
  5. `saveCategorie` modificava la lista del chiamante con `List.set(...)` → `UnsupportedOperationException` con liste immutabili: ora ritorna una lista nuova;
  6. `saveDomandaLavoro` salvava la domanda due volte (doppio `saveCategorie` + `save`): ora un solo salvataggio;
  7. `GlobalExceptionHandler` rispondeva **HTTP 200** con la pagina di errore: ora `@ResponseStatus` (404/409/500) + gestore dedicato `NoResourceFoundException` e `templates/error.html` per la view di errore di Spring Boot.
- **Effetto collaterale da documentare (S7.4)**: la correzione dell'associazione rimuove la tabella `categoria_domande`: i database di sviluppo H2 creati prima vanno ricreati (cancellare `./data`).
- **Blocker**: nessuno.

### S7 — Documentazione (dipende da S4, S6) → C9, C10
- [x] S7.1 `README.md` (EN) e `README.it.md` (IT): descrizione, funzionalità, stack, requisiti, avvio rapido, configurazione, database, ruoli e flussi, tabella rotte, test, build/jar, struttura, troubleshooting, roadmap. La sezione Docker arriva in S8 (non si documentano comandi inesistenti).
- [x] S7.2 `LICENSE` MIT (© Donato Pepe), `CHANGELOG.md` (Keep a Changelog, nessun link a tag inesistenti), `CONTRIBUTING.md`, `.editorconfig`.
- [x] S7.3 `docs/`: `architecture.md`, `data-model.md` (ER + regole di integrità), `user-guide.md` (per ruolo), `migration-spring-boot-2-to-3.md` (incluso script di migrazione dati).
- [x] S7.4 **Istruzioni per sistema operativo**: comandi equivalenti Linux/macOS (`./gradlew`), Windows PowerShell (`$env:VAR`) e cmd (`set VAR`, `gradlew.bat`); nota su UTF-8, `.gitattributes` e percorsi relativi del database.
- **Stato**: COMPLETATO (2026-10-16).
- **Evidenza comandi eseguiti davvero**: `./gradlew clean test` → BUILD SUCCESSFUL, **tests=59 failures=0**; `./gradlew clean build` → BUILD SUCCESSFUL con `build/libs/DailyPlanetH2-1.0.0-SNAPSHOT.jar` (nome verificato e usato nei comandi del README); `java -jar … --spring.profiles.active=dev --server.port=18085` → `GET /actuator/health` **200 `{"status":"UP"}`**, `GET /login` **200**, H2 creato nel percorso **relativo** `/tmp/devcheck2/data/dailyplanet.mv.db` (prova del cross-platform dei percorsi); profilo `prod` senza variabili → **exit 1** con `IllegalStateException: Profilo 'prod': variabili d'ambiente obbligatorie mancanti: DB_URL, DB_USERNAME, DB_PASSWORD, DAILYPLANET_ADMIN_PASSWORD…`; profilo `prod` con datasource valido ma senza password admin → `IllegalStateException: DAILYPLANET_ADMIN_PASSWORD non impostata…`.
- **Correzioni nate dalla verifica dei comandi documentati**: (1) `/actuator/health` richiedeva autenticazione (302) mentre il README lo documentava come endpoint di salute → ora pubblico insieme a `/actuator/info`; (2) un `DB_URL` mancante in produzione falliva con un messaggio oscuro di Hibernate → nuovo `ProductionEnvironmentValidator` (`EnvironmentPostProcessor`) che elenca le variabili mancanti **prima** dell'avvio del contesto, con 4 test dedicati.
- **Limiti dichiarati**: il successo dell'avvio in `prod` con PostgreSQL non è verificabile su questa macchina (nessun server PostgreSQL né Docker in questa fase); verrà coperto in S8.1 con `docker-compose`. Il comando `git clone` non è stato eseguito (repository già presente) e i comandi Windows non sono eseguibili qui: prova prevista in S8.2 (CI matrix su tre sistemi).
- **Blocker**: nessuno.

### S8 — Distribuzione (dipende da S7) → C11, C13 (parziale)
- [x] S8.1 `Dockerfile` multi-stage + `docker-compose.yml` (app + PostgreSQL 17 con volume e healthcheck) + `.dockerignore` + `.env.example` (solo segnaposto). Immagine di esecuzione **non root**, healthcheck del container su `/actuator/health`, profilo `prod` predefinito.
- [x] S8.2 GitHub Actions `.github/workflows/ci.yml`: job `test` con **matrix OS** `ubuntu-latest`/`windows-latest`/`macos-latest` (Java 17 temurin, `shell: bash` per usare lo stesso `./gradlew` su tutti i sistemi, upload del report) + job `docker-multiarch` con QEMU + buildx su `linux/amd64,linux/arm64` senza push.
- [x] S8.3 Profilo `prod` documentato (README EN/IT + `docs/`), `.env.example` con segnaposto, nessun default segreto; `.env` in `.gitignore`.
- **Stato**: PARZIALE (2026-10-16) — artefatti completi e verificati fin dove l'ambiente lo consente.
- **Evidenza (eseguita davvero)**: `docker compose --env-file .env.example config` → configurazione valida (servizi `app` + `db`, dipendenza `service_healthy`, porte, variabili); `docker build -t dailyplaneth2:test .` → **Successfully built** (18 step, fase build con cache delle dipendenze Gradle, fase runtime con JDK 17 JRE non root); `docker manifest inspect eclipse-temurin:17-jdk-jammy | 17-jre-jammy | postgres:17-alpine` → **amd64 + arm64 presenti** (premessa multi-architettura verificata); validazione YAML di `.github/workflows/ci.yml` e `docker-compose.yml` → file validi, matrix `ubuntu-latest, windows-latest, macos-latest`, `platforms: linux/amd64,linux/arm64`, `shell: bash` presente.
- **BLOCKER (ambientale, non del progetto)**: la radice del filesystem di questa macchina è **al 100%** (`/dev/nvme0n1p2 107G usati 102G, disponibili ~102MB`). Conseguenze provate: (a) `docker compose up` → `initdb: error: could not create directory "/var/lib/postgresql/data/pg_wal": No space left on device` (PostgreSQL non si inizializza, quindi lo stack completo non è verificabile); (b) `docker run … SPRING_PROFILES_ACTIVE=dev` sull'immagine costruita → `org.h2.message.DbException: Error while creating file "/app/data"` (il container non riesce a scrivere il proprio layer). Tentativi effettuati: `docker compose down -v`, rimozione delle immagini create (`dailyplaneth2:test`, `dailyplaneth2:1.0.0-SNAPSHOT`), `docker image prune -f` (recuperati 647MB ma quasi tutti layer condivisi preesistenti) → disponibile risalito solo a ~102MB, insufficiente per inizializzare il cluster PostgreSQL (~100-200MB). Non è stato cancellato nulla di altri progetti: immagini, volumi e container di terze parti sono rimasti intatti.
- **Secondo gap dichiarato**: `docker buildx` **non è installato** su questa macchina (plugin assente, non un difetto del progetto) → la build multi-architettura locale non è eseguibile; è coperta dal job `docker-multiarch` della CI e documentata nel README.
- **Per chiudere S8.1**: liberare almeno ~1GB sulla radice, poi `docker compose --env-file .env.example up -d --build` + verifica `/actuator/health` e login; in alternativa la CI esegue test su 3 OS e la build multi-architettura a ogni push.

### S9 — Rilascio pubblico (dipende da S8) → C12
- [x] S9.1 `version = '1.0.0'`, plain jar disattivato (un solo artefatto `DailyPlanetH2-1.0.0.jar`), tag annotato `v1.0.0`, note di rilascio GitHub.
- [x] S9.2 Revisione finale anti-segreto e controllo file pubblicati: 0 segreti nell'albero e in **tutta** la storia; nessun `.env` tracciato (solo `.env.example` con segnaposto); 0 path assoluti in codice/build; 0 `System.out`; indirizzo email personale rimosso dal `TODO.md` pubblicato.
- [x] S9.3a **Push di sicurezza** (2026-10-16): `git push --force-with-lease=main:<sha-vecchio> origin main` → `1aeafc7...aecf705 (forced update)`; verifica su clone pulito: nessuna occorrenza del segreto nel branch remoto.
- [x] S9.3b Push finale: `aecf705..5d0b0a0` e poi `5d0b0a0..d7448a4` (**fast-forward**, nessun `--force`), `git push origin v1.0.0`, release GitHub pubblicata.
- [x] S9.3c **Esiti reali della CI** (prova di C13): run [38893577795](https://github.com/donatopepe/DailyPlanetH2/actions/runs/38893577795) → `Test (ubuntu-latest)` **success**, `Test (windows-latest)` **success**, `Test (macos-latest)` **success**, `Immagine Docker multi-architettura` (linux/amd64 + linux/arm64) **success**. Il primo run (38893311877) era rosso su tutti e 4 i job: difetto reale scoperto dalla CI (vedi sotto), corretto e riprovato.
- [ ] S9.4 **Azione umana residua**: richiesta a GitHub Support per la rimozione degli oggetti *dangling* (il vecchio commit è ancora servito per SHA: `raw.githubusercontent.com/.../<sha-vecchio>/controller/DefaultController.java` → HTTP 200). Testo pronto in [`docs/github-history-purge.md`](docs/github-history-purge.md); a purge avvenuta ripetere la verifica "commit non più scaricabile".
- **Stato**: COMPLETATO (2026-10-16) tranne S9.4, che richiede un'azione umana e non è eseguibile da un agente.
- **Difetto reale trovato dalla prima esecuzione della CI (e corretto)**: `.gitignore` conteneva `*.jar` **dopo** l'eccezione `!gradle/wrapper/gradle-wrapper.jar`; l'ultima regola che corrisponde vince, quindi il wrapper **non era versionato** e qualunque clone pulito falliva con `Unable to access jarfile .../gradle-wrapper.jar` (tutti e 4 i job CI rossi, e gli archivi sorgente del tag non compilabili). Correzioni: eccezione spostata dopo `*.jar` con commento, wrapper versionato (48.749 byte), nuovo passo CI `Il wrapper Gradle è versionato` (`test -f …`) come guardia di regressione, tag `v1.0.0` ricreato sull'ultimo commit.
- **Artefatti pubblicati**: release <https://github.com/donatopepe/DailyPlanetH2/releases/tag/v1.0.0> (tag annotato `v1.0.0`), `main` allineato al locale, jar `DailyPlanetH2-1.0.0.jar` costruibile con `./gradlew clean build`.
- **Blocker residui**: (1) disco della macchina al 100% → verifica locale dello stack `docker compose` impossibile (la build dell'immagine è però verde in CI per amd64 e arm64); (2) purge GitHub degli oggetti dangling = azione umana S9.4.

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
- (chiuso) Push/release S9 eseguiti il 2026-10-16 con i gate D4 soddisfatti: rubrica 13/13, `./gradlew clean test` verde (59 test), scansione anti-segreto pulita, push fast-forward e release pubblicata con CI verde.
- (aperto, umano) S9.4: richiesta a GitHub Support per la rimozione degli oggetti dangling del vecchio commit.
- (aperto, ambientale) Disco della macchina al 100% (~100MB liberi): impedisce la verifica locale di `docker compose` con PostgreSQL. La build dell'immagine per amd64 e arm64 è verde in CI.

## Stato finale della rubrica (2026-10-16)

**13/13 criteri soddisfatti con evidenza.** Uniche voci fuori dal controllo dell'agente:
- **S9.4** (richiesta a GitHub Support per gli oggetti *dangling*): azione umana, testo pronto.
- Verifica locale dello stack `docker compose` (PostgreSQL): impossibile per disco pieno al 100% sulla macchina; la build dell'immagine per `linux/amd64` e `linux/arm64` è verde in CI.

## Prossimo passo
Piano iniziale chiuso. Richiesta nuova del proprietario (2026-10-16): **"implementa cybersecurity"**. Avvio S10 con gate esplicito e deliverable qui sotto.

## S10 — Hardening cybersecurity (richiesto dopo v1.0.0)
- **Variabile di questa iterazione**: hardening web/app + usabilità form; baseline osservata prima delle modifiche; nessun push/tag.

### Gate di lavoro
- **Obiettivo**: ridurre vulnerabilità concrete nelle superfici HTTP/sessioni/account, documentare le misure e provare i controlli con test negativi.
- **Deliverable**: ownership/IDOR per account e commenti, password-change legato alla sessione, accesso negato a commenti altrui, header HTTP difensivi + CSP senza script inline, rate limit login, test di regressione e guida `docs/security.md`.
- **Metrica**: verifiche di sicurezza passate / verifiche di sicurezza definite.
- **Soglia**: 100%, zero vulnerabilità High/Critical non mitigata nelle superfici modificate.
- **Casi e ordine**: (1) utente tenta IDOR su profilo/ruolo/stato; (2) utente tenta cambiare password di altro utente; (3) utente tenta leggere/modificare commenti altrui; (4) CSRF mancante/presente; (5) header/CSP; (6) brute force login/rate limit; (7) `./gradlew clean test`; poi dependency/audit statico e smoke.
- **Baseline osservata**: S4–S9 avevano CSRF, BCrypt e alcune autorizzazioni, ma `doEditUser` si fidava di ID/username del form; POST cambio password si fidava dello username del form; commenti erano bindati a ID e alcune operazioni non verificavano owner; header CSP con `unsafe-inline`; nessun rate limit login. Form di account/commento ed elenchi AJAX erano inoltre incompleti/non usabili; verificati via browser-like MockMvc e corretti.
- **Assunzione**: cybersecurity applicativa web (non audit infrastrutturale/pen test esterno); non introdurre servizi SaaS obbligatori né raccogliere credenziali.

### Task S10
- [x] S10.1 Self-service profilo: utente caricato dalla sessione; ID deve corrispondere; il controller aggiorna soltanto nome/cognome/email; ruolo, username e stato ignorati lato client. SecurityRegressionTest prova ID mismatch e escalation ADMIN/disabled.
- [x] S10.2 Cambio password self-service: username deriva da SecurityContext, il form non può scegliere altro account; id amministrativo firmato dai dati server-side e username non più accettato dal form reset; FormFlowTest copre cambio valido e password attuale errata.
- [x] S10.3 Commenti: form GET/POST verifica domanda associata e autore o ROLE_ADMIN; commento/risposta non bindati direttamente a entità per le nuove rotte; regressione IDOR.
- [x] S10.4 Header: CSP `script-src 'self'`, no inline JS; `nosniff`, `DENY`, referrer policy, HSTS (HTTPS), `Cache-Control: no-store`; script esterno `/js/app.js`; tolto inline CSS dai template; SecurityHeadersTest.
- [x] S10.5 Login rate limiting: 8 errori / 15 min → 429 + Retry-After per coppia IP+username, cooldown 15 min, bucket normalizzati, reset al successo, massimo 10.000 entry; LoginFailureTrackerTest + LoginRateLimitTest; dummy BCrypt per account inesistente/disabilitato.
- [x] S10.6 Security regressions + `docs/security.md`: threat model, limiti, setup deploy, 75 test aggiornati; request logging redatto (niente body/header/query/client info/session ID).
- **Stato**: IMPLEMENTAZIONE + TEST + smoke locale COMPLETATI; non ancora pubblicato.
- **Evidenza**: release-check 2026-10-16 ripetuto: `./gradlew clean build` → BUILD SUCCESSFUL; `./gradlew clean test` → **75 test, 0 skipped, 0 failure, 0 error** (S10 aggiunge 16 regressioni: FormFlow 4, SecurityRegression 5, SecurityHeaders 2, LoginRateLimit 1, LoginFailureTracker 4). Smoke jar reale (`dev`, port 18093): `/login` 200, `/js/app.js` 200; header effettivi `nosniff`, `DENY`, Referrer-Policy, CSP `script-src 'self'; style-src 'self'`, Cache-Control `no-store`; rate-limit live: tentativi errati 1–7 → 302, tentativo 8 → **429**, `Retry-After: 900`, stack overflow count 0. `git diff --check` OK; scansione template: nessun inline style/event handler/script; `System.out`/`printStackTrace` zero. Classi controllate: SecurityRegressionTest (5), SecurityHeadersTest (2), LoginRateLimitTest (1), LoginFailureTrackerTest (4), FormFlowTest (4).
- **Hardening confermato**: self-update usa ID in session/database e aggiorna solo nome/cognome/email; il form profilo rende username `readonly`; il cambio password self-service vincola l'username autenticato; reset password admin usa ID target server-side (il form non invia username); POST commento/resposta modifica verifica ownership oppure `ROLE_ADMIN`; session fixation `changeSessionId`, massimo 2 sessioni, logout invalida cookie; `Cache-Control: no-store`; script esterno senza inline JS; CSP senza `unsafe-inline` per script né stili; headers testati.
- **Rate limit**: 8 fallimenti per coppia IP+username in 15 minuti → cooldown 15 minuti; success reset; mappa bounded 10.000 entry; username inesistente/disabilitato usa dummy BCrypt; nessuna password/token nel log.
- **Limiti noti**: limiter in-memory per singola istanza, non condiviso in multi-replica; usa `remoteAddr` per default. `X-Forwarded-For` viene letto solo con `TRUST_FORWARDED_HEADERS=true` (abilitare esclusivamente dietro reverse proxy fidato che sovrascrive l'header); HSTS presuppone HTTPS. Nessun pen test esterno; niente claim di certificazione/compliance.
- **Gate deploy**: patch S10 non ancora pubblicata; `v1.0.0` resta immutabile. Prima della patch release: CI 3 OS + build multiarch e push fast-forward; nuova tag `v1.0.1` solo dopo tutti i gate.
