package se.llbit.chunky.renderer.scene.renderbuffer;

import se.llbit.math.Vector3;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class DoubleArrayRenderBuffer implements RenderBuffer {
    private final double[] samples;
    private final int[] spp;

    private final long width;
    private final long height;

    public class Tile implements RenderTile {
        private final long xOffset;
        private final long yOffset;
        private final long tileWidth;
        private final long tileHeight;

        protected Tile(long xOffset, long yOffset, long tileWidth, long tileHeight) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
        }

        private int getIndex(long x, long y) {
            return Math.toIntExact(getBufferX(x) + getBufferY(y) * width);
        }

        @Override
        public int getColor(long x, long y, Vector3 color) {
            int i = getIndex(x, y);
            color.x = samples[i*3 + 0];
            color.y = samples[i*3 + 1];
            color.z = samples[i*3 + 2];
            return spp[i];
        }

        @Override
        public void mergeColor(long x, long y, double r, double g, double b, int s) {
            int i = getIndex(x, y);
            int baseSpp = spp[i];
            double sinv = 1.0 / (baseSpp + s);

            samples[i*3 + 0] = (samples[i*3 + 0]*baseSpp + r*s) * sinv;
            samples[i*3 + 1] = (samples[i*3 + 1]*baseSpp + g*s) * sinv;
            samples[i*3 + 2] = (samples[i*3 + 2]*baseSpp + b*s) * sinv;
        }

        @Override
        public void setPixel(long x, long y, double r, double g, double b, int s) {
            int i = getIndex(x, y);
            samples[i*3 + 0] = r;
            samples[i*3 + 1] = g;
            samples[i*3 + 2] = b;
            spp[i] = s;
        }

        @Override
        public long getBufferX(long x) {
            return x + xOffset;
        }

        @Override
        public long getBufferY(long y) {
            return y + yOffset;
        }

        @Override
        public long getBufferWidth() {
            return width;
        }

        @Override
        public long getBufferHeight() {
            return height;
        }

        @Override
        public long getTileWidth() {
            return tileWidth;
        }

        @Override
        public long getTileHeight() {
            return tileHeight;
        }
    }

    public DoubleArrayRenderBuffer(long width, long height) {
        int bufferLength = Math.toIntExact(3 * width * height);

        this.width = width;
        this.height = height;
        this.samples = new double[bufferLength];
        this.spp = new int[bufferLength];
    }

    @Override
    public Future<RenderTile> getTile(long x, long y, long width, long height) {
        return CompletableFuture.completedFuture(new Tile(x, y, width, height));
    }

    @Override
    public long getWidth() {
        return width;
    }

    @Override
    public long getHeight() {
        return height;
    }

    @Override
    public String getName() {
        return "Double Array Render Buffer";
    }

    @Override
    public String getDescription() {
        return "Double precision in-memory render buffer.";
    }

    @Override
    public String getId() {
        return "DoubleArrayRenderBuffer";
    }
}
