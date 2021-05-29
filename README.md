# ComputeShaderExample
An example of GLSL Compute Shaders for Android

Demonstration of the usage of GLSL Compute Shaders for Android.

The applications lets the user pick an image from their gallery, calculates the histogram on the BW version of the image and finally applies the histogram equalization algorithm on the the BW version.

For demonstration purposes, the left half of the BW image is left untouched, while the right one is equalized.

For an image of dimensions ~4000x2000 it takes about 30ms to compute an a Samsung Galaxy S8, while for a ~2000x1000 image 14ms.
