# Executed by Fiji's Jython interpreter, not the runner's CPython.
import json
import os
from time import time
import traceback

from java.io import File
from java.lang import Class, System, Thread
from java.util import ArrayList
from java.util.function import Consumer
from jarray import array

from ai.nets.samj.communication.model import SAM2Tiny
from ai.nets.samj.ij import SAMJ_Annotator
from ai.nets.samj.install import Sam2EnvManager
from ij import IJ
from ij.io import FileSaver
from net.imglib2.img.display.imagej import ImageJFunctions


class PrintConsumer(Consumer):
    def accept(self, message):
        System.out.println(message)


def write_json(name, value):
    with open(os.path.join(output, name), "w") as stream:
        json.dump(value, stream, indent=2)


output = System.getenv("SAMJ_TEST_OUTPUT_DIR")
model_dir = System.getenv("SAMJ_TEST_MODEL_DIR")
model = None
exit_code = 1
try:
    assert output and model_dir, "Missing test directories"
    assert not os.path.exists(model_dir), "Model environment must be installed from scratch"
    with open(os.path.join(output, "build-manifest.json")) as stream:
        manifest = json.load(stream)
    class_sources = {}
    for name, artifact_id in (
            ("ai.nets.samj.communication.model.SAM2Tiny", "samj"),
            ("io.bioimage.modelrunner.apposed.appose.Mamba", "dl-modelrunner"),
            ("ai.nets.samj.ij.SAMJ_Annotator", "samj-IJ"),
            ("com.sun.jna.Native", "jna")):
        cls = Class.forName(name, True, Thread.currentThread().getContextClassLoader())
        source = File(cls.getProtectionDomain().getCodeSource().getLocation().toURI())
        expected, = [a for a in manifest["artifacts"] if a["artifactId"] == artifact_id]
        assert source.getName() == os.path.basename(expected["path"]), \
            "Unexpected jar for %s: %s" % (name, source)
        class_sources[name] = str(source)
        print("Loaded %s from %s" % (name, source))

    manager = Sam2EnvManager.create(model_dir, "tiny")
    model = SAM2Tiny(manager)
    manager.setConsumer(PrintConsumer())
    started = time()
    manager.installEverything()
    assert model.isInstalled(), "SAM2 Tiny installation did not complete"
    write_json("installation.json", {"installed": True, "model_dir": model_dir,
                                     "seconds": time() - started, "class_sources": class_sources})

    image = IJ.openImage(os.path.join(output, "input.gif"))
    assert image is not None, "Could not open test image"
    rai = ImageJFunctions.convertFloat(image)
    points = ArrayList()
    points.add(array([104, 113], "i"))
    started = time()
    mask = SAMJ_Annotator.samJReturnMask(model, rai, points, None)
    elapsed = time() - started
    assert mask is not None, "Inference returned no mask"
    assert mask.dimension(0) == image.getWidth() and mask.dimension(1) == image.getHeight(), \
        "Mask dimensions differ from input"
    foreground = 0
    cursor = mask.cursor()
    while cursor.hasNext():
        if cursor.next().getRealDouble() > 0:
            foreground += 1
    assert foreground > 0, "Inference produced an empty mask"
    assert foreground < image.getWidth() * image.getHeight(), "Mask covers the entire image"

    mask_image = ImageJFunctions.wrap(mask, "SAM2 Tiny mask")
    mask_path = os.path.join(output, "mask.tif")
    assert FileSaver(mask_image).saveAsTiff(mask_path), "Could not export mask TIFF"
    reopened = IJ.openImage(mask_path)
    assert reopened is not None, "Could not reopen exported mask"
    pixels = reopened.getProcessor()
    exported_foreground = sum(1 for i in range(reopened.getWidth() * reopened.getHeight())
                              if pixels.getf(i) > 0)
    assert exported_foreground == foreground, "Exported TIFF differs from inference mask"
    write_json("inference.json", {"model": "SAM2 Tiny", "point_prompt": [104, 113],
                                  "width": image.getWidth(), "height": image.getHeight(),
                                  "foreground_pixels": foreground, "seconds": elapsed,
                                  "mask": "mask.tif"})
    print("SAM2 Tiny passed: %d foreground pixels; %.3f seconds" % (foreground, elapsed))
    exit_code = 0
except:
    traceback.print_exc()
finally:
    if model is not None:
        try:
            model.closeProcess()
        except:
            traceback.print_exc()
            exit_code = 1
    System.exit(exit_code)
