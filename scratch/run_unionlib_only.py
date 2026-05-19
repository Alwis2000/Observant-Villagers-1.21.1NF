import os
import shutil
import subprocess

build_gradle = 'build.gradle'
main_dir = 'src/main'
disabled_dir = 'src/main_disabled'

# Rename main dir to disable obville
if os.path.exists(main_dir):
    os.rename(main_dir, disabled_dir)

print('Disabled obville (src/main). Keeping unionlib enabled. Running client...')
try:
    res = subprocess.run(['cmd', '/c', 'gradlew.bat runClient > run/class_load_log_unionlib_only.txt 2>&1'], timeout=45, capture_output=True, text=True)
    print('Client execution finished.')
except subprocess.TimeoutExpired as e:
    print('Client execution timed out (expected if it did not crash immediately).')
finally:
    # Restore main dir
    if os.path.exists(disabled_dir):
        os.rename(disabled_dir, main_dir)
    print('Restored obville (src/main).')
