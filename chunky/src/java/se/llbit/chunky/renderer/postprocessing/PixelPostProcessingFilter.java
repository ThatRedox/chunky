package se.llbit.chunky.renderer.postprocessing;

import se.llbit.chunky.plugin.PluginApi;
import se.llbit.chunky.renderer.scene.renderbuffer.Pixel;

/**
 * Post-processing filter that supports processing one pixel at a time.
 */
@PluginApi
public interface PixelPostProcessingFilter extends PostProcessingFilter {
    /**
     * Post-process a single pixel
     * @param pixel The pixel to post process and to output to.
     * @param exposure The exposure value
     */
    void processPixel(Pixel pixel, double exposure);
}
