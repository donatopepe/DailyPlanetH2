# DailyPlanetH2

Portale di gestione degli **ordini di lavoro**: apertura e classificazione delle domande,
commenti e risposte, categorie, gestione degli account e delle password.

*Progetto originale del 2021 migrato a uno stack attuale e reso distribuibile:
Spring Boot 3.5, Java 17, Spring Security 6, Thymeleaf, H2/PostgreSQL, wrapper Gradle.*

- Licenza: MIT (vedi [LICENSE](LICENSE))
- Documentazione in inglese: [README.md](README.md)
- Piano e stato di avanzamento: [TODO.md](TODO.md)

---

## Indice

1. [Funzionalità](#funzionalità)
2. [Stack tecnologico](#stack-tecnologico)
3. [Requisiti](#requisiti)
4. [Avvio rapido](#avvio-rapido)
5. [Configurazione](#configurazione)
6. [Database](#database)
7. [Ruoli e flussi principali](#ruoli-e-flussi-principali)
8. [Rotte HTTP](#rotte-http)
9. [Test](#test)
10. [Build, avvio del jar e Docker](#build-avvio-del-jar-e-docker)
11. [Struttura del progetto](#struttura-del-progetto)
12. [Risoluzione dei problemi](#risoluzione-dei-problemi)
13. [Roadmap](#roadmap)
14. [Licenza](#licenza)

## Funzionalità

- **Ordini di lavoro**: creazione, modifica e chiusura di una domanda con titolo,
  descrizione, categorie e autore.
- **Commenti e risposte**: più commenti per domanda, risposte collegate a un commento
  specifico, con autore e date.
- **Categorie**: una domanda può appartenere a più categorie; le categorie vengono
  ritrovate o create per nome.
- **Account**: area amministratore per elencare, creare, modificare e disabilitare gli
  account e per reimpostare le password.
- **Sicurezza delle password**: solo hash BCrypt (mai in chiaro, mai nei log), password
  attuale richiesta per il cambio volontario, **cambio password obbligatorio al primo
  accesso** per gli account creati dal seed o da un amministratore.
- **Controllo degli accessi**: form login, due ruoli (`ROLE_USER`, `ROLE_ADMIN`),
  protezione CSRF su ogni form e chiamata AJAX, pagina 403 personalizzata.
- **Account iniziale**: il primo amministratore è creato da una variabile d'ambiente,
  mai da una credenziale scritta nel codice.
- **Gestione errori**: `@ControllerAdvice` centralizzato con pagine 404/409/500 e log
  privi di dati sensibili.

## Stack tecnologico

| Livello | Scelta |
|---|---|
| Linguaggio | Java 17 (toolchain Gradle, scaricata automaticamente se assente) |
| Framework | Spring Boot 3.5.16 |
| Web | Spring MVC + Thymeleaf |
| Sicurezza | Spring Security 6 (form login, BCrypt, CSRF) |
| Persistenza | Spring Data JPA / Hibernate 6 |
| Database | H2 (file, sviluppo) · PostgreSQL (produzione) |
| Build | Gradle 8.14 con wrapper incluso nel repository |
| Test | JUnit 5, Mockito, MockMvc, `@DataJpaTest`, `@SpringBootTest` |

## Requisiti

- **JDK 17 o superiore**. La toolchain Gradle scarica automaticamente il JDK 17 se manca
  (`org.gradle.toolchains.foojay-resolver-convention`), quindi un JDK locale è comodo ma
  non obbligatorio.
- Nessuna installazione di Gradle: si usa il wrapper (`./gradlew`) incluso nel repository.
- Database: nessuno da installare in sviluppo (H2 su file), PostgreSQL in produzione.

## Avvio rapido

I comandi sono identici su Linux, macOS e Windows (su Windows `cmd`/PowerShell si usa
`gradlew.bat` al posto di `./gradlew`).

### Linux / macOS

```bash
git clone https://github.com/donatopepe/DailyPlanetH2.git
cd DailyPlanetH2

# opzionale: scegli la password dell'amministratore iniziale (in dev il default è "admin")
export DAILYPLANET_ADMIN_PASSWORD='scegli-una-password-robusta'

./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Windows (PowerShell)

```powershell
git clone https://github.com/donatopepe/DailyPlanetH2.git
Set-Location DailyPlanetH2

$env:DAILYPLANET_ADMIN_PASSWORD = 'scegli-una-password-robusta'

.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

### Windows (cmd)

```bat
git clone https://github.com/donatopepe/DailyPlanetH2.git
cd DailyPlanetH2
set DAILYPLANET_ADMIN_PASSWORD=scegli-una-password-robusta
gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

Poi aprire <http://localhost:8080/login> e accedere con l'amministratore iniziale
(`administrator` / la password impostata, default `admin` nel profilo dev). Al primo
accesso il portale **obbliga il cambio password**.

## Configurazione

Tutta la configurazione è in `src/main/resources/application.yml` e nei file di profilo
`application-dev.yml` / `application-prod.yml`. Ogni valore è sovrascrivibile con una
variabile d'ambiente. Su Windows: `set VAR=valore` (cmd) oppure `$env:VAR = 'valore'`
(PowerShell).

| Variabile | Default | Descrizione |
|---|---|---|
| `SERVER_PORT` | `8080` | Porta HTTP |
| `DAILYPLANET_ADMIN_USERNAME` | `administrator` | Nome utente dell'amministratore iniziale |
| `DAILYPLANET_ADMIN_EMAIL` | `admin@example.invalid` | Email dell'amministratore iniziale |
| `DAILYPLANET_ADMIN_PASSWORD` | `admin` (**solo profilo dev**) | Password dell'amministratore iniziale. **Obbligatoria in produzione**: senza di essa l'avvio fallisce |
| `DB_URL` | – (richiesta in prod) | URL JDBC, es. `jdbc:postgresql://localhost:5432/dailyplanet` |
| `DB_USERNAME` | – (richiesta in prod) | Utente del database |
| `DB_PASSWORD` | – (richiesta in prod) | Password del database |
| `DB_POOL_SIZE` | `10` | Dimensione massima del pool HikariCP (prod) |

Profili:

| Profilo | Database | Note |
|---|---|---|
| `dev` (default) | H2 su file `./data/dailyplanet` | console H2 su `/h2-console`, default documentato per la password admin |
| `prod` | PostgreSQL | nessuna credenziale di default, console H2 disabilitata, `forward-headers-strategy: framework` |

## Database

### Sviluppo (H2, predefinito)

Il profilo di sviluppo usa un database H2 **su file** in `./data/dailyplanet`: percorso
relativo alla directory di lavoro, quindi identico su Linux, macOS e Windows. Lo schema è
gestito da Hibernate (`ddl-auto: update`).

> **Dopo l'aggiornamento da un checkout precedente**: l'associazione categoria–domanda è
> stata corretta (il lato inverso non è più un secondo proprietario), quindi la tabella
> obsoleta `categoria_domande` non esiste più. Cancellare il database di sviluppo e
> lasciarlo ricreare: `rm -rf data` (Linux/macOS) oppure `rmdir /s /q data` (Windows cmd).

### Produzione (PostgreSQL)

```bash
export DB_URL='jdbc:postgresql://localhost:5432/dailyplanet'
export DB_USERNAME='dailyplanet'
export DB_PASSWORD='…'
export DAILYPLANET_ADMIN_PASSWORD='…'
java -jar build/libs/DailyPlanetH2-1.0.0.jar --spring.profiles.active=prod
```

Lo schema è gestito da Hibernate (`ddl-auto: update`); le migrazioni versionate
(Flyway/Liquibase) sono in roadmap.

## Ruoli e flussi principali

| Ruolo | Può fare |
|---|---|
| Anonimo | leggere `/`, `/about`, `/home`, autenticarsi |
| `ROLE_USER` | tutto sotto `/user/**`: creare e modificare domande, commentare, rispondere, cambiare la propria password, modificare il proprio account |
| `ROLE_ADMIN` | tutto sotto `/admin/**`: elencare/creare/modificare/disabilitare account, reimpostare password — più l'area `/user/**` |

Flusso dell'amministratore iniziale: primo accesso → cambio password obbligatorio → uso
normale. Account creato da un amministratore: `mustChangePassword` attivo, l'utente la
cambia al primo accesso.

## Rotte HTTP

| Metodo | Percorso | Accesso | Scopo |
|---|---|---|---|
| GET | `/`, `/home`, `/about` | pubblico | pagina iniziale |
| GET | `/login`, POST `/login` | pubblico | form login |
| POST | `/logout` | autenticato | logout (protetto da CSRF) |
| GET | `/403` | pubblico | pagina accesso negato |
| GET | `/user` | USER, ADMIN | area utente |
| GET | `/user/newdomanda`, POST `/user/doNewDomanda/SaveQuestion` | USER, ADMIN | creazione domanda |
| GET | `/user/domanda/{id}` | USER, ADMIN | dettaglio domanda con commenti/risposte |
| POST | `/user/doNewCommento/{id}`, POST `/user/doNewAnswer/{id}` | USER, ADMIN | aggiunta commento / risposta |
| GET | `/user/edituser`, POST `/user/doEditUser` | USER, ADMIN | modifica del proprio account |
| GET | `/user/changepw`, POST `/user/doChangepw` | USER, ADMIN | cambio della propria password |
| GET | `/user/listdomande...` | USER, ADMIN | elenchi domande (frammenti) |
| GET | `/admin` | ADMIN | area amministratore |
| GET | `/admin/account`, `/admin/newaccount`, `/admin/edit/{id}` | ADMIN | elenco / creazione / modifica account |
| GET | `/admin/changepw/{id}`, POST `/admin/doAdminChangepw` | ADMIN | reimpostazione password |

Nota AJAX: gli endpoint a frammento accettano `POST` con il token CSRF preso dal tag
`<meta name="_csrf">` incluso dal layout condiviso (`window.dpCsrf.fetch(...)`).

## Test

```bash
./gradlew clean test        # Linux / macOS
gradlew.bat clean test      # Windows
```

55 test automatici:

| Classe di test | Ambito |
|---|---|
| `DailyPlanetH2ApplicationTests` | avvio del contesto applicativo |
| `MyUserDetailsServiceTest` | hash BCrypt, login, utenti disabilitati, cambio password |
| `AdminAccountInitializerTest` | idempotenza del seed, fail-fast senza password in prod |
| `CategoriaServiceImplTest`, `DomandaServiceImplTest`, `CommentoServiceImplTest` | logica dei servizi con repository simulati |
| `UserRepositoryTest`, `DomandaLavoroRepositoryTest` | mappatura JPA, query, caricamento `EntityGraph`, più commenti per domanda |
| `ViewRenderingTest` | rendering di tutte le viste/frammenti Thymeleaf |
| `RoleAccessMatrixTest` | matrice rotte × ruoli (anonimo → login, USER → 403 su admin, ADMIN → tutto) |
| `EndToEndFlowTest` | login → crea domanda → commento → lettura → logout |

Report HTML: `build/reports/tests/test/index.html`.

## Build, avvio del jar e Docker

```bash
./gradlew clean build
java -jar build/libs/DailyPlanetH2-1.0.0.jar --spring.profiles.active=dev --server.port=8080
```

Endpoint di salute (pubblico, senza autenticazione): `GET /actuator/health` → `{"status":"UP"}`.

### Docker

Prerequisiti: Docker con il plugin Compose v2.

```bash
cp .env.example .env       # poi modificare i valori (le password sono obbligatorie)
docker compose up --build   # applicazione su http://localhost:8080 + PostgreSQL
docker compose down         # con -v elimina anche il volume del database
```

Cosa fa lo stack:

- `db`: PostgreSQL 17 con volume dedicato (`db-data`) e healthcheck `pg_isready`;
- `app`: immagine costruita dal `Dockerfile`, `SPRING_PROFILES_ACTIVE=prod`, credenziali di
  database e del seed iniettate da `.env`, healthcheck del container su `/actuator/health`;
- ordine di avvio: l'applicazione attende che il database sia pronto;
- nessuna password di default: se manca una variabile, Compose si ferma con un messaggio
  esplicito e l'applicazione rifiuta di partire (vedi `ProductionEnvironmentValidator`).

Immagine singola:

```bash
docker build -t dailyplaneth2:1.0.0 .
docker run --rm -e SPRING_PROFILES_ACTIVE=dev -p 8080:8080 dailyplaneth2:1.0.0
```

Immagine multi-architettura (linux/amd64 + linux/arm64) — richiede buildx e QEMU:

```bash
docker buildx build --platform linux/amd64,linux/arm64 -t dailyplaneth2:1.0.0 .
```

La stessa build gira in CI ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) insieme
alla suite di test su Linux, macOS e Windows.


## Struttura del progetto

```
build.gradle, settings.gradle   build Gradle (plugin Spring Boot, toolchain Java 17)
gradlew, gradlew.bat, gradle/   wrapper Gradle 8.14
src/main/java/com/pepedonato/DailyPlanetH2/
    config/       sicurezza, interceptor MVC, seed dell'amministratore
    controller/   controller admin, utente e pubblici
    error/        gestore accesso negato + gestore centralizzato delle eccezioni
    model/        entità JPA e oggetti di form
    repository/   repository Spring Data (con EntityGraph)
    service/      servizi di dominio
src/main/resources/
    application*.yml         configurazione (comune, dev, prod)
    templates/               viste Thymeleaf (layouts/ + pagine + frammenti AJAX)
    static/css/app.css       foglio di stile minimale, nessuna risorsa esterna
src/test/java/...            55 test automatici
docs/                        architettura, modello dati, guida utente, note di migrazione
TODO.md                      piano di lavoro, decisioni, evidenze
```

## Risoluzione dei problemi

| Sintomo | Causa / soluzione |
|---|---|
| `401/403` su un POST di un form | token CSRF mancante: usare `th:action` nei form e `window.dpCsrf.fetch` nelle chiamate AJAX |
| Login dell'amministratore iniziale non funziona | la password viene da `DAILYPLANET_ADMIN_PASSWORD`; in dev il default è `admin` e il primo accesso obbliga al cambio |
| Avvio fallito con `Profilo 'prod': variabili d'ambiente obbligatorie mancanti: DB_URL, DB_USERNAME, DB_PASSWORD, DAILYPLANET_ADMIN_PASSWORD` | comportamento previsto nel profilo `prod`: impostare tutte le variabili elencate (nessun default segreto; il controllo avviene prima dell'avvio del contesto) |
| Avvio fallito con `DAILYPLANET_ADMIN_PASSWORD non impostata` | database configurato ma password del seed assente nel profilo `prod`: impostare la variabile |
| Errori di schema/vincoli in sviluppo | database H2 obsoleto: `rm -rf data` e riavviare |
| `Could not find or load main class` su Windows | usare `gradlew.bat` ed eseguirlo dalla radice del repository |
| Pagina "Si è verificato un errore" | controllare il log: l'eccezione è registrata con metodo e percorso della richiesta |

## Roadmap

- Migrazioni di schema versionate (Flyway/Liquibase).
- Screenshot e breve video dimostrativo nel README.
- Opzionali: login OIDC/Keycloak, notifiche e-mail sui nuovi commenti.

## Licenza

MIT © Donato Pepe — vedi [LICENSE](LICENSE).
