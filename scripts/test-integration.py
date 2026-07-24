#!/usr/bin/env python3
"""Integrationstest: lädt Beispiel-JSONs, legt Systeme an, testet Sheet + Probe."""
import json, sys, os, urllib.request, urllib.error

BASE = "http://192.168.31.151:8080/api/v1"
total, passed = 0, 0

def api(method, path, data=None, token=None):
    url = BASE + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        err = json.loads(e.read())
        return {"error": err.get("error", {}).get("code", str(e.code))}

def login():
    r = api("POST", "/auth/login", {"email": "cenksaatci@googlemail.com", "password": "test123"})
    return r.get("accessToken")

def test_system(name, filepath):
    global total, passed
    total += 1
    print(f"\n=== Testing: {name} ===")
    
    with open(filepath) as f:
        rules = json.load(f)
    rules.pop("_comment", None)
    rules.pop("_gaps", None)
    rules_str = json.dumps(rules)
    
    # Create system
    r = api("POST", "/game-systems", {"name": name, "version": 1, "rulesJson": rules_str, "schemaJson": "{}"}, token)
    sys_id = r.get("id")
    if not sys_id:
        print(f"  FAIL: Create system ({r})")
        return
    print(f"  ✅ System created: {sys_id}")
    
    # Read back
    r = api("GET", f"/game-systems/{sys_id}", token=token)
    read_json = r.get("rules_json", "")
    try:
        json.loads(read_json)
        print("  ✅ Roundtrip: rulesJson valid")
    except:
        print("  FAIL: rulesJson invalid after roundtrip")
        return
    
    # Extract attribute defaults
    attrs = {a["name"]: a.get("default", 10) for a in rules.get("attributes", [])}
    
    # Create world
    r = api("POST", "/worlds", {"name": f"Testwelt-{name}", "gameSystemId": sys_id}, token)
    world_id = r.get("id")
    if not world_id:
        print(f"  FAIL: Create world ({r})")
        return
    print(f"  ✅ World created: {world_id}")
    
    # Create entity
    r = api("POST", f"/worlds/{world_id}/entities", {"entityType": "PC", "name": f"Held-{name}", "attributesJson": json.dumps(attrs)}, token)
    ent_id = r.get("id")
    if not ent_id:
        print(f"  FAIL: Create entity ({r})")
        return
    print(f"  ✅ Entity created: {ent_id}")
    
    # GET sheet
    r = api("GET", f"/entities/{ent_id}/sheet", token=token)
    attr_count = len(r.get("attributes", []))
    if attr_count > 0:
        print(f"  ✅ Sheet loaded: {attr_count} attributes")
    else:
        print(f"  FAIL: Sheet not loaded ({r})")
        return
    
    # POST probe
    r = api("POST", "/rolls/probe", {"entityId": ent_id, "skillName": "", "target": 10, "advantage": False}, token)
    if "total" in r:
        print(f"  ✅ Probe rolled: total={r['total']}")
    else:
        print(f"  FAIL: Probe ({r})")
        return
    
    passed += 1
    print(f"  ✅ {name}: ALL CHECKS PASSED")

token = login()
if not token:
    print("LOGIN FAILED")
    sys.exit(1)

script_dir = os.path.dirname(os.path.abspath(__file__))
examples_dir = os.path.join(script_dir, "..", "docs", "examples")
test_system("DnD5e", os.path.join(examples_dir, "dnd5e.json"))
test_system("CoC7e", os.path.join(examples_dir, "coc7e.json"))
test_system("DSA5",  os.path.join(examples_dir, "dsa5.json"))

print(f"\n{'='*30}\nResults: {passed}/{total} passed\n{'='*30}")
sys.exit(0 if passed == total else 1)
