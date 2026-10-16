# Modello dati

Diagramma delle entità persistite (Java 17, Jakarta Persistence 3.1, Hibernate 6).

```
┌──────────────────────────┐             ┌──────────────────────────┐
│ users                    │             │ categorie                │
├──────────────────────────┤             ├──────────────────────────┤
│ id (PK)                  │             │ id (PK)                  │
│ username (unique,        │             │ nome (unique, not null,  │
│   not null, min 8)       │             │   min 3)                 │
│ password (BCrypt hash)   │             └───────────┬──────────────┘
│ firstname, lastname      │                         │  (lato inverso)
│ email (valid)            │                         │  mappedBy = "categorie"
│ userrole (ADMIN/USER)    │                         │
│ disabled (default false) │                         │
│ must_change_password     │                         │
└───────┬──────────────────┘                         │
        │ 1                                          │
        │                                            │
        │ N                                          │
┌───────▼─────────────────────────────────────────────▼──────────────┐
│ domanda_lavoro                                                     │
├────────────────────────────────────────────────────────────────────┤
│ id (PK) · titolo (not null, min 8) · descrizione (LONGTEXT, min 8) │
│ datacreazione · datamodifica · closed (default false)              │
│ user_id (FK → users.id, not null)                                  │
│ domanda_lavoro_categorie (tabella di join, lato proprietario)      │
└───────┬────────────────────────────────────────────────────────────┘
        │ 1
        │
        │ N
┌───────▼───────────────────────────────┐
│ commento                              │
├───────────────────────────────────────┤
│ id (PK) · descrizione (LONGTEXT, min 8)│
│ risposta (true = risposta a un commento)│
│ datacreazione · datamodifica          │
│ user_id (FK → users.id, not null)     │
│ domanda_id (FK → domanda_lavoro.id)   │
│ commento_id (FK → commento.id,        │
│   commento genitore, solo per risposte)│
└───────────────────────────────────────┘
```

## Entità

| Entità | Tabella | Note |
|---|---|---|
| `User` | `users` | nome `user` evitato perché riservato in H2 2.x e PostgreSQL; `userrole` contiene `ADMIN` o `USER` e diventa l'autorità `ROLE_*`; `mustChangePassword` obbliga il cambio password al primo accesso |
| `Categoria` | `categorie` | `nome` unico, minimo 3 caratteri; lato **inverso** dell'associazione con le domande |
| `DomandaLavoro` | `domanda_lavoro` | lato **proprietario** dell'associazione con le categorie (`domanda_lavoro_categorie`); titolo e descrizione obbligatori, minimo 8 caratteri |
| `Commento` | `commento` | `risposta = false` → commento alla domanda; `risposta = true` → risposta collegata a un commento genitore tramite `commento_id`; una domanda può avere molti commenti (`@ManyToOne`) |

## Oggetti di form (non persistiti)

| Classe | Uso |
|---|---|
| `DomandaView` | binding del form di creazione/modifica domanda (`titolo`, `descrizione`, `closed`, `categorie` come elenco di nomi) |
| `ChangePwForm` | cambio password (`username`, `password`, `new_password`, `confirm_password`) |
| `UserNoPW` / `UserNoPWRule` | modifica account senza gestione della password (admin / utente) |

## Regole di integrità applicate dal codice

- La password è **sempre** un hash BCrypt in database; la verifica usa
  `PasswordEncoder.matches` e non esiste alcuna query di confronto su password.
- Cambio password volontario: richiede la password attuale, nuova e conferma uguali, e
  minimo 8 caratteri.
- Cambio password da amministratore: non richiede la password attuale, azzera
  `mustChangePassword`.
- Le categorie sono ritrovate per nome (case-insensitive) e create solo se assenti; la
  lista passata dal form non viene mai modificata dal service.
- Eliminare/chiudere una domanda non elimina utenti o categorie.

## Nota sulle migrazioni

Lo schema è gestito da Hibernate (`spring.jpa.hibernate.ddl-auto: update`). La correzione
del lato inverso dell'associazione (in precedenza entrambi i lati erano proprietari, con
due tabelle di join duplicate) rende obsoleta la tabella `categoria_domande`: nei database
di sviluppo basta cancellare `./data` e riavviare. L'introduzione di migrazioni versionate
(Flyway/Liquibase) è in roadmap.
