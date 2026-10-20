#!/usr/bin/env python3
"""Coverage inventory ONLY: existing Maestro scripts are not executed here."""
from pathlib import Path
import re,json
root=Path(__file__).resolve().parents[2]
acts=sorted(p.stem for p in (root/"app/src/main/java/com/nikahbridge").glob("*Activity.java"))
files=sorted((root/".maestro").glob("*.yaml"))
data=[]
for file in files:
 t=file.read_text(encoding="utf-8")
 labels=re.findall(r'^- tapOn:\s*["\x27]?([^\n"\x27]+)',t,re.M)
 data.append({"file":str(file.relative_to(root)),"tap_commands":len(labels),"labels":labels,"signed_in_required":"signed-in" in t or "Secure Azure session verified" in t})
payload={"screen_activity_count":len(acts),"activity_classes":acts,"maestro_existing_flows":data,"runtime_50_screen_verified":False,"caveat":"These flows require an actual device and authorized signed-in Azure test session; script inventory is NOT UI PASS."}
report=root/"audit-maestro-50-screen-readiness.json"
report.write_text(json.dumps(payload,indent=2,ensure_ascii=False)+"\n",encoding="utf8")
assert len(acts)==50,"Unexpected activity count"
assert files,"No Maestro scripts"
print("50 ACTIVITIES AND MAESTRO SCRIPT INVENTORY: PASS")
for x in data: print(x["file"],"tap commands:",x["tap_commands"],"signed in:",x["signed_in_required"])
print("RUNTIME ROBOT: PENDING — no emulator used, no auth bypass.")
