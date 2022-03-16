package se.llbit.chunky.renderer.postprocessing;

import se.llbit.math.Vector3;

public class NoneFilter extends SimplePixelPostProcessingFilter {
    @Override
    public void processPixel(Vector3 pixel) {

    }

    @Override
    public String getName() {
        return "None";
    }

    @Override
    public String getDescription() {
        return "None";
    }

    @Override
    public String getId() {
        return "NONE";
    }
}
