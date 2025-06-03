import java.lang
from java.io import File
import os
from java.util import UUID


filename = "micromamba-" + str(UUID.randomUUID()) + ".tar.bz2"

home = java.lang.System.getProperty("user.home")

folder_path = os.path.join(home, ".local", "share", "appose", "micromamba3", filename)

ff = File(folder_path)

if not ff.getParentFile().exists():
	ff.getParentFile().mkdirs()

ff.createNewFile()

#created = File(folder_path).mkdir()

#print("created " + str(created))
print("exists: " + str(File(folder_path).isFile()))