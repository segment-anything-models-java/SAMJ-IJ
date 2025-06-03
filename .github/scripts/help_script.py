from java.io import File


created = File("models").mkdir()

print("created " + created)
print("exists: " + File("models").exists())