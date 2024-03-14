# SAMJ-IJ

The SAMJ-IJ is a powerful Fiji plugin for annotating microscopy images using various versions of the Segment Anything Model (SAM). This README provides detailed instructions on how to use the plugin for image annotation. In this first version of the plugin, the SAMJ-IJ Annotator is delivered to annotate images through the usage of prompts. The plugin is designed to be user-friendly and efficient, allowing for easy and accurate image annotation for further analysis.
Documentation preliminary version. Needs to be reviewed, improved and, of course, to add screenshots of the plugin usage.

## Contents
- [Installation](#installation)
- [Model Installation](#model-installation)
- [Annotating Images](#annotating-images)
- [Saving Annotations](#saving-annotations)
- [Usage Example](#usage-example)
- [Notes](#notes)

## Fiji and Plugin Installation

Before you can annotate images using SAMJ-IJ, you need to install the plugin in Fiji:

1. **Install Fiji**: If you haven't already, download and install [Fiji](https://fiji.sc/).
2. **Install SAMJ Plugin***: Open Fiji and navigate to `Help > Update...`. In the `Manage update sites` window, click on `Add update site` and look for the SAMJ Annotator Plugin. Click `OK` to add the update site. Restart Fiji to apply the changes.
3. **Open SAMJ-IJ Annotator**: Start Fiji and navigate to `Plugins > SAMJ > SAMJ Annotator` to open the plugin.

## Model Installation

To use the SAMJ-IJ plugin, you must install a SAM model. These are the models available for installation:
* **EfficientSAM:** A base model designed for segmentation tasks, optimized for efficiency and performance on standard computational resources. Ideal for quick, accurate segmentation in real-time applications.
* **EfficientViTSAM-l0:** A lightweight variant of the EfficientViTSAM model, offering a balance between segmentation accuracy and computational demand, suitable for use on normal computers.
* **EfficientViTSAM-l1:** An intermediate version, providing enhanced accuracy for complex segmentation tasks with manageable resource requirements.
* **EfficientViTSAM-l2:** A more advanced version, designed for high-accuracy segmentation in demanding scenarios, requiring higher computational resources.
* **EfficientViTSAM-xl0:** An extra-large model variant, pushing the boundaries of segmentation accuracy at the expense of increased computational demand.
* **EfficientViTSAM-xl1:** The most advanced and resource-intensive version, offering state-of-the-art segmentation performance for the most challenging tasks.

This are the steps to install a model:
1. Open the SAMJ Annotator plugin as described above.
2. Choose a SAM model from the list provided within the plugin.
3. Click on the `Install` button next to the selected model.
4. Wait for the installation process to complete. This may take some time depending on the model size, your computer and your internet connection.

This video demonstrates the live installation of EfficientViTSAM-l1 on a Mac M1.
![Installing EfficientViTSAM-l1](./images/installing-gif.gif)



## Annotating Images

Once you have installed a model, follow these steps to annotate your image:

1. **Open Image**: Open the microscopy image you want to annotate in Fiji.
2. **Select the Image**: In the SAMJ Annotator plugin, ensure your image is selected in the dropdown bar.
3. **Start Annotation**: Click on `Go` to begin the annotation process. This button will encode your image so you can start annotating. It can take a while.
4. **Choose Annotation Method**: Use one of the following tools to annotate your image:
   - `Rectangle (Rect)`: Draw rectangular Regions Of Interest (ROIs).
   - `Points`: Click to mark points on the image. Hold `Ctrl` to select multiple points for a single object.
   - `Brush`: Paint freeform ROIs.

   Optionally, untick the `Add to ROI Manager` checkbox if you don't want your annotations to be added to the Fiji ROI Manager automatically.
   *Note: the first annotation can take several seconds.*
5. **Annotate**: Annotate as many objects as needed. With each ROI drawn using one of the three tools, the installed SAM version will run and the object will be annotated.
6. **Manage Annotations**: All annotations will be sent to the ROI Manager (if the checkbox is ticked), where you can perform various operations as allowed by Fiji's ROI Manager functionality.

## Saving Annotations

### All ROIs or the largest one
To save your annotations, you can opt for either exporting every ROI using the "Return all ROIs" feature or selecting "Only return the largest ROI" to export solely the largest one. In the context of annotating heterogeneous images with various ROIs, as displayed below, you have the choice to either preserve the entirety of the ROIs, which would include every annotated point such as the nuclei and the entire embryo, or to conserve exclusively the predominant ROI, which, in this instance, would be the complete embryo.

![Embryo Annotation](./images/allROI-largestROI.png)

### Export to Labelling

With this button it is easy to export your annotations. It is exported as a semantic annotation meaning that each of the annotated regions will have a different value. We recommend change its Look Up Table (LUT) in Fiji for better visualization (`Image > Lookup Tables > Glasbey` or any other one).

<p float="center">
  <img src="/images/embryo.png" width="25%" />
  <img src="/images/embryo-nuclei-labeling.png" width="25%" /> 
</p>


## Usage Example

![Usage Example](images/usage-example.png)
> **Comment:** Add some screenshots of the plugin usage when available.

The image shows an example of how objects are annotated within the SAMJ-IJ plugin. Each annotated object is clearly marked and labeled, indicating the ease and efficiency of the plugin's use for image analysis.

## Notes

- This plugin is intended for use with microscopy images.
- The documentation here is for users only. Developer documentation, including contribution guidelines, will be available in a separate repository.

For further assistance or to report issues, please visit the [plugin's repository](https://github.com/segment-anything-models-java/SAMJ-IJ).

Thank you for using the SAMJ-IJ Fiji plugin!
