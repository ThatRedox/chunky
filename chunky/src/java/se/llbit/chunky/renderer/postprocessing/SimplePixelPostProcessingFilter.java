package se.llbit.chunky.renderer.postprocessing;

import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.renderer.scene.imagebuffer.ImageBuffer;
import se.llbit.chunky.renderer.scene.imagebuffer.WriteableImageTile;
import se.llbit.chunky.renderer.scene.renderbuffer.Pixel;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderTile;
import se.llbit.chunky.renderer.scene.renderbuffer.iteration.RenderBufferIterable;
import se.llbit.math.Vector3;
import se.llbit.util.TaskTracker;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for post processing filter that process each pixel independently
 */
public abstract class SimplePixelPostProcessingFilter implements PixelPostProcessingFilter {
    /**
     * Post-process a single pixel
     * @param pixel the rgb component of the pixel. Input/output parameter.
     *              The exposure has already been applied
     */
    public abstract void processPixel(Vector3 pixel);

    @Override
    public void processFrame(RenderBuffer input, ImageBuffer output, double exposure, TaskTracker.Task task) {
        RenderBufferIterable<RenderTile> tiles = RenderBufferIterable.read(input);
        task.update((int) tiles.numTiles(), 0);  // int cast should fail at about 35 trillion pixels, so it's fine for now
        AtomicInteger tilesProgress = new AtomicInteger(0);

        Chunky.getCommonThreads().submit(() -> tiles.stream().forEach(tile -> {
            try(WriteableImageTile outTile = output.getWriteTile(tile.getBufferX(0), tile.getBufferY(0), tile.getTileWidth(), tile.getTileHeight())) {
                RenderBufferIterable.pixelStream(tile).parallel().forEach(pixel -> {
                    this.processPixel(pixel, exposure);
                    outTile.setColor(pixel.bufferX, pixel.bufferY, pixel.color.x, pixel.color.y, pixel.color.z, 1.0);
                });
            }
            task.update(tilesProgress.incrementAndGet());
        })).join();
    }

    @Override
    public void processPixel(Pixel pixel, double exposure) {
        pixel.color.scale(exposure);
        processPixel(pixel.color);
    }
}
