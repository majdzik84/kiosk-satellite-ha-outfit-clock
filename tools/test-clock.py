#!/usr/bin/env python3
import os
from pathlib import Path
import subprocess
import sys
import tempfile
root=Path(__file__).resolve().parents[1]
subprocess.run([sys.executable,str(root/'tools/embed-renderer.py')],check=True)
java_home=os.environ.get('JAVA_HOME')
def tool(name):
    return str(Path(java_home)/'bin'/(name+('.exe' if os.name=='nt' else ''))) if java_home else name
with tempfile.TemporaryDirectory(prefix='ha-clock-test-') as directory:
    sources=[*sorted((root/'sdk/src').rglob('*.java')),*sorted((root/'src').rglob('*.java')),*sorted((root/'tests').rglob('*.java'))]
    subprocess.run([tool('javac'),'--release','8','-encoding','UTF-8','-d',directory,*map(str,sources)],check=True)
    subprocess.run([tool('java'),'-ea','-cp',directory,'pl.piotrek.kiosk.ClockPluginTest'],check=True)
for name in ('test_android_sdk.py','test_plugin_manifest.py','test_plugin_assets.py'):
    subprocess.run([sys.executable,str(root/'tools'/name)],check=True)
