# Guida utente

Questa guida descrive l'uso del portale dalla parte dell'utente finale. I termini tra
parentesi indicano i percorsi del portale.

## Accesso

1. Aprire il portale e scegliere **Accedi** (oppure `/login`).
2. Inserire nome utente e password.
3. Al **primo accesso** (account creato dal seed o da un amministratore) il portale mostra
   la pagina **Cambio password**: la nuova password deve avere almeno 8 caratteri e deve
   essere diversa da quella attuale. Finché non viene cambiata, ogni altra pagina dell'area
   utente o amministratore riporta forzatamente al cambio password.
4. Il pulsante **Esci** chiude la sessione.

Se le credenziali non sono valide, la pagina di login mostra un messaggio di errore. Un
account **disabilitato** da un amministratore non può accedere.

## Ruolo `ROLE_USER` (utente)

### Area utente (`/user`)

La pagina iniziale dell'area utente raccoglie le azioni disponibili:

- **Nuova domanda di lavoro** (`/user/newdomanda`)
- **Modifica il tuo account** (`/user/edituser`)
- **Cambia password** (`/user/changepw`)
- **Tutte le domande** (`/user/listdomande`) e filtri per categoria o per utente

### Creare una domanda di lavoro

1. Aprire **Nuova domanda di lavoro**.
2. Compilare **Titolo** e **Descrizione** (minimo 8 caratteri ciascuno).
3. Selezionare una o più **categorie**; una categoria nuova viene creata automaticamente,
   una esistente viene riutilizzata.
4. Facoltativamente spuntare **domanda chiusa** (una domanda chiusa resta consultabile, ma
   segnalata come chiusa).
5. Inviare: la domanda compare nell'elenco e nella pagina di dettaglio.

### Consultare e commentare una domanda (`/user/domanda/{id}`)

- In alto sono riportati titolo, descrizione, stato, autore, data e categorie.
- **Commenti**: elenco dei commenti alla domanda; ogni riga ha i collegamenti per
  modificare il commento o la relativa risposta.
- **Risposte**: elenco delle risposte, con il commento a cui si riferiscono.
- **Aggiungi un commento** / **Aggiungi una risposta**: il testo deve avere almeno 8
  caratteri; l'esito viene mostrato nella pagina senza ricaricarla.

### Modificare il proprio account e la password

- **Modifica il tuo account**: nome, cognome, email; il nome utente non può essere già
  usato da un altro account.
- **Cambia password**: richiede la password attuale, la nuova password e la conferma.

## Ruolo `ROLE_ADMIN` (amministratore)

L'amministratore vede anche tutta l'area utente.

### Elenco account (`/admin/account`)

Tabella con id, nome utente, nome, cognome, email, ruolo e stato (attivo/disabilitato), con
i collegamenti **modifica** e **password** per ogni riga.

### Creare un account (`/admin/newaccount`)

Campi: nome utente (minimo 8 caratteri), password (minimo 8 caratteri), nome, cognome,
email, ruolo (`USER` o `ADMIN`) e spunta **disabilitato**. Il nome utente deve essere
unico. L'account creato è obbligato a cambiare la password al primo accesso.

### Modificare un account (`/admin/edit/{id}`)

Consente di aggiornare anagrafica, email, ruolo e stato di disabilitazione. La password non
si modifica da qui: si usa la reimpostazione.

### Reimpostare una password (`/admin/changepw/{id}`)

Inserire la nuova password e la conferma. All'utente non viene richiesta la password
precedente: al successivo accesso dovrà cambiarla di nuovo (impostazione
`mustChangePassword`).

### Assistenza

Se un utente non riesce ad accedere: verificare che l'account non sia **disabilitato** e,
se serve, reimpostarne la password dalla pagina dell'account.

## Sicurezza delle password

- Le password sono memorizzate solo come hash BCrypt: nessuno, nemmeno l'amministratore,
  può leggerle.
- Il modulo di accesso e tutte le operazioni di modifica sono protetti da token CSRF.
- La sessione viene chiusa dal pulsante **Esci**; l'uscita invalida il cookie di sessione.

## Problemi frequenti

| Situazione | Soluzione |
|---|---|
| Il modulo non si invia e compare un errore di accesso negato | la pagina è rimasta aperta troppo a lungo o il token CSRF è scaduto: ricaricare la pagina e ripetere |
| Non ricordo la password | chiedere all'amministratore la reimpostazione |
| "Accesso negato (403)" | l'account non ha i permessi per quella sezione (l'area amministratore è riservata a `ROLE_ADMIN`) |
| Messaggio "Il commento deve avere almeno 8 caratteri" | il testo inserito è troppo corto |
