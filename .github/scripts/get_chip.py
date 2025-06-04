from io.bioimage.modelrunner.system import PlatformDetection


chip = PlatformDetection.getArch()
rosseta = PlatformDetection.isUsingRosseta()

print("Chip: " + chip)
print("Rosseta: " + str(rosseta))