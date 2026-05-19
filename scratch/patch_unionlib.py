import zipfile
import json
import os
import shutil

jar_path = r"C:\Users\alwis\.gradle\caches\modules-2\files-2.1\curse.maven\unionlib-367806\7402936\4ccbea8de2cad02ba9f8faf9eea3af8eec8c6d92\unionlib-367806-7402936.jar"
backup_path = jar_path + ".backup"

# 1. Back up the jar if not already backed up
if not os.path.exists(backup_path):
    print("Creating backup of unionlib jar...")
    shutil.copyfile(jar_path, backup_path)

temp_dir = r"P:\Github Repos\Observant-Villagers-1.21.1NF\build\tmp\patch_unionlib"
if os.path.exists(temp_dir):
    shutil.rmtree(temp_dir)
os.makedirs(temp_dir, exist_ok=True)

# 2. Extract backup jar to temp dir
print("Extracting unionlib jar...")
with zipfile.ZipFile(backup_path, "r") as jar:
    jar.extractall(temp_dir)

# 3. Clear all mixin lists in all mixin JSONs
for filename in ["unionlib.mixins.json", "unionlib.insert.mixins.json", "unionlib_potionfluid.mixins.json"]:
    path = os.path.join(temp_dir, filename)
    if os.path.exists(path):
        print(f"Clearing mixins in {filename}")
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        
        if "mixins" in data:
            data["mixins"] = []
        if "client" in data:
            data["client"] = []
        if "server" in data:
            data["server"] = []
        
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)

# 4. Pack back into the original jar path
print("Re-packing unionlib jar...")
if os.path.exists(jar_path):
    os.remove(jar_path)

with zipfile.ZipFile(jar_path, "w", zipfile.ZIP_DEFLATED) as jar:
    for root, dirs, files in os.walk(temp_dir):
        for file in files:
            full_path = os.path.join(root, file)
            rel_path = os.path.relpath(full_path, temp_dir)
            jar.write(full_path, rel_path)

print("Patch complete!")
