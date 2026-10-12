#!/usr/bin/env python3
"""Portable D8 build for this pure-Java plugin. Release builds use build.py and Android SDK."""
import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile
from plugin_assets import asset_files, MAX_PACKAGE_BYTES
from plugin_manifest import build_manifest
from importlib.machinery import SourceFileLoader
SourceFileLoader('embed_renderer',str(Path(__file__).with_name('embed-renderer.py'))).load_module().generate()
root = Path(__file__).resolve().parents[1]
java_home = Path(os.environ['JAVA_HOME'])
r8 = Path(os.environ['R8_JAR'])
manifest_bytes = build_manifest(root/'kiosk-satellite-plugin.json')
manifest = json.loads(manifest_bytes)
assets = asset_files(root)
def tool(name):
    return str(java_home/'bin'/(name+('.exe' if os.name=='nt' else '')))
def run(*args):
    subprocess.run(list(args),check=True)
def archive_bytes(files):
    stream = io.BytesIO()
    with zipfile.ZipFile(stream,'w') as archive:
        for name,data in files:
            info = zipfile.ZipInfo(name,(2020,1,1,0,0,0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            archive.writestr(info,data)
    return stream.getvalue()
with tempfile.TemporaryDirectory(prefix='ha-clock-build-') as temp:
    temp = Path(temp)
    sdk,classes,dex = [temp/p for p in ('sdk','classes','dex')]
    for directory in (sdk,classes,dex): directory.mkdir()
    run(tool('javac'),'--release','8','-encoding','UTF-8','-d',str(sdk),*map(str,(root/'sdk/src').rglob('*.java')))
    sdk_jar = temp/'sdk.jar'
    run(tool('jar'),'cf',str(sdk_jar),'-C',str(sdk),'.')
    run(tool('javac'),'--release','8','-encoding','UTF-8','-cp',str(sdk_jar),'-d',str(classes),*map(str,(root/'src').rglob('*.java')))
    run(tool('java'),'-cp',str(r8),'com.android.tools.r8.D8','--release','--min-api',str(manifest['minAndroidSdk']),
        '--lib',str(java_home),'--classpath',str(sdk_jar),'--output',str(dex),*map(str,classes.rglob('*.class')))
    if not list(dex.glob('*.dex')): raise SystemExit('D8 did not produce classes.dex')
    jar = archive_bytes([(p.name,p.read_bytes()) for p in sorted(dex.glob('*.dex'))])
    files = [('kiosk-satellite-plugin.json',manifest_bytes),('plugin.jar',jar),('LICENSE',(root/'LICENSE').read_bytes())]
    files += [(name,p.read_bytes()) for name,p in assets]
    if sum(len(data) for _,data in files)>MAX_PACKAGE_BYTES: raise SystemExit('Expanded package exceeds 4 MiB')
    package = archive_bytes(files)
    if len(package)>MAX_PACKAGE_BYTES: raise SystemExit('ZIP exceeds 4 MiB')
    out = root/'dist'
    out.mkdir(exist_ok=True)
    name = f"{manifest['id']}-{manifest['version']}.zip"
    (out/name).write_bytes(package)
    (out/(name+'.sha256')).write_text(hashlib.sha256(package).hexdigest()+'  '+name+'\n',encoding='utf-8')
    (out/'kiosk-satellite-plugin.json').write_bytes(manifest_bytes)
    print(f'Package: {out/name} ({len(package)} bytes)')
