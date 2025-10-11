from ai.nets.samj.ij.utils import Constants
from java.io import File
from io.bioimage.modelrunner.system import PlatformDetection

from ai.nets.samj.install import SamEnvManagerAbstract
from ai.nets.samj.communication.model import SAM2Tiny


if not PlatformDetection.isMacOS() or not PlatformDetection.isUsingRosseta():
    platform = PlatformDetection.getArch()
else:
    platform = PlatformDetection.ARCH_ARM64



default_dir = Constants.FIJI_FOLDER + File.separator + "appose_" + platform

print(default_dir)

SamEnvManagerAbstract.DEFAULT_DIR = default_dir

SAM2Tiny().getInstallationManger().installEverything()

print("everything installed")