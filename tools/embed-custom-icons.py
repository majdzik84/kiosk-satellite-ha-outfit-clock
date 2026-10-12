import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
icons=json.loads((root/'resources/custom-icons.json').read_text(encoding='utf-8'))
source='// Generated from resources/custom-icons.json.\npackage pl.piotrek.kiosk;\nfinal class CustomIcons {\n'
source+=' static boolean has(String key) { return '+ ' || '.join(json.dumps(k)+'.equals(key)' for k in icons)+'; }\n'
source+=' static String svg(String key, boolean open) {\n'
for key,icon in icons.items():
    svg='<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-linecap="butt" stroke-linejoin="miter"><path stroke-width="1.15" d="%PATH%"/>'
    if icon.get('post'): svg+='<path stroke-width="2.76" d="'+icon['post']+'"/>'
    svg+='</svg>'
    source+='  if ('+json.dumps(key)+'.equals(key)) return '+json.dumps(svg)+'.replace("%PATH%",open ? '+json.dumps(icon['open'])+' : '+json.dumps(icon['closed'])+');\n'
source+='  return "";\n }\n}\n'
(root/'src/pl/piotrek/kiosk/CustomIcons.java').write_text(source,encoding='utf-8')
