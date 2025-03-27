from ai.nets.samj.communication.model import SAM2Tiny
from java.lang import System
from java.util.function import Consumer

class PrintConsumer(Consumer):
    def accept(self, s):
        System.out.println(s)

model = SAM2Tiny()
model.getInstallationManger().setConsumer(PrintConsumer());
model.getInstallationManger().installEverything();

assert not model.isInstalled(), "Model not installed correctly"