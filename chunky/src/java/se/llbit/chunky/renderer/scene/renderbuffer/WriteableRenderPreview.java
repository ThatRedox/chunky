package se.llbit.chunky.renderer.scene.renderbuffer;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * This is a preview of a render. This is designed to be a low memory footprint preview of a (potentially larger) render.
 * This implements {@link RenderBuffer} but is not a real render buffer implementation.
 * This is so {@link se.llbit.chunky.renderer.postprocessing.PostProcessingFilter}s only need to implement
 * postprocessing for the {@code RenderBuffer} interface. This will not always reflect the real render exactly,
 * but is intended to be a close preview.
 */
public class WriteableRenderPreview extends RenderPreview {
    protected final RenderBuffer buffer;

    protected float[] backBuffer;
    protected AtomicIntegerArray backSamples;

    protected WriteableRenderPreview(RenderBuffer buffer) {
        this.buffer = buffer;
        this.width = buffer.getWidth();
        this.height = buffer.getHeight();
        this.preview = new float[this.width * this.height * 3];
        this.backBuffer = new float[this.width * this.height * 3];
        this.backSamples = new AtomicIntegerArray(this.width * this.height);
    }

    /**
     * Set the color of a (buffer space) pixel.
     * @param x Buffer x
     * @param y Buffer y
     * @param r Red color
     * @param g Green color
     * @param b Blue color
     */
    public void setBackColor(int x, int y, double r, double g, double b) {
        float[] backBuffer = this.backBuffer;
        AtomicIntegerArray backSamples = this.backSamples;
        int width = this.width;
        int height = this.height;

        // Check for race conditions
        if (backBuffer.length != backSamples.length()*3 || backBuffer.length != width * height * 3) {
            setBackColor(x, y, r, g, b);
        }

        int index = getBinIndex(x, y, buffer.getWidth(), buffer.getHeight(), width, height);

        while (true) {
            // Try to lock the pixel
            int samples = backSamples.get(index);
            if (samples != -1 && backSamples.compareAndSet(index, samples, -1)) {
                backBuffer[index*3 + 0] += r;
                backBuffer[index*3 + 1] += g;
                backBuffer[index*3 + 2] += b;
                backSamples.set(index, samples+1);
                return;
            }
        }
    }

    public synchronized void commitBackColor() {
        IntArrayList problemPixels = new IntArrayList();

        for (int index = 0; index < backSamples.length(); index++) {
            int samples = backSamples.get(index);
            if (samples == 0) continue;

            // Try to lock the pixel
            if (samples != -1 && backSamples.compareAndSet(index, samples, -1)) {
                for (int i = 0; i < 3; i++) {
                    int offset = index*3 + i;
                    preview[offset] = backBuffer[offset] / samples;
                }
                backSamples.set(index, 0);
                break;
            } else {
                problemPixels.add(index);
            }
        }

        if (problemPixels.size() > 0) {
            Thread.yield();
            for (int index : problemPixels) {
                while (true) {
                    int samples = backSamples.get(index);
                    if (samples == 0) break;

                    // Try to lock the pixel
                    if (samples != -1 && backSamples.compareAndSet(index, samples, -1)) {
                        for (int i = 0; i < 3; i++) {
                            int offset = index*3 + i;
                            preview[offset] = backBuffer[offset] / samples;
                        }
                        backSamples.set(index, 0);
                        break;
                    } else {
                        Thread.yield();
                    }
                }
            }
        }
    }

    public void reset() {
        Arrays.fill(preview, 0);
        for (int i = 0; i < backSamples.length(); i++) backSamples.set(i, 0);
    }

    /**
     * Set the preferred resolution.
     * @param perfWidth  Preferred width.
     * @param perfHeight Preferred height.
     */
    @Override
    public synchronized void setPreviewResolution(int perfWidth, int perfHeight) {
        perfWidth = Math.max(MIN_WIDTH, perfWidth);
        perfHeight = Math.max(MIN_HEIGHT, perfHeight);

        double widthFactor = (double) perfWidth / buffer.getWidth();    // Scale factor for X
        double heightFactor = (double) perfHeight / buffer.getHeight(); // Scale factor for Y
        double factor = Math.min(widthFactor, heightFactor);            // Get the smaller factor

        // New dimensions
        int newWidth = (int) (buffer.getWidth() * factor);
        int newHeight = (int) (buffer.getHeight() * factor);
        float[] newPreview = new float[newWidth * newHeight * 3];
        int[] binCount = new int[newWidth * newHeight];

        // Box filter
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int index = getBinIndex(x, y, width, height, newWidth, newHeight);
                System.arraycopy(preview, (x + y * width) * 3, newPreview, index * 3, 3);
                binCount[index] += 1;
            }
        }

        // Normalize the samples
        for (int i = 0; i < binCount.length; i++) {
            for (int j = 0; j < 3; j++) {
                newPreview[i*3 + j] /= binCount[i];
            }
        }

        this.width = newWidth;
        this.height = newHeight;
        this.preview = newPreview;
        this.backBuffer = new float[newPreview.length];
        this.backSamples = new AtomicIntegerArray(binCount.length);
    }

    protected static int getBinIndex(int x, int y, int bufferWidth, int bufferHeight, int previewWidth, int previewHeight) {
        int px = (x * previewWidth) / bufferWidth;
        int py = (y * previewHeight) / bufferHeight;
        return px + py * previewWidth;
    }

    @Deprecated
    @Override
    public double[] asDoubleArray() {
        double[] out = new double[preview.length];
        Arrays.setAll(out, i -> preview[i]);
        return out;
    }
}
