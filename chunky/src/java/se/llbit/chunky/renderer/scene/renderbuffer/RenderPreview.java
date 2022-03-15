package se.llbit.chunky.renderer.scene.renderbuffer;

import se.llbit.log.Log;
import se.llbit.math.Vector3;
import se.llbit.util.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public abstract class RenderPreview implements RenderBuffer {
    protected static final int MIN_WIDTH = 128;
    protected static final int MIN_HEIGHT = 128;

    /**
     * "Sample" buffer is single precision to decrease memory footprint.
     */
    protected float[] preview;
    protected int width;
    protected int height;

    /**
     * Set the preferred resolution.
     * @param perfWidth  Preferred width.
     * @param perfHeight Preferred height.
     */
    public abstract void setPreviewResolution(int perfWidth, int perfHeight);

    @Deprecated
    public abstract double[] asDoubleArray();

    public class Tile implements RenderTile {
        private final int offsetX;
        private final int offsetY;
        private final int tileWidth;
        private final int tileHeight;

        protected Tile(int offsetX, int offsetY, int tileWidth, int tileHeight) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
        }

        private int getIndex(int x, int y) {
            return x + y * width;
        }

        private boolean boundsCheck(int x, int y) {
            synchronized (RenderPreview.this) {
                return 0 <= x - offsetX && x - offsetX < tileWidth &&
                       0 <= y - offsetY && y - offsetY < tileHeight;
            }
        }

        @Override
        public int getColor(int x, int y, @Nullable Vector3 color) {
            assert boundsCheck(x, y);

            if (color == null) {
                return 1;
            }
            int index = getIndex(x, y);
            color.x = preview[3*index + 0];
            color.y = preview[3*index + 1];
            color.z = preview[3*index + 2];
            return 1;
        }

        @Override
        public int getBufferX(int x) {
            return x + offsetX;
        }

        @Override
        public int getBufferY(int y) {
            return y + offsetY;
        }

        @Override
        public int getBufferWidth() {
            return width;
        }

        @Override
        public int getBufferHeight() {
            return height;
        }

        @Override
        public int getTileWidth() {
            return tileWidth;
        }

        @Override
        public int getTileHeight() {
            return tileHeight;
        }
    }

    @Override
    public Future<Tile> getTile(int x, int y, int width, int height) {
        return CompletableFuture.completedFuture(new Tile(x, y, width, height));
    }

    /**
     * Height of the preview.
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * Width of the preview.
     */
    @Override
    public int getHeight() {
        return height;
    }

    /**
     * The preview of the preview is itself. This should not normally be called.
     */
    @Override
    public RenderPreview getPreview() {
        Log.warn("Attempted to get the render preview of a preview.");
        return this;
    }

    /**
     * This is not a real render buffer implementation.
     */
    @Override
    public String getName() {
        return null;
    }

    /**
     * This is not a real render buffer implementation.
     */
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * This is not a real render buffer implementation.
     */
    @Override
    public String getId() {
        return null;
    }
}
