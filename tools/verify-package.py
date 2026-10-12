#!/usr/bin/env python3
"""Inspect actual final ZIP, its DEX container, local assets and checksum."""
import hashlib
import io
import json
from pathlib import Path
import re
import zipfile
root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'kiosk-satellite-plugin.json').read_text(encoding='utf-8'))
path=root/'dist'/f"{manifest['id']}-{manifest['version']}.zip"
raw=path.read_bytes()
assert len(raw)<4*1024*1024
assert path.with_suffix('.zip.sha256').read_text().split()[0]==hashlib.sha256(raw).hexdigest()
with zipfile.ZipFile(path) as package:
    assert package.testzip() is None
    assert len(package.namelist())<=512
    assert sum(i.file_size for i in package.infolist())<4*1024*1024
    assert json.loads(package.read('kiosk-satellite-plugin.json'))==manifest
    assert len(manifest['settings'])<=20
    assert set(manifest['capabilities'])=={'screensaver','host.read'}
    with zipfile.ZipFile(io.BytesIO(package.read('plugin.jar'))) as dex:
        assert dex.testzip() is None
        assert 'classes.dex' in dex.namelist()
        assert all(re.fullmatch(r'classes(?:[2-9][0-9]*)?\.dex',name) for name in dex.namelist())
        assert dex.read('classes.dex').startswith(b'dex\n')
        assert b'Lpl/piotrek/kiosk/ClockPlugin;' in dex.read('classes.dex')
        assert b'Lpl/piotrek/kiosk/InlineRenderer;' in dex.read('classes.dex')
        assert sum(i.file_size for i in dex.infolist())<4*1024*1024
    for local in (root/'assets').rglob('*'):
        if local.is_file(): assert package.read(local.relative_to(root).as_posix())==local.read_bytes()
    html=package.read('assets/clock/index.html').decode('utf-8')
    js=package.read('assets/clock/clock.js').decode('utf-8')
    # SVG namespace identifies the format; it is not a network request.
    assert not re.search(r'https?://|HA_TOKEN|fetch\(', (html+js).replace('http://www.w3.org/2000/svg',''))
    assert 'fonts/Outfit.ttf' in html
    assert 'Europe/Warsaw' in js
print(f'PASS: ZIP integrity, DEX, checksum, manifest, asset identity, no remote requests; {len(raw)} bytes')
