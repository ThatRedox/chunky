package se.llbit.chunky.renderer.postprocessing;

import se.llbit.math.Vector3;

public abstract class IntensityScalingPostProcessingFilter extends SimplePixelPostProcessingFilter {
    /**
     * Process an intensity value.
     * @param intensity The intensity to process
     * @return The processed intensity
     */
    public abstract double process(double intensity);

    @Override
    public void processPixel(Vector3 pixel) {
        pixel.x = process(pixel.x);
        pixel.y = process(pixel.y);
        pixel.z = process(pixel.z);
    }
}
