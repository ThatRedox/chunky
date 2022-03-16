package se.llbit.chunky.renderer.postprocessing;

import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.renderer.scene.renderbuffer.Pixel;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.iteration.RenderBufferTiledIterable;
import se.llbit.chunky.resources.BitmapImage;
import se.llbit.math.ColorUtil;
import se.llbit.math.Vector3;
import se.llbit.util.TaskTracker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

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
    public void processFrame(RenderBuffer input, BitmapImage output, double exposure, TaskTracker.Task task) {
        task.update(input.getHeight(), 0);
        AtomicInteger virtualRowCount = new AtomicInteger(0);
        AtomicLong done = new AtomicLong(0);

        new RenderBufferTiledIterable(input).tiles().forEach(tile -> {
            Vector3 color = new Vector3();
            for (int x = tile.getBufferX(0); x < tile.getBufferX(tile.getTileWidth()); x++) {
                for (int y = tile.getBufferY(0); y < tile.getBufferY(tile.getTileHeight()); y++) {
                    int s = tile.getColor(x, y, color);
                    Pixel pixel = new Pixel(color, s, x, y);
                    processPixel(pixel, exposure);
                    output.setPixel(pixel.bufferX, pixel.bufferY, ColorUtil.getRGBClamped(pixel.color));

                    if (done.incrementAndGet() % input.getWidth() == 0) {
                        task.update(virtualRowCount.incrementAndGet());
                    }
                }
            }
        });

//        Chunky.getCommonThreads().submit(() -> StreamSupport.stream(new RenderBufferTiledIterable(input).pixels().spliterator(), true).forEach(pixel -> {
//            processPixel(pixel, exposure);
//            output.setPixel(pixel.bufferX, pixel.bufferY, ColorUtil.getRGBClamped(pixel.color));
//
//            if (done.incrementAndGet() % input.getWidth() == 0) {
//                task.update(virtualRowCount.incrementAndGet());
//            }
//        })).join();
    }

    @Override
    public void processPixel(Pixel pixel, double exposure) {
        pixel.color.scale(exposure);
        processPixel(pixel.color);
    }
}
