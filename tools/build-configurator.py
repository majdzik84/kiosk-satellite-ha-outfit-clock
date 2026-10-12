import gzip,json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
catalog=dict(line.split('\t',1) for line in gzip.decompress((root/'resources/mdi/paths.tsv.gz').read_bytes()).decode().splitlines())
html=(root/'tools/icon-configurator.html').read_text(encoding='utf-8').replace('%MDI_JSON%',json.dumps(catalog,separators=(',',':')))
html=html.replace('%CUSTOM_JSON%',(root/'resources/custom-icons.json').read_text(encoding='utf-8'))
(root/'konfigurator-ikon.html').write_text(html,encoding='utf-8')
print('Created offline konfigurator-ikon.html')
