from java.io import File


created = File("models").mkdir()

print("created " + str(created))
print("exists: " + str(File("models").exists()))