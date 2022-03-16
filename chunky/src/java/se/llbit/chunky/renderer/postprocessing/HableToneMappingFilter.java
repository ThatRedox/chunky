package se.llbit.chunky.renderer.postprocessing;

/**
 * Implementation of Hable tone mapping
 * @link http://filmicworlds.com/blog/filmic-tonemapping-operators/
 */
public class HableToneMappingFilter extends IntensityScalingPostProcessingFilter {
    private static final float hA = 0.15f;
    private static final float hB = 0.50f;
    private static final float hC = 0.10f;
    private static final float hD = 0.20f;
    private static final float hE = 0.02f;
    private static final float hF = 0.30f;
    private static final float hW = 11.2f;
    private static final float whiteScale = 1.0f / (((hW * (hA * hW + hC * hB) + hD * hE) / (hW * (hA * hW + hB) + hD * hF)) - hE / hF);

    @Override
    public double process(double intensity) {
        // This adjusts the exposure by a factor of 16 so that the resulting exposure approximately matches the other
        // post-processing methods. Without this, the image would be very dark.
        intensity *= 16;
        intensity = ((intensity * (hA * intensity + hC * hB) + hD * hE) / (intensity * (hA * intensity + hB) + hD * hF)) - hE / hF;
        intensity *= whiteScale;
        return intensity;
    }

    @Override
    public String getName() {
        return "Hable tone mapping";
    }

    @Override
    public String getDescription() {
        return "Hable tone mapping";
    }

    @Override
    public String getId() {
        return "TONEMAP3";
    }
}
