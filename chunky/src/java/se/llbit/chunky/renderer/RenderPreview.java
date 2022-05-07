package se.llbit.chunky.renderer;

import se.llbit.chunky.renderer.scene.imagebuffer.ImageBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderTile;
import se.llbit.chunky.renderer.scene.renderbuffer.WriteableRenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.WriteableRenderTile;
import se.llbit.chunky.renderer.scene.renderbuffer.iteration.RenderBufferIterable;
import se.llbit.math.QuickMath;
import se.llbit.math.Vector3;
import se.llbit.util.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * A render preview object.
 * @param <T> backing ImageBuffer type
 */
public class RenderPreview<T extends ImageBuffer> {
    protected WriteableRenderBuffer buffer = null;
    protected int maxWidth = 128;
    protected int maxHeight = 128;

    protected final ImageBuffer.Factory<T> imageBufferFactory;
    protected final ReentrantLock imageLock = new ReentrantLock();
    protected T imageBuffer;

    protected Preview preview;

    protected final Consumer<RenderTile> tileConsumerInstance = this::tileConsumer;

    /**
     * @param imageBufferFactory Factory for the backing ImageBuffer type.
     */
    public RenderPreview(ImageBuffer.Factory<T> imageBufferFactory) {
        this.imageBufferFactory = imageBufferFactory;
    }

    /**
     * Register this preview with a render buffer.
     */
    public void register(WriteableRenderBuffer buffer) {
        buffer.getTileCallbacks().add(tileConsumerInstance);
        this.buffer = buffer;
        setResolution();
    }

    /**
     * Build this preview with the stored render buffer.
     */
    public void build() {
        RenderBufferIterable.write(this.buffer).stream().forEach(WriteableRenderTile::close);
    }

    /**
     * Set the maximum desired resolution.
     */
    public void setMaxResolution(int maxWidth, int maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        setResolution();
    }

    /**
     * Run something with the image buffer protected.
     * Note: inside this consumer, the preview render buffer is also protected from structural modifications.
     */
    public void withImageProtected(Consumer<T> consumer) {
        imageLock.lock();
        try {
            consumer.accept(imageBuffer);
        } finally {
            imageLock.unlock();
        }
    }

    /**
     * Get the preview RenderBuffer.
     */
    public Preview getPreview() {
        return preview;
    }

    protected void setResolution() {
        if (buffer == null) return;

        int perfWidth = Math.min(buffer.getWidth(), maxWidth);
        int perfHeight = Math.min(buffer.getHeight(), maxHeight);

        if (perfWidth == 0 || perfHeight == 0) {
            perfWidth = buffer.getWidth();
            perfHeight = buffer.getHeight();
        }

        double widthFactor = (double) perfWidth / buffer.getWidth();    // Scale factor for X
        double heightFactor = (double) perfHeight / buffer.getHeight(); // Scale factor for Y
        double factor = Math.min(widthFactor, heightFactor);            // Smaller scale factor

        // New dimensions
        int newWidth = (int) (buffer.getWidth() * factor);
        int newHeight = (int) (buffer.getHeight() * factor);

        if (imageBuffer != null && preview != null &&
            imageBuffer.getWidth() == newWidth && imageBuffer.getHeight() == newHeight)
            return;

        imageLock.lock();
        try {
            imageBuffer = imageBufferFactory.create(newWidth, newHeight, false);
            preview = new Preview(newWidth, newHeight);
        } finally {
            imageLock.unlock();
        }
    }

    protected final void tileConsumer(RenderTile tile) {
        if (preview != null) {
            Vector3 color = new Vector3();
            for (int i = 0; i < tile.getTileWidth(); i++) {
                for (int j = 0; j < tile.getTileHeight(); j++) {
                    tile.getColor(tile.getBufferX(i), tile.getBufferY(j), color);
                    int x = QuickMath.clamp(tile.getBufferX(i) * preview.getWidth() / tile.getBufferWidth(), 0, preview.getWidth());
                    int y = QuickMath.clamp(tile.getBufferY(j) * preview.getHeight() / tile.getBufferHeight(), 0, preview.getHeight());
                    preview.setPixel(x, y, color.x, color.y, color.z);
                }
            }
        }
    }

    /**
     * The preview render buffer. This is not a "real" render buffer. It does not keep track of samples, call tile or
     * pixel callbacks, cannot be reset, and does not have a name, id, or description.
     * <p>
     * Note: this holds sample data with 30 bits per pixel. The top 10 bits are red, next 10 are green, and low 10 bits
     * are blue. Pixels are clamped to be in the range [0, 1] and quantized to 1024 possible values. Pixels are also
     * updated atomically.
     */
    public static class Preview implements WriteableRenderBuffer {
        private final int width;
        private final int height;

        protected int[] data;

        protected Preview(int width, int height) {
            this.width = width;
            this.height = height;
            this.data = new int[width * height];
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public Future<? extends WriteableRenderTile> getTile(int x, int y, int width, int height) {
            return CompletableFuture.completedFuture(new Tile(x, y, width, height));
        }

        @Override
        public void reset() {

        }

        /**
         * Note: This does not do anything.
         */
        @Override
        public Set<Consumer<RenderTile>> getTileCallbacks() {
            return new CopyOnWriteArraySet<>();
        }

        /**
         * Note: This does not do anything.
         */
        @Override
        public Set<RawPixelConsumer> getPixelCallbacks() {
            return new CopyOnWriteArraySet<>();
        }

        protected void setPixel(int x, int y, double r, double g, double b) {
            int index = x + y * getWidth();
            int red = (int) QuickMath.clamp(r*1023, 0, 1023);
            int green = (int) QuickMath.clamp(g*1023, 0, 1023);
            int blue = (int) QuickMath.clamp(b*1023, 0, 1023);
            int color = (red << 20) | (green << 10) | (blue);
            data[index] = color;
        }

        public class Tile implements WriteableRenderTile {
            protected final int width;
            protected final int height;
            protected final int x;
            protected final int y;

            protected Tile(int x, int y, int width, int height) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }

            @Override
            public int getColor(int x, int y, @Nullable Vector3 color) {
                if (color != null) {
                    int index = x + y * getWidth();
                    int d = data[index];
                    int r = d >> 20;
                    int g = (d >> 10) & 0b11_1111_1111;
                    int b = d & 0b11_1111_1111;
                    color.x = r / 1023.0;
                    color.y = g / 1023.0;
                    color.z = b / 1023.0;
                }
                return 1;
            }

            @Override
            public int getBufferX(int x) {
                return x + this.x;
            }

            @Override
            public int getBufferY(int y) {
                return y + this.y;
            }

            @Override
            public int getBufferWidth() {
                return Preview.this.width;
            }

            @Override
            public int getBufferHeight() {
                return Preview.this.height;
            }

            @Override
            public int getTileWidth() {
                return width;
            }

            @Override
            public int getTileHeight() {
                return height;
            }

            /**
             * Note: This sets the color at (x, y) to (r, g, b). The number of samples is discarded.
             */
            @Override
            public void mergeColor(int x, int y, double r, double g, double b, int s) {
                setPixel(x, y, r, g, b, s);
            }

            /**
             * Note: The number of samples is discarded.
             */
            @Override
            public void setPixel(int x, int y, double r, double g, double b, int s) {
                Preview.this.setPixel(x, y, r, g, b);
            }

            @Override
            public void close() {

            }
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
}
