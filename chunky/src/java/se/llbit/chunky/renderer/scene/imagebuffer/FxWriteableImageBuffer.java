package se.llbit.chunky.renderer.scene.imagebuffer;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import se.llbit.math.ColorUtil;
import se.llbit.math.Vector3;
import se.llbit.util.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Image buffer backed by a JavaFX WriteableImage.
 */
public class FxWriteableImageBuffer implements ImageBuffer {
    protected final WritableImage image;
    protected final PixelWriter writer;
    protected final PixelReader reader;
    protected final boolean alpha;

    public FxWriteableImageBuffer(int width, int height, boolean alpha) {
        this.image = new WritableImage(width, height);
        this.alpha = alpha;

        this.writer = image.getPixelWriter();
        this.reader = image.getPixelReader();
    }

    @Override
    public Future<ReadableImageTile> getReadTile(int x, int y, int width, int height) {
        return CompletableFuture.completedFuture(new Tile(x, y, width, height));
    }

    @Override
    public WriteableImageTile getWriteTile(int x, int y, int width, int height) {
        return new Tile(x, y, width, height);
    }

    @Override
    public Future<ReadWriteImageTile> getReadWriteTile(int x, int y, int width, int height) {
        return CompletableFuture.completedFuture(new Tile(x, y, width, height));
    }

    @Override
    public int getWidth() {
        return (int) image.getWidth();
    }

    @Override
    public int getHeight() {
        return (int) image.getHeight();
    }

    /**
     * Get the backing JavaFX writeable image.
     */
    public WritableImage getImage() {
        return this.image;
    }

    public class Tile implements ReadWriteImageTile {
        protected final int x;
        protected final int y;
        protected final int width;
        protected final int height;

        protected Tile(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public int getBufferX(int x) {
            return this.x + x;
        }

        @Override
        public int getBufferY(int y) {
            return this.y + y;
        }

        @Override
        public int getBufferWidth() {
            return FxWriteableImageBuffer.this.getWidth();
        }

        @Override
        public int getBufferHeight() {
            return FxWriteableImageBuffer.this.getHeight();
        }

        @Override
        public int getTileWidth() {
            return width;
        }

        @Override
        public int getTileHeight() {
            return height;
        }

        @Override
        public double getColor(int x, int y, @Nullable Vector3 color) {
            assert boundsCheck(x, y);
            int argb = reader.getArgb(x, y);

            if (color != null) {
                color.x = (0xFF & (argb >> 16)) / 255.f;
                color.y = (0xFF & (argb >> 8)) / 255.f;
                color.z = (0xFF & argb) / 255.f;
            }
            if (alpha) {
                return (argb >>> 24) / 255.0;
            } else {
                return 1.0;
            }
        }

        @Override
        public void setColor(int x, int y, double r, double g, double b, double a) {
            assert boundsCheck(x, y);
            if (alpha) {
                writer.setArgb(x, y, ColorUtil.getArgbClamped(r, g, b, a));
            } else {
                writer.setArgb(x, y, ColorUtil.getArgbClamped(r, g, b, 1.0));
            }
        }

        @Override
        public void close() {

        }

        private boolean boundsCheck(int x, int y) {
            return this.x <= x && x < this.x + this.width &&
                   this.y <= y && y < this.y + this.height;
        }
    }
}
