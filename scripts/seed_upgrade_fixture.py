"""Seed synthetic v2 data on an emulator before installing the v3 test APK."""
import json
import subprocess
from datetime import date
from xml.sax.saxutils import escape
plans=[{"id":101,"title":"Önceki sürümden plan","date":date.today().isoformat(),"type":"Saatli","time":"10:00","startTime":"10:00","endTime":"11:00","duration":"1s","note":"Korunan not","repeat":"none","category":"reading","details":"Kitabım","status":"planned","done":False}]
xml='<?xml version="1.0" encoding="utf-8"?><map><string name="plans">'+escape(json.dumps(plans,ensure_ascii=False))+'</string></map>'
subprocess.run(['adb','shell','run-as','com.rota.app','mkdir','-p','shared_prefs'],check=True)
subprocess.run(['adb','shell',"run-as com.rota.app sh -c 'cat > shared_prefs/rota_store.xml'"],input=xml.encode(),check=True)
print('Seeded one synthetic legacy plan for the in-place upgrade test.')
