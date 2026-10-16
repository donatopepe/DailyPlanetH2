# Rimozione definitiva dei segreti dalla storia pubblicata (GitHub)

Contesto: la storia git locale è già stata sanificata con `git filter-repo` (task S3.6,
commit `83b97ff`): la credenziale di default introdotta nel 2021 non esiste più in nessun
oggetto locale. GitHub, però, continua a conservare gli oggetti originali non più
raggiungibili e li serve ancora se interrogati direttamente per SHA.

## Stato verificato (2026-10-16)

| Verifica | Comando | Esito |
|---|---|---|
| Branch pubblicato | `git ls-remote origin` | `refs/heads/main` = commit 2021 con il segreto |
| Accessibilità del vecchio commit per SHA | `git fetch --depth 1 origin <sha-vecchio>` in un repo vuoto | **recuperabile** → ancora esposto |
| Credenziali di push | helper `!/usr/bin/gh auth git-credential` | funzionanti |
| Sanificazione locale | scansione di tutti gli oggetti | 0 occorrenze del segreto |

## Passo 1 — rimuovere il riferimento (branch `main`)

Il push di `main` riscritto richiede `--force-with-lease` con il valore atteso esplicito
(decisione D6, mai `--force`):

```bash
cd /home/public/DailyPlanetH2
OLD=$(git ls-remote origin refs/heads/main | cut -f1)     # deve essere il commit 2021
git push --force-with-lease=main:"$OLD" origin main
```

Effetto: il branch di default non espone più il segreto. Gli oggetti originali restano
però *dangling* lato GitHub (recuperabili per SHA) finché GitHub non li elimina.

## Passo 2 — richiesta a GitHub Support per la cancellazione dei dangling object

GitHub non offre un comando per forzare il garbage collection dei repository. La procedura
ufficiale è aprire un ticket a GitHub Support (piano gratuito incluso) chiedendo la
rimozione dei commit non più raggiungibili.

Testo pronto da inviare (sostituire `<SHA-VECCHIO>` con l'hash del commit 2021):

> Oggetto: Richiesta di rimozione di commit non più raggiungibili (segreti esposti) —
> repository `donatopepe/DailyPlanetH2`
>
> Buongiorno,
>
> nel repository `donatopepe/DailyPlanetH2` ho riscritto la storia con
> `git filter-repo` per rimuovere una credenziale di default che era stata
> committata per errore nel 2021 (commit `<SHA-VECCHIO>`, rimosso dal branch
> di default con un push `--force-with-lease`).
>
> Poiché i commit originali rimangono accessibili tramite i loro SHA
> (unreachable/dangling objects), chiedo se è possibile forzare la garbage
> collection del repository o rimuovere manualmente quegli oggetti, in modo che
> il contenuto non sia più recuperabile. La credenziale interessata è già stata
> considerata compromessa e non è più utilizzata.
>
> Grazie.

## Passo 3 — verifica finale

```bash
# il branch di default non deve contenere la stringa del segreto
git fetch origin main && git grep -I '<stringa-segreta>' FETCH_HEAD -- . ; echo "atteso: nessun risultato"

# il vecchio commit non deve più essere scaricabile per SHA
cd $(mktemp -d) && git init -q . && git remote add origin https://github.com/donatopepe/DailyPlanetH2.git
git fetch --depth 1 origin <SHA-VECCHIO>     # atteso: "couldn't find remote ref"
```

## Nota sulla rotazione

La credenziale esposta era la password di default del seed dell'utente amministratore di
un'applicazione locale, mai usata in un ambiente con utenti reali. Misure già adottate:

- il seed ora richiede `DAILYPLANET_ADMIN_PASSWORD` (nessun default in produzione, task S3);
- il primo accesso obbliga al cambio password (`mustChangePassword`);
- nessun segreto è presente nell'albero di lavoro né nella storia locale.
