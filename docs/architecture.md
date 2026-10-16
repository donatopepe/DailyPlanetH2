# Architettura

## Vista d'insieme

DailyPlanetH2 è una applicazione web monolitica a tre livelli, con rendering lato server:

```
Browser (Thymeleaf + CSS, piccoli script fetch per i frammenti AJAX)
   │  HTTP
   ▼
Spring Security 6  ── form login, sessioni, CSRF, autorizzazioni per ruolo
   ▼
Controller (Spring MVC)  ── admin / utente / pubblico
   ▼
Service (logica di dominio)  ── transazioni, uso di SecurityContextHolder
   ▼
Repository (Spring Data JPA + EntityGraph)  ── query e caricamento anticipato
   ▼
Hibernate 6  ── mappatura entità
   ▼
H2 su file (sviluppo) · PostgreSQL (produzione)
```

Il modello è volutamente semplice: nessun layer DTO separato, poche classi per livello e
nessuna infrastruttura esterna obbligatoria (niente code, cache o servizi remoti).

## Componenti principali

| Componente | Responsabilità |
|---|---|
| `DailyPlanetApplication` | punto di ingresso Spring Boot |
| `config/SpringSecurityConfig` | catena di filtri: regole per ruolo, form login, logout, handler 403, bean `PasswordEncoder` (BCrypt) |
| `config/AdminAccountInitializer` | seed idempotente dell'amministratore a partire da variabili d'ambiente; fail-fast in produzione |
| `config/ForcedPasswordChangeInterceptor` + `config/WebMvcConfig` | obbliga il cambio password quando `mustChangePassword` è attivo |
| `controller/DefaultController` | pagine pubbliche (`/`, `/about`, `/home`, `/login`, `/403`) |
| `controller/UserController` | area utente: domande, commenti, risposte, proprio account, cambio password |
| `controller/AdminController` | elenco/creazione/modifica account, reimpostazione password |
| `service/MyUserDetailsService` | autenticazione e gestione account; unico punto che codifica/verifica le password |
| `service/DomandaServiceImpl`, `service/CommentoServiceImpl`, `service/CategoriaServiceImpl` | regole di dominio per domande, commenti e categorie |
| `error/MyAccessDeniedHandler` | redirezione a `/403` per accessi negati |
| `error/GlobalExceptionHandler` | `@ControllerAdvice`: 404 (`NoSuchElementException`, `IllegalArgumentException`, `NoResourceFoundException`), 409, 500 con log SLF4J |

## Flusso di una richiesta tipica

1. Il filtro di sicurezza verifica la sessione e l'autorità richiesta dalla rotta.
2. Il controller prepara gli attributi del model richiesti dalla vista, delegando ai
   service.
3. Il service legge/scrive tramite i repository; le password passano sempre da
   `PasswordEncoder`.
4. Thymeleaf rende la pagina completa, oppure solo un frammento per le chiamate AJAX.
5. Gli errori non gestiti finiscono in `GlobalExceptionHandler`, che registra l'eccezione e
   mostra una pagina dedicata con lo stato HTTP corretto.

## Decisioni progettuali rilevanti

- **`open-in-view: false`**: le sessioni Hibernate sono chiuse a fine richiesta, quindi
  ogni associazione letta dai template viene caricata con `@EntityGraph` nei repository.
  Questo evita query implicite durante il rendering e rende esplicito il costo delle query.
- **CSRF attivo**: i form usano `th:action` (Thymeleaf inserisce il token) e le chiamate
  `fetch` prendono il token dal tag `<meta name="_csrf">` del layout condiviso.
- **Nessun segreto nel codice**: l'unico account iniziale nasce da variabili d'ambiente;
  in produzione l'avvio fallisce se `DAILYPLANET_ADMIN_PASSWORD` non è impostata.
- **Cross-platform**: UTF-8 imposto su compilazione, test e runtime; percorsi sempre
  relativi (H2 in `./data`); fine riga normalizzati da `.gitattributes`; wrapper Gradle
  presente per Linux, macOS e Windows.
- **Test come specifica**: la suite (55 test) include una matrice rotte × ruoli e un flusso
  end-to-end che verifica il percorso reale di login e creazione dei dati.

## Estensioni previste

- Immagine container multi-architettura e `docker-compose` con PostgreSQL.
- CI su Linux, macOS e Windows.
- Migrazioni versionate dello schema (Flyway/Liquibase).
