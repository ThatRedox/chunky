package se.llbit.chunky.renderer.scene.renderbuffer;

import se.llbit.math.Vector3;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class DoubleArrayRenderBuffer implements WriteableRenderBuffer {
    private final double[] samples;
    private final int[] spp;

    private final int width;
    private final int height;

    private final CopyOnWriteArraySet<RawPixelConsumer> pixelCallbacks = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArraySet<Consumer<RenderTile>> tileCallbacks = new CopyOnWriteArraySet<>();

    public class Tile implements WriteableRenderTile {
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
            return 0 <= x - offsetX && x - offsetX < tileWidth &&
                   0 <= y - offsetY && y - offsetY < tileHeight;
        }

        @Override
        public int getColor(int x, int y, Vector3 color) {
            assert boundsCheck(x, y);

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
            assert boundsCheck(x, y);

            int i = getIndex(x, y);
            int baseSpp = spp[i];
            double sinv = 1.0 / (baseSpp + s);

            r = (samples[i*3 + 0]*baseSpp + r*s) * sinv;
            g = (samples[i*3 + 1]*baseSpp + g*s) * sinv;
            b = (samples[i*3 + 2]*baseSpp + b*s) * sinv;

            samples[i*3 + 0] = r;
            samples[i*3 + 1] = g;
            samples[i*3 + 2] = b;
            spp[i] = baseSpp + s;

            for (RawPixelConsumer consumer : pixelCallbacks) {
                consumer.accept(x, y, r, g, b, s);
            }
        }

        @Override
        public void setPixel(int x, int y, double r, double g, double b, int s) {
            assert boundsCheck(x, y);

            int i = getIndex(x, y);
            samples[i*3 + 0] = r;
            samples[i*3 + 1] = g;
            samples[i*3 + 2] = b;
            spp[i] = s;

            for (RawPixelConsumer consumer : pixelCallbacks) {
                consumer.accept(x, y, r, g, b, s);
            }
        }

        @Override
        public void close() {
            for (Consumer<RenderTile> callback : tileCallbacks) {
                callback.accept(this);
            }
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

    public DoubleArrayRenderBuffer(int width, int height) {
        int bufferLength = 3 * width * height;

        this.width = width;
        this.height = height;
        this.samples = new double[bufferLength];
        this.spp = new int[bufferLength];
    }

    @Override
    public Future<Tile> getTile(int x, int y, int width, int height) {
        return CompletableFuture.completedFuture(new Tile(x, y, width, height));
        // TODO Remove this debug stuff
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//
//            }
//            return new Tile(x, y, width, height);
//        });
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
    public Set<RawPixelConsumer> getPixelCallbacks() {
        return pixelCallbacks;
    }

    @Override
    public Set<Consumer<RenderTile>> getTileCallbacks() {
        return tileCallbacks;
    }

    @Override
    public void reset() {
        Arrays.fill(spp, 0);
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
