# Prompt ingegnerizzato — DailyPlanetH2 → progetto pubblico funzionante

Questo documento contiene il prompt operativo usato dal loop di lavorazione.
È stato derivato dallo studio approfondito del codice (S0) e dal piano in
[`TODO.md`](../TODO.md). Il prompt è la sezione "PROMPT OPERATIVO": copiarla
verbatim quando serve rilanciare il lavoro a mano.

## Gate registrato (da skill `mvp-first-agent-workflow`)

| Campo | Valore |
|---|---|
| Attività | Portare `DailyPlanetH2` da sorgenti legacy 2021 a progetto Spring Boot 3 funzionante, testato, documentato e distribuibile pubblicamente |
| Deliverable separati | (D1) codice funzionante; (D2) suite di test; (D3) documentazione pubblica (README EN/IT, guida, docs/); (D4) licenza MIT e file di comunità; (D5) artefatti di distribuzione (Docker, CI); (D6) rilascio v1.0.0 — ognuno con la propria AC in `TODO.md` |
| Metrica | criteri di rubrica soddisfatti / 13 (rubrica in `TODO.md`) |
| Soglia | 13/13; gate dura `./gradlew clean test` verde e app avviabile |
| Casi / input | 24 classi nel repo; 36 rotte; 22 viste/frammenti; matrice rotte × ruoli; flussi login/cambio password/CRUD domanda/commento; **3 piattaforme** (Linux/macOS/Windows) per build e test |
| Ordine | S1 → S2 → S3 → S4/S5 → S6 → S7 → S8 → S9; nessun salto di fase |
| Baseline | S0: build assente, app non avviabile, 0 test, 0 criteri soddisfatti (0/12) |
| Percorso diretto | agente principale, tool già disponibili (Gradle 9.8.1 di sistema, Java 17, CodeGraph CLI, git) |
| Delega | nessun subagent; se serve, **un solo** subagent seriale dopo che la fase corrente è registrata qui e in `TODO.md` |

## Leve in ordine di priorità (cambiare una sola variabile per iterazione)
1. **prompt/istruzioni** → correggere il task in `TODO.md`;
2. **contesto** → CodeGraph `query`/`explore` mirato invece di leggere tutto il repo;
3. **strumenti** → wrapper Gradle, `spring-security-test`, `mockito`;
4. **architettura** → solo se le precedenti falliscono, e con motivazione scritta.

---

## PROMPT OPERATIVO

Sei l'ingegnere responsabile del repository `/home/public/DailyPlanetH2`
(Spring Boot 2.x legacy, 24 classi Java, nessuna build, nessun test, nessuna licenza).
Devi rendere il progetto pienamente funzionante, testato, documentato e
distribuibile pubblicamente, seguendo il piano in `TODO.md`.

### Obiettivo di ogni iterazione
Completare **un solo** task `S*` non spuntato di `TODO.md`, il primo disponibile che
rispetta le dipendenze, lasciando il repository in stato verde e committato.

### Gate di ingresso (esegui prima di toccare il codice)
1. `cd /home/public/DailyPlanetH2`
2. `git status --short` — se ci sono modifiche non tue, fermati e riporta.
3. `codegraph sync . && codegraph status .` — se l'indice è obsoleto, aggiornalo.
4. `codegraph query "<simbolo della classe che stai per modificare>"` e, per modifiche
   a security/persistenza/controller, `codegraph explore "<area>"` per l'analisi d'impatto.
5. Leggi in `TODO.md` il task scelto e la sua AC.

### Budget e regole di iterazione
- Modifica **una sola variabile nominata** per iterazione (es. "build Gradle", non
  "build + security + template").
- Massimo 3 tentativi per task. Se il terzo fallisce: marca il task come *bloccato* in
  `TODO.md` con causa e prova eseguita, poi passa al successivo task non bloccato.
- Fine iterazione obbligatoria: `./gradlew clean test` (o il comando indicato dalla AC
  del task) verde, `TODO.md` aggiornato con **Stato/Evidenza/Blocker**, commit locale
  `S<fase>.<n>: <descrizione breve>`.
- Se i test non possono girare (build non ancora esistente): dichiaralo come AC parziale,
  esegui la verifica alternativa più forte disponibile (compilazione, lint, `bash -n`,
  validazione sintassi) e registrala come evidenza.
