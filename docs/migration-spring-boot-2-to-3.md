# Note di migrazione da Spring Boot 2 (2021) a Spring Boot 3.5

Il progetto originale era una applicazione Spring Boot 2.x con `javax.*`, senza build file
e senza risorse (template, configurazione, schema). Questa nota riassume cosa è cambiato e
perché, utile a chi ha un fork o un checkout vecchio.

## Cambi di piattaforma

| Prima (2021) | Ora | Motivo |
|---|---|---|
| Nessun build file, layout piatto (stile IDE) | `build.gradle` + wrapper Gradle 8.14, sorgenti in `src/main/java/...` | build riproducibile su qualunque macchina |
| `javax.persistence` / `javax.validation` / `javax.servlet` | `jakarta.*` | obbligatorio da Spring Boot 3 |
| `WebSecurityConfigurerAdapter` + `authorizeRequests()` + `antMatchers()` | bean `SecurityFilterChain` + `authorizeHttpRequests()` + `requestMatchers()` | `WebSecurityConfigurerAdapter` rimosso in Spring Security 6 |
| CSRF disattivato (`csrf().disable()`) | CSRF **attivo** | protezione standard; i form usano `th:action`, le chiamate AJAX il meta token |
| `new Sort(Sort.Direction.ASC, "campo")` | `Sort.by(Sort.Direction.ASC, "campo")` | costruttore rimosso nelle versioni recenti di Spring Data |
| Configurazione assente | `application.yml` + profili `dev`/`prod`, variabili d'ambiente | nessun default segreto, deploy 12-factor |
| Template Thymeleaf assenti | `src/main/resources/templates/**` (layout + pagine + frammenti) | le viste referenziate dai controller ora esistono |
| Nessun test | 55 test (unit, JPA, rendering, matrice ruoli, E2E) | ogni difetto corretto è coperto da una regressione |

## Correzioni funzionali

1. **Autenticazione**: il codice ri-codificava la password caricata dal database e la
   salvava in chiaro quando un flag era disattivo, rendendo impossibile il login. Ora
   l'hash BCrypt è memorizzato così com'è e verificato con `PasswordEncoder.matches`.
2. **Credenziale di default scritta nel codice**: l'account `administrator` con password
   fissa è stato eliminato (anche dalla storia git, riscritta con `git filter-repo`); il
   seed legge `DAILYPLANET_ADMIN_PASSWORD` e in produzione l'avvio fallisce se manca.
3. **Cambio password obbligatorio**: nuovo campo `User.mustChangePassword` e interceptor
   che forza la pagina di cambio password al primo accesso.
4. **Commenti**: l'associazione con la domanda era `@OneToOne`, quindi il database
   impediva più di un commento per domanda; ora è `@ManyToOne`.
5. **Associazione categorie**: entrambi i lati erano proprietari (due tabelle di join
   duplicate); ora il lato categoria è `mappedBy = "categorie"`.
6. **Binding delle variabili di percorso**: su `POST /user/doNewCommento/{id}` e
   `/user/doNewAnswer/{id}` la variabile `id` veniva legata al campo `id` dell'entità
   `Commento`, facendo credere a Hibernate che la riga esistesse già. Ora il testo arriva
   dal parametro `descrizione` e l'entità è costruita dal controller.
7. **Caricamento lazy**: con `open-in-view: false` le associazioni usate dai template sono
   caricate con `@EntityGraph` nei repository (domande e commenti), evitando
   `LazyInitializationException`.
8. **Errori HTTP**: le eccezioni non gestite restituivano HTTP 200 con la pagina di errore;
   ora lo stato è corretto (404/409/500) e gli errori sono registrati con metodo e percorso.

## Migrazione dei dati esistenti

Lo schema è creato/aggiornato da Hibernate (`ddl-auto: update`). Per i database di sviluppo
H2 creati prima di questa versione:

```bash
rm -rf data            # Linux / macOS
rmdir /s /q data       # Windows cmd
```

Per un database PostgreSQL di produzione:

1. fare un backup (`pg_dump`), quindi applicare le modifiche su un ambiente di prova;
2. la tabella `user` è stata rinominata in `users` (`ALTER TABLE "user" RENAME TO users;`);
3. l'eventuale tabella `categoria_domande` non è più usata e può essere eliminata dopo aver
   verificato che le associazioni in `domanda_lavoro_categorie` siano complete;
4. le password esistenti in chiaro devono essere rigenerate: al primo accesso usare la
   reimpostazione da parte dell'amministratore, che salva l'hash BCrypt.

## Verifica dopo la migrazione

```bash
./gradlew clean test                                     # 55 test attesi, 0 failure
./gradlew bootRun --args='--spring.profiles.active=dev'  # avvio con H2
```

Controlli funzionali minimi: login dell'amministratore, cambio password obbligatorio,
creazione di una domanda con due categorie, aggiunta di due commenti alla stessa domanda,
visualizzazione della pagina della domanda, accesso negato all'area amministratore con un
utente `ROLE_USER`.
