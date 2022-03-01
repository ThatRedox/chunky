package se.llbit.chunky.renderer.scene.renderbuffer;

import se.llbit.math.Vector3;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class DoubleArrayRenderBuffer implements RenderBuffer {
    private final double[] samples;
    private final int[] spp;

    private final int width;
    private final int height;

    public class Tile implements RenderTile {
        private final int xOffset;
        private final int yOffset;
        private final int tileWidth;
        private final int tileHeight;

        protected Tile(int xOffset, int yOffset, int tileWidth, int tileHeight) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
        }

        private int getIndex(int x, int y) {
            return Math.toIntExact(getBufferX(x) + getBufferY(y) * width);
        }

        @Override
        public int getColor(int x, int y, Vector3 color) {
            int i = getIndex(x, y);
            if (color == null) {
                return spp[i];
            }

            color.x = samples[i*3 + 0];
            color.y = samples[i*3 + 1];
            color.z = samples[i*3 + 2];
            return spp[i];
        }

        @Override
        public void mergeColor(int x, int y, double r, double g, double b, int s) {
            int i = getIndex(x, y);
            int baseSpp = spp[i];
            double sinv = 1.0 / (baseSpp + s);

            samples[i*3 + 0] = (samples[i*3 + 0]*baseSpp + r*s) * sinv;
            samples[i*3 + 1] = (samples[i*3 + 1]*baseSpp + g*s) * sinv;
            samples[i*3 + 2] = (samples[i*3 + 2]*baseSpp + b*s) * sinv;
        }

        @Override
        public void setPixel(int x, int y, double r, double g, double b, int s) {
            int i = getIndex(x, y);
            samples[i*3 + 0] = r;
            samples[i*3 + 1] = g;
            samples[i*3 + 2] = b;
            spp[i] = s;
        }

        @Override
        public void commit() {
            // Nothing needs to be done here
        }

        @Override
        public int getBufferX(int x) {
            return x + xOffset;
        }

        @Override
        public int getBufferY(int y) {
            return y + yOffset;
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

    public class Preview implements RenderPreview {
        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public double[] getPreview() {
            return samples;
        }
    }

    public DoubleArrayRenderBuffer(int width, int height) {
        int bufferLength = Math.toIntExact(3 * width * height);

        this.width = width;
        this.height = height;
        this.samples = new double[bufferLength];
        this.spp = new int[bufferLength];
    }

    @Override
    public Future<RenderTile> getTile(int x, int y, int width, int height) {
        return CompletableFuture.completedFuture(new Tile(x, y, width, height));
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
    public void setPreviewResolution(int perfWidth, int perfHeight) {
        // Does nothing right now
    }

    @Override
    public RenderPreview getPreview() {
        return new Preview();
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
    
    @Deprecated
    public double[] getSampleBuffer() {
        return samples;
    }
}
