#!/usr/bin/env bash
# P34-T02: räumt E2E-/Test-Artefakte des Login-Users auf.
# Nur eigene Objekte (API erzwingt Ownership: DELETE fremder Objekte -> 403, wird übersprungen).
# NPC-Intents haben keinen DELETE-Endpoint — sie werden mit der Welt unsichtbar
# (deaktivierte Welt) und hier nur gezählt.
#
# Usage:
#   E2E_EMAIL=devbe@test.de E2E_PASSWORD='Test123!' ./scripts/e2e-cleanup.sh [--dry-run] [--filter E2E]
#   API=http://localhost:8080 ./scripts/e2e-cleanup.sh --dry-run
set -u

API="${API:-http://localhost:8080}"
EMAIL="${E2E_EMAIL:-devbe@test.de}"
PASSWORD="${E2E_PASSWORD:-Test123!}"
FILTER="E2E"
DRY_RUN="false"
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN="true" ;;
    --filter) shift ;;
    --filter=*) FILTER="${arg#--filter=}" ;;
  esac
done
# shellcheck disable=SC2001
if [ "${1:-}" = "--filter" ]; then FILTER="${2:-E2E}"; fi

TOKEN="$(curl -s -X POST "$API/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | jq -r '.accessToken // empty')"
[ -z "$TOKEN" ] && { echo "Login fehlgeschlagen ($EMAIL)"; exit 1; }
AUTH=(-H "Authorization: Bearer $TOKEN")
OWNER_ID="$(curl -s "$API/api/v1/worlds" "${AUTH[@]}" | jq -r '.[0].ownerId // empty')"
[ -z "$OWNER_ID" ] && OWNER_ID="$(curl -s "$API/api/v1/game-systems" "${AUTH[@]}" | jq -r '.[0].ownerId // empty')"
[ -z "$OWNER_ID" ] && { echo "Keine eigenen Welten/Systeme — nichts zu tun."; exit 0; }

del() { # del <methode-url> <label>
  if [ "$DRY_RUN" = "true" ]; then echo "  [dry-run] $2"; return 0; fi
  code="$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$1" "${AUTH[@]}")"
  echo "  [$code] $2"
}

echo "== Kampagnen (Hard-Delete, Fork-Welt wird mit deaktiviert) =="
curl -s "$API/api/v1/campaigns" "${AUTH[@]}" \
  | jq -r --arg uid "$OWNER_ID" --arg f "$FILTER" \
    '.[] | select(.name | contains($f)) | "\(.id) \(.name)"' \
  | while read -r id name; do
      [ -z "$id" ] && continue
      del "$API/api/v1/campaigns/$id" "campaign $name ($id)"
    done

echo "== Systeme (Soft-Delete: inaktiv oder Name matcht) =="
curl -s "$API/api/v1/game-systems" "${AUTH[@]}" \
  | jq -r --arg uid "$OWNER_ID" --arg f "$FILTER" \
    '.[] | select(.ownerId == $uid and (.active == false or (.name | contains($f)))) | "\(.id) \(.name) active=\(.active)"' \
  | while read -r id name rest; do
      [ -z "$id" ] && continue
      del "$API/api/v1/game-systems/$id" "system $name ($id $rest)"
    done

echo "== Welten (Soft-Delete, nur eigene mit Namens-Match) =="
curl -s "$API/api/v1/worlds" "${AUTH[@]}" \
  | jq -r --arg uid "$OWNER_ID" --arg f "$FILTER" \
    '.[] | select(.ownerId == $uid and (.name | contains($f))) | "\(.id) \(.name)"' > /tmp/e2e-worlds.txt
while read -r id name; do
  [ -z "$id" ] && continue
  n="$(curl -s "$API/api/v1/npc-intents?worldId=$id" "${AUTH[@]}" | jq 'length')"
  echo "  Welt $name ($id): $n offene Intents (kein DELETE-Endpoint — werden mit Welt unsichtbar)"
  del "$API/api/v1/worlds/$id" "world $name ($id)"
done < /tmp/e2e-worlds.txt
rm -f /tmp/e2e-worlds.txt

echo "Fertig (dry-run=$DRY_RUN, filter=$FILTER)."
