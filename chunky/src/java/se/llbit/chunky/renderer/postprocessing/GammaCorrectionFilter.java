package se.llbit.chunky.renderer.postprocessing;

import org.apache.commons.math3.util.FastMath;
import se.llbit.chunky.renderer.scene.Scene;

public class GammaCorrectionFilter extends IntensityScalingPostProcessingFilter {
    @Override
    public double process(double intensity) {
        return FastMath.pow(intensity, 1 / Scene.DEFAULT_GAMMA);
    }


    @Override
    public String getName() {
        return "Gamma correction";
    }

    @Override
    public String getDescription() {
        return "Gamma correction";
    }

    @Override
    public String getId() {
        return "GAMMA";
    }
}