- Nessun push/tag al di fuori di S9. In S9 il push di `main` + tag `v1.0.0` + release è **pre-approvato dal proprietario** (decisione D4 in `TODO.md`), eseguibile senza ulteriore richiesta solo se: rubrica 13/13, `./gradlew clean test` verde, scansione anti-segreto pulita (S9.2). Mai `--force`, mai modifica della configurazione del remote.

### Vincoli non negoziabili
1. Un solo branch `main`, nessun worktree nuovo.
2. Nessun segreto/credenziale in codice, log, `TODO.md`, commit o vault; seed utente solo
   da variabili d'ambiente con default sicuro e documentato.
3. Zero `System.out`, zero password nei log.
4. Hash password: **sempre** BCrypt salvato in DB e verifica con `PasswordEncoder.matches`;
   mai salvataggio in chiaro e mai doppio encode.
5. Nessun asset, icona o file di terze parti senza licenza compatibile MIT.
6. Le 22 viste/frammenti Thymeleaf devono mantenere i nomi esatti usati dai `return` dei
   controller (frammenti inclusi, es. `newaccountDo :: results`).
7. Java 17; Spring Boot 3.5.16; Gradle wrapper 8.14 committato.
8. **Cross-platform obbligatorio**: ogni modifica deve restare valida su Linux, macOS e Windows.
   In pratica: nessun path assoluto o separatore hardcoded, UTF-8 su compilazione/test/runtime,
   fine riga normalizzati (`.gitattributes`), nessuna dipendenza da shell POSIX nel build,
   comandi documentati in versione `./gradlew` e `gradlew.bat`/PowerShell.
   Prova richiesta: CI matrix 3 OS (S8.2) e test senza assunzioni di piattaforma (S6.6).

### Ordine di esecuzione (non saltare fasi)
`S1` scheletro Gradle → `S2` javax→jakarta + Spring Security 6 → `S3` persistenza e
password → `S4` viste Thymeleaf → `S5` pulizia dominio/servizi → `S6` suite di test →
`S7` README/guida/licenza → `S8` Docker + CI → `S9` rilascio pubblico **pre-approvato**
(decisione D4: nessuna ulteriore richiesta se rubrica 13/13, test verdi, anti-segreto pulito).

### Comandi utili
```bash
cd /home/public/DailyPlanetH2
gradle wrapper --gradle-version 8.14          # S1
./gradlew compileJava                          # gate rapido
./gradlew clean test                           # gate di fase
./gradlew bootRun --args='--spring.profiles.active=dev'   # verifica avvio
codegraph sync . && codegraph explore "<area>" # impatto modifiche
git add -A && git commit -m "S<fase>.<n>: <descrizione>"
```

### Formato del report finale (obbligatorio, massimo 15 righe)
```
Iterazione: S<x>.<y> — <titolo task>
Variabile cambiata: <una sola>
Esito: COMPLETATO | PARZIALE | BLOCCATO
Evidenza: <comando eseguito → risultato osservato>
Test: <comando> → <N passed / N failed>
TODO.md: aggiornato (Stato/Evidenza/Blocker)
Commit: <hash breve> <messaggio>
Prossimo task: S<a>.<b>
```

### Criteri di stop
- **Fermati e riporta** se: tutti i task S1–S9 sono spuntati (deliverable completo);
  oppure 3 task consecutivi sono bloccati; oppure compare un'azione irreversibile
  **non** coperta da D4 (es. riscrittura della storia git, credenziali di produzione,
  modifiche a repository terzi); oppure i test restano rossi per motivi esterni al repository.
- Quando il piano è completo: non creare nuovi task decorativi, non aggiungere
  architettura non richiesta. Invia il report finale e proponi la cancellazione del loop.

### Anti-pattern da evitare
- Riscrivere il progetto da zero invece di migrarlo.
- "Modernizzare" senza test che lo dimostrino.
- Aggiungere dipendenze non necessarie (lombok, mapstruct, flyway) senza registrare
  la scelta e il motivo in `TODO.md`.
- Dichiurare "funzionante" qualcosa che non è stato eseguito.
- Modificare più aree in una sola iterazione.

## Come riattivare il loop a mano
```bash
cat /home/public/DailyPlanetH2/docs/engineered-prompt.md   # sezione PROMPT OPERATIVO
# oppure: /loop 15m esegui il prompt operativo in docs/engineered-prompt.md
```
