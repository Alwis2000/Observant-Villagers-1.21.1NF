import os
import shutil
import subprocess

build_gradle = 'build.gradle'
java_dir = 'src/main/java'
java_disabled_dir = 'src/main/java_disabled'

# Read original build.gradle
with open(build_gradle, 'r', encoding='utf-8') as f:
    orig_gradle = f.read()

# Comment out unionlib
new_gradle = orig_gradle.replace(
    'implementation "curse.maven:unionlib-367806:7402936"',
    '// implementation "curse.maven:unionlib-367806:7402936"'
)
with open(build_gradle, 'w', encoding='utf-8') as f:
    f.write(new_gradle)

# Rename java dir
if os.path.exists(java_dir):
    os.rename(java_dir, java_disabled_dir)

print('Disabled mod classes and unionlib dependency. Running client...')
try:
    # Run client and wait up to 45 seconds
    res = subprocess.run(['powershell', '-Command', './gradlew runClient'], timeout=45, capture_output=True, text=True)
    print('Client execution finished.')
    print('STDOUT:', res.stdout[-2000:])
    print('STDERR:', res.stderr[-2000:])
except subprocess.TimeoutExpired as e:
    print('Client execution timed out (which is expected if the client successfully loaded and remained open).')
    if e.stdout:
        print('STDOUT:', e.stdout[-2000:])
    if e.stderr:
        print('STDERR:', e.stderr[-2000:])
finally:
    # Restore build.gradle
    with open(build_gradle, 'w', encoding='utf-8') as f:
        f.write(orig_gradle)
    # Restore java dir
    if os.path.exists(java_disabled_dir):
        os.rename(java_disabled_dir, java_dir)
    print('Restored mod classes and unionlib dependency.')
