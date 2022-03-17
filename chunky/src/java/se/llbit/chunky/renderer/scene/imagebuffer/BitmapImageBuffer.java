package se.llbit.chunky.renderer.scene.imagebuffer;

import se.llbit.chunky.resources.BitmapImage;
import se.llbit.math.ColorUtil;
import se.llbit.math.Vector3;
import se.llbit.util.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class BitmapImageBuffer implements ImageBuffer {
    protected final BitmapImage image;
    protected final boolean alpha;

    public BitmapImageBuffer(int width, int height, boolean alpha) {
        this.image = new BitmapImage(width, height);
        this.alpha = alpha;
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
        return image.width;
    }

    @Override
    public int getHeight() {
        return image.height;
    }

    public BitmapImage getBitmap() {
        return image;
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
            return image.width;
        }

        @Override
        public int getBufferHeight() {
            return image.height;
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
            int argb = image.getPixel(x, y);

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
                image.setPixel(x, y, ColorUtil.getArgbClamped(r, g, b, a));
            } else {
                image.setPixel(x, y, ColorUtil.getArgbClamped(r, g, b, 1.0));
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
