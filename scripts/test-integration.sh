#!/bin/bash
# P16-T09: Integrationstest für D&D 5e, CoC 7e, DSA 5
# Nutzt die Beispiel-JSONs aus docs/examples/
set -e

BASE="http://192.168.31.151:8080/api/v1"
TOTAL=0
PASS=0

login() {
  TOKEN=$(curl -s "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d '{"email":"cenksaatci@googlemail.com","password":"test123"}' \
    | python3 -c "import sys,json;print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
  if [ -z "$TOKEN" ]; then echo "LOGIN FAILED"; exit 1; fi
  export TOKEN
}

test_system() {
  local NAME=$1 FILE=$2
  TOTAL=$((TOTAL+1))
  echo ""
  echo "=== Testing: $NAME ($FILE) ==="

  # 1. Create game system from JSON
  local RULES=$(cat "$FILE")
  local RES=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"$NAME\",\"version\":1,\"rulesJson\":$(echo "$RULES" | jq -c . | jq -R -s -c .),\"schemaJson\":\"{}\"}" \
    "$BASE/game-systems")
  local HTTP=$(echo "$RES" | tail -1)
  local BODY=$(echo "$RES" | head -n -1)
  local SYS_ID=$(echo "$BODY" | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null)

  if [ "$HTTP" != "201" ] || [ -z "$SYS_ID" ]; then
    echo "  FAIL: Create system (HTTP $HTTP)"
    return
  fi
  echo "  ✅ System created: $SYS_ID"

  # 2. Read system back — verify roundtrip
  local READ=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE/game-systems/$SYS_ID")
  local READ_JSON=$(echo "$READ" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('rules_json',''))" 2>/dev/null)
  if echo "$READ_JSON" | python3 -c "import sys,json;json.loads(sys.stdin.read())" 2>/dev/null; then
    echo "  ✅ Roundtrip: rulesJson valid"
  else
    echo "  FAIL: rulesJson invalid after roundtrip"
    return
  fi

  # 3. Extract attributes from rules for entity creation
  local ATTRS=$(echo "$RULES" | python3 -c "
import sys,json
r=json.load(sys.stdin)
attrs={}
for a in r.get('attributes',[]):
    attrs[a['name']]=a.get('default',10)
print(json.dumps(attrs))
" 2>/dev/null)

  # 4. Create a world for the entity
  local WORLD_RES=$(curl -s -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"Testwelt-$NAME\",\"gameSystemId\":\"$SYS_ID\"}" \
    "$BASE/worlds")
  local WORLD_ID=$(echo "$WORLD_RES" | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
  if [ -z "$WORLD_ID" ]; then echo "  FAIL: Create world"; return; fi
  echo "  ✅ World created: $WORLD_ID"

  # 5. Create a character entity with attributes
  local ENTITY_RES=$(curl -s -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"entityType\":\"PC\",\"name\":\"Testheld-$NAME\",\"attributesJson\":$(echo "$ATTRS" | jq -c . | jq -R -s -c .)}" \
    "$BASE/worlds/$WORLD_ID/entities")
  local ENTITY_ID=$(echo "$ENTITY_RES" | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
  if [ -z "$ENTITY_ID" ]; then echo "  FAIL: Create entity"; return; fi
  echo "  ✅ Entity created: $ENTITY_ID"

  # 6. GET /entities/{id}/sheet
  local SHEET=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE/entities/$ENTITY_ID/sheet")
  local SHEET_PARSE=$(echo "$SHEET" | python3 -c "import sys,json;d=json.load(sys.stdin);print(len(d.get('attributes',[])))" 2>/dev/null)
  if [ "$SHEET_PARSE" -gt 0 ] 2>/dev/null; then
    echo "  ✅ Sheet loaded: $SHEET_PARSE attributes"
  else
    echo "  FAIL: Sheet not loaded"
    return
  fi

  # 7. POST /rolls/probe — basic probe
  local PROBE_RES=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"entityId\":\"$ENTITY_ID\",\"skillName\":\"\",\"target\":10,\"advantage\":false}" \
    "$BASE/rolls/probe")
  local PROBE_HTTP=$(echo "$PROBE_RES" | tail -1)
  if [ "$PROBE_HTTP" = "200" ]; then
    echo "  ✅ Probe rolled"
  else
    echo "  FAIL: Probe (HTTP $PROBE_HTTP)"
    return
  fi

  PASS=$((PASS+1))
  echo "  ✅ $NAME: ALL CHECKS PASSED"
}

login

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
test_system "DnD5e-Test" "$SCRIPT_DIR/docs/examples/dnd5e.json"
test_system "CoC7e-Test" "$SCRIPT_DIR/docs/examples/coc7e.json"
test_system "DSA5-Test" "$SCRIPT_DIR/docs/examples/dsa5.json"

echo ""
echo "=============================="
echo "Results: $PASS/$TOTAL passed"
echo "=============================="
