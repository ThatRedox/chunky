package se.llbit.chunky.renderer.postprocessing;

import org.apache.commons.math3.util.FastMath;

public class PreviewFilter extends IntensityScalingPostProcessingFilter {
    public static final PreviewFilter INSTANCE = new PreviewFilter();

    @Override
    public double process(double intensity) {
        return FastMath.sqrt(intensity);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }
}
