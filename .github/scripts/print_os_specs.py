import java.lang
from java.io import File
import os


home = java.lang.System.getProperty("user.home")

folder_path = os.path.join(home, ".local", "share", "appose", "micromamba")

created = File(folder_path).mkdir()

print("created " + str(created))
print("exists: " + str(File("models").exists()))