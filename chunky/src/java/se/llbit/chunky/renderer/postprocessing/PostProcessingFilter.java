package se.llbit.chunky.renderer.postprocessing;

import se.llbit.chunky.plugin.PluginApi;
import se.llbit.chunky.renderer.scene.imagebuffer.ImageBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.util.Registerable;
import se.llbit.util.TaskTracker;

/**
 * A post processing filter.
 * <p>
 * These filters are used to convert the HDR sample buffer into an SDR image that can be displayed.
 * Exposure is also applied by the filter.
 * <p>
 * Filters that support processing a single pixel at a time should implement {@link
 * PixelPostProcessingFilter} instead.
 */
@PluginApi
public interface PostProcessingFilter extends Registerable {
    /**
     * Post-process the entire frame
     * @param input The input render buffer. Exposure has not been applied
     * @param output The output image
     * @param exposure The exposure value
     * @param task Task
     */
    void processFrame(RenderBuffer input, ImageBuffer output, double exposure, TaskTracker.Task task);
}
