from ai.nets.samj.ij.utils import Constants
from java.io import File
from io.bioimage.modelrunner.system import PlatformDetection


if not PlatformDetection.isMacOS() or not PlatformDetection.isUsingRosseta():
    platform = PlatformDetection.getArch()
else:
    platform = PlatformDetection.ARCH_ARM64



default_dir = Constants.FIJI_FOLDER + File.separator + "appose_" + platform

print(default_dir)