from ai.nets.samj.communication.model import SAM2Tiny
from java.lang import System
from java.util.function import Consumer
from ai.nets.samj.ij.utils import Constants
from ai.nets.samj.install import Sam2EnvManager

from io.bioimage.modelrunner.system import PlatformDetection

from java.io import File
import os


class PrintConsumer(Consumer):
    def accept(self, s):
        System.out.println(s)

if PlatformDetection.isUsingRosseta() or PlatformDetection.getArch() == PlatformDetection.ARCH_ARM64:
        relative_mamba = "appose_" + PlatformDetection.ARCH_ARM64
else:
        relative_mamba = "appose_" + PlatformDetection.getArch()

manager = Sam2EnvManager(os.path.join(Constants.FIJI_FOLDER, relative_mamba), "tiny")
model = SAM2Tiny(manager)
model.getInstallationManger().setConsumer(PrintConsumer());
model.getInstallationManger().installEverything();

assert model.isInstalled(), "Model not installed correctly"