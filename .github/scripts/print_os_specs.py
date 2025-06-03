import java.lang
from java.io import File
import os
from java.util import UUID


filename = "micromamba-" + str(UUID.randomUUID()) + ".tar.bz2"

home = java.lang.System.getProperty("user.home")

folder_path = os.path.join(home, ".local", "share", "appose", "micromamba2", filename)

File(folder_path).createNewFile()

#created = File(folder_path).mkdir()
print(folder_path)
#print("created " + str(created))
print("exists: " + str(File(folder_path).isFile()))