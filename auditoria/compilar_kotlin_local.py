"""Verificación complementaria con el compilador cacheado; no sustituye assembleDebug."""
from pathlib import Path
import subprocess, zipfile, re
root=Path(__file__).resolve().parents[1]
cache=Path('/tmp/idmji-audit-gradle/caches/modules-2/files-2.1')
out=Path('/tmp/idmji-kotlin-local'); out.mkdir(exist_ok=True)
def jars(group, artifact, version):
    return [p for p in (cache/group/artifact/version).glob('*/*.jar') if not p.name.endswith(('-sources.jar','-javadoc.jar'))]
compiler=[]
for group,art,ver in [('org.jetbrains.kotlin','kotlin-compiler-embeddable','2.3.20'),('org.jetbrains.kotlin','kotlin-stdlib','2.3.20'),('org.jetbrains.kotlin','kotlin-script-runtime','2.3.20'),('org.jetbrains.kotlin','kotlin-reflect','1.6.10'),('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.10.2'),('org.jetbrains','annotations','23.0.0')]:
    compiler+=jars(group,art,ver)
cp=[]
for group in sorted(cache.iterdir()):
    if not group.is_dir(): continue
    for art in sorted(group.iterdir()):
        if not art.is_dir(): continue
        versions=sorted([p for p in art.iterdir() if p.is_dir()],key=lambda p:tuple(int(x) for x in re.findall(r'\d+',p.name)),reverse=True)
        if not versions: continue
        version=versions[0]
        for p in version.glob('*/*'):
            if p.name.endswith(('-sources.jar','-javadoc.jar','-sources.aar')): continue
            if p.suffix=='.jar': cp.append(p)
            elif p.suffix=='.aar':
                dest=out/(group.name+'-'+art.name+'-'+version.name+'.jar')
                with zipfile.ZipFile(p) as z:
                    if 'classes.jar' in z.namelist(): dest.write_bytes(z.read('classes.jar'));cp.append(dest)
android=Path('/home/rocua/Android/Sdk/platforms/android-36/android.jar')
cp=[android]+cp+list((root/'app/build/intermediates').glob('**/R.jar'))
plugin=jars('org.jetbrains.kotlin','kotlin-compose-compiler-plugin-embeddable','2.3.20')
sources=list((root/'app/src/main/kotlin').rglob('*.kt'))
tests=list((root/'app/src/test/kotlin/org/idmji/biblia/ui/components').glob('*.kt'))
instrumentation=list((root/'app/src/androidTest/kotlin/org/idmji/biblia/data').glob('*.kt'))
cmd=['java','-Xmx2g','-cp',':'.join(map(str,compiler)),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','17','-classpath',':'.join(map(str,cp)),'-d',str(out/'classes')]
if plugin: cmd+=['-Xplugin='+str(plugin[0])]
cmd+=list(map(str,sources+tests+instrumentation))
result=subprocess.run(cmd,cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
(root/'auditoria/correcciones-kotlin.log').write_text(result.stdout)
print(result.stdout); print('KOTLIN_EXIT_CODE='+str(result.returncode))
if result.returncode: raise SystemExit(result.returncode)
run=['java','-cp',str(out/'classes')+':'+':'.join(map(str,cp)),'org.junit.runner.JUnitCore','org.idmji.biblia.ui.components.TranscriptionParserTest']
result=subprocess.run(run,cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
(root/'auditoria/correcciones-tests.log').write_text(result.stdout)
print(result.stdout); raise SystemExit(result.returncode)
