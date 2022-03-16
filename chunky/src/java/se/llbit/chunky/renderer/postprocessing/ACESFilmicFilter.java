package se.llbit.chunky.renderer.postprocessing;

import org.apache.commons.math3.util.FastMath;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.math.QuickMath;
import se.llbit.math.Vector3;

/**
 * Implementation of ACES filmic tone mapping
 * @link https://knarkowicz.wordpress.com/2016/01/06/aces-filmic-tone-mapping-curve/
 */
public class ACESFilmicFilter extends IntensityScalingPostProcessingFilter {
    private static final float aces_a = 2.51f;
    private static final float aces_b = 0.03f;
    private static final float aces_c = 2.43f;
    private static final float aces_d = 0.59f;
    private static final float aces_e = 0.14f;

    @Override
    public double process(double intensity) {
        intensity = QuickMath.max(QuickMath.min((intensity * (aces_a * intensity + aces_b)) / (intensity * (aces_c * intensity + aces_d) + aces_e), 1), 0);
        return FastMath.pow(intensity, 1 / Scene.DEFAULT_GAMMA);
    }

    @Override
    public String getName() {
        return "ACES filmic tone mapping";
    }

    @Override
    public String getDescription() {
        return "Implementation of ACES filmic tone mapping";
    }

    @Override
    public String getId() {
        return "TONEMAP2";
    }
}
