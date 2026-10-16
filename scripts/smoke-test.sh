#!/usr/bin/env bash
#
# Smoke test end-to-end sullo stack reale: applicazione (profilo prod) + PostgreSQL.
#
# Verifica ciò che i test con H2 non possono verificare:
#   1. l'immagine Docker si costruisce;
#   2. lo schema viene creato davvero su PostgreSQL (nessun tipo DDL non portabile);
#   3. il login con l'amministratore del seed funziona (CSRF incluso);
#   4. la creazione di una domanda di lavoro e di un commento persiste su PostgreSQL;
#   5. il volume del database sopravvive al riavvio dell'applicazione.
#
# Uso:
#   bash scripts/smoke-test.sh
#   APP_PORT=18090 POSTGRES_PORT=15432 bash scripts/smoke-test.sh
#
# Richiede: docker con il plugin compose, curl. Funziona su Linux, macOS e Windows
# (Git Bash/WSL); non usa percorsi assoluti.
set -euo pipefail

APP_PORT="${APP_PORT:-18090}"
POSTGRES_PORT="${POSTGRES_PORT:-15432}"
ADMIN_USER="${DAILYPLANET_ADMIN_USERNAME:-administrator}"
ADMIN_PASSWORD="${DAILYPLANET_ADMIN_PASSWORD:-cambiami-password-amministratore}"
MAX_WAIT="${MAX_WAIT:-180}"

cd "$(dirname "$0")/.."
ROOT="$(pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"; docker compose --env-file .env.example down -v >/dev/null 2>&1 || true' EXIT

export APP_PORT POSTGRES_PORT

passo() { printf '\n== %s\n' "$1"; }
ok() { printf '   OK: %s\n' "$1"; }
ko() { printf '   FALLITO: %s\n' "$1" >&2; exit 1; }

passo "Costruzione e avvio dello stack (app + PostgreSQL)"
docker compose --env-file .env.example up -d --build >/dev/null

passo "Attesa che l'applicazione risponda su /actuator/health (max ${MAX_WAIT}s)"
stato=""
for _ in $(seq 1 "$MAX_WAIT"); do
    stato="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${APP_PORT}/actuator/health" || true)"
    [ "$stato" = "200" ] && break
    sleep 1
done
[ "$stato" = "200" ] || { docker compose --env-file .env.example logs app | tail -40; ko "health non disponibile (ultimo stato: $stato)"; }
ok "health 200"

passo "Schema creato su PostgreSQL (tutte le tabelle attese)"
tabelle="$(docker compose --env-file .env.example exec -T db \
    psql -U "${POSTGRES_USER:-dailyplanet}" -d "${POSTGRES_DB:-dailyplanet}" -tAc \
    "select string_agg(tablename, ',' order by tablename) from pg_tables where schemaname='public';")"
for attesa in users categoria domanda_lavoro commento domanda_lavoro_categorie; do
    case ",$tabelle," in
        *",$attesa,"*) ok "tabella $attesa" ;;
        *) ko "tabella '$attesa' assente su PostgreSQL. Trovate: $tabelle" ;;
    esac
done

passo "Login dell'amministratore del seed (con token CSRF)"
curl -s -c "$TMP/cookies" "http://localhost:${APP_PORT}/login" -o "$TMP/login.html"
token="$(grep -oE 'name="_csrf" value="[^"]+"' "$TMP/login.html" | head -1 | sed -E 's/.*value="([^"]+)".*/\1/')"
[ -n "$token" ] || ko "token CSRF non trovato nella pagina di login"
codice="$(curl -s -b "$TMP/cookies" -c "$TMP/cookies" -o /dev/null -w '%{http_code}' \
    -d "username=${ADMIN_USER}&password=${ADMIN_PASSWORD}&_csrf=${token}" \
    "http://localhost:${APP_PORT}/login")"
[ "$codice" = "302" ] || ko "login fallito (HTTP $codice)"
ok "login 302 (credenziali del seed accettate)"

passo "Cambio password obbligatorio al primo accesso"
codice="$(curl -s -b "$TMP/cookies" -o /dev/null -w '%{http_code}' "http://localhost:${APP_PORT}/user")"
[ "$codice" = "302" ] || ko "atteso redirect al cambio password, ricevuto HTTP $codice"
ok "area utente reindirizzata al cambio password (mustChangePassword)"

