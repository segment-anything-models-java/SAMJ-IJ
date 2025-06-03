from ai.nets.samj.communication.model import SAM2Tiny
from ai.nets.samj.ij import SAMJ_Annotator

from java.util import ArrayList
from time import time

from ij import IJ

from net.imglib2.img.display.imagej import ImageJFunctions


blobs = IJ.openImage("https://imagej.net/images/blobs.gif")
wrapImg = ImageJFunctions.convertFloat(blobs)

model = SAM2Tiny()

point_prompt = ArrayList()
point_prompt.add((100, 100))

start_time = time()

mask = SAMJ_Annotator.samJReturnMask(model, wrapImg, point_prompt, None)

end_time = time()

mask_sum = 0
cursor = mask.cursor()

while cursor.hasNext():
    mask_sum += cursor.get()
    cursor.next()

print("Total non-zero pixels: " + str(mask_sum))
print("Total time: " + str(end_time - start_time))