passo "Creazione di una domanda di lavoro e di un commento (tabelle LONGTEXT/TEXT)"
curl -s -b "$TMP/cookies" "http://localhost:${APP_PORT}/user/changepw" -o "$TMP/changepw.html"
token="$(grep -oE 'name="_csrf" value="[^"]+"' "$TMP/changepw.html" | head -1 | sed -E 's/.*value="([^"]+)".*/\1/')"
NUOVA_PASSWORD="password-smoke-test-1"
curl -s -b "$TMP/cookies" -c "$TMP/cookies" -o /dev/null \
    -d "username=${ADMIN_USER}&password=${ADMIN_PASSWORD}&new_password=${NUOVA_PASSWORD}&confirm_password=${NUOVA_PASSWORD}&_csrf=${token}" \
    "http://localhost:${APP_PORT}/user/doChangepw"

curl -s -b "$TMP/cookies" "http://localhost:${APP_PORT}/user/newdomanda" -o "$TMP/newdomanda.html"
token="$(grep -oE 'name="_csrf" value="[^"]+"' "$TMP/newdomanda.html" | head -1 | sed -E 's/.*value="([^"]+)".*/\1/')"
curl -s -b "$TMP/cookies" -o /dev/null -w '%{http_code}' \
    -d "titolo=Domanda di smoke test&descrizione=Descrizione creata dallo smoke test end-to-end.&closed=false&categorie=Manutenzione&_csrf=${token}" \
    "http://localhost:${APP_PORT}/user/doNewDomanda/SaveQuestion" > "$TMP/codice-domanda"

domande="$(docker compose --env-file .env.example exec -T db \
    psql -U "${POSTGRES_USER:-dailyplanet}" -d "${POSTGRES_DB:-dailyplanet}" -tAc \
    "select count(*) from domanda_lavoro;")"
[ "$domande" = "1" ] || ko "domanda non persistita (righe in domanda_lavoro: $domande)"
ok "domanda persistita su PostgreSQL"

domanda_id="$(docker compose --env-file .env.example exec -T db \
    psql -U "${POSTGRES_USER:-dailyplanet}" -d "${POSTGRES_DB:-dailyplanet}" -tAc \
    "select id from domanda_lavoro order by id limit 1;" | tr -d '[:space:]')"
curl -s -b "$TMP/cookies" "http://localhost:${APP_PORT}/user/domanda/${domanda_id}" -o "$TMP/domanda.html"
token="$(grep -oE 'name="_csrf" value="[^"]+"' "$TMP/domanda.html" | head -1 | sed -E 's/.*value="([^"]+)".*/\1/')"
curl -s -b "$TMP/cookies" -o /dev/null \
    -d "descrizione=Commento creato dallo smoke test end-to-end.&_csrf=${token}" \
    "http://localhost:${APP_PORT}/user/doNewCommento/${domanda_id}"

commenti="$(docker compose --env-file .env.example exec -T db \
    psql -U "${POSTGRES_USER:-dailyplanet}" -d "${POSTGRES_DB:-dailyplanet}" -tAc \
    "select count(*) from commento;")"
[ "$commenti" = "1" ] || ko "commento non persistito (righe in commento: $commenti)"
ok "commento persistito su PostgreSQL"

passo "Persistenza dopo il riavvio dell'applicazione (volume del database)"
docker compose --env-file .env.example restart app >/dev/null
for _ in $(seq 1 "$MAX_WAIT"); do
    stato="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${APP_PORT}/actuator/health" || true)"
    [ "$stato" = "200" ] && break
    sleep 1
done
[ "$stato" = "200" ] || ko "applicazione non ripartita dopo il restart"
domande="$(docker compose --env-file .env.example exec -T db \
    psql -U "${POSTGRES_USER:-dailyplanet}" -d "${POSTGRES_DB:-dailyplanet}" -tAc \
    "select count(*) from domanda_lavoro;")"
[ "$domande" = "1" ] || ko "i dati non sono sopravvissuti al riavvio"
ok "dati conservati nel volume dopo il restart"

printf '\nSmoke test completato: stack %s + PostgreSQL verificati end-to-end.\n' "$(basename "$ROOT")"
