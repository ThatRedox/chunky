package se.llbit.chunky.renderer.renderdump;

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderTile;
import se.llbit.util.TaskTracker;
import sun.plugin.dom.exception.InvalidStateException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This is the newest dump format. This stores samples in a tiled format.
 */
public abstract class AbstractTiledDumpFormat implements DumpFormat {
    /**
     * Size of each tile written to disk.
     */
    public static final int TILE_SIZE = 128;

    public static class TileIterable implements Iterable<RenderTile> {
        protected final RenderBuffer buffer;
        protected final int tileSize;

        public TileIterable(RenderBuffer buffer, int tileSize) {
            this.buffer = buffer;
            this.tileSize = tileSize;
        }

        public long numTiles() {
            return (buffer.getHeight() / tileSize + 1L) * (buffer.getWidth() / tileSize + 1L);
        }

        @Override
        public Iterator<RenderTile> iterator() {
            return new Iterator<RenderTile>() {
                private int x = 0;
                private int y = 0;

                private Future<RenderTile> tileFuture = nextTile();

                private Future<RenderTile> nextTile() {
                    x += TILE_SIZE;
                    if (x >= buffer.getWidth()) {
                        x = 0;
                        y += TILE_SIZE;
                    }
                    if (y >= buffer.getHeight()) {
                        return null;
                    }
                    int tileW = Math.min(TILE_SIZE, buffer.getWidth() - x);
                    int tileH = Math.min(TILE_SIZE, buffer.getHeight() - y);
                    return buffer.getTile(x, y, tileW, tileH);
                }

                @Override
                public boolean hasNext() {
                    return (x + TILE_SIZE >= buffer.getWidth()) && (y + TILE_SIZE >= buffer.getHeight());
                }

                @Override
                public RenderTile next() {
                    if (tileFuture != null) {
                        try {
                            RenderTile tile = tileFuture.get();
                            tileFuture = nextTile();
                            return tile;
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        throw new InvalidStateException("Attempted to iterate past the end of the tile iterator.");
                    }
                }
            };
        }
    }

    protected abstract void readTiles(DataInputStream inputStream, Scene scene, boolean merge, TaskTracker.Task progress) throws IOException;

    protected abstract void writeTiles(DataOutputStream outputStream, Scene scene, TileIterable tiles, TaskTracker.Task progress) throws IOException;

    public abstract int getVersion();

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getId();

    @Override
    public void load(DataInputStream inputStream, Scene scene, TaskTracker taskTracker) throws IOException, IllegalStateException {
        try (TaskTracker.Task task = taskTracker.task("Loading render dump", scene.width * scene.height)) {
            readHeader(inputStream, scene);
            readTiles(inputStream, scene, false, task);
        }
    }

    @Override
    public void save(DataOutputStream outputStream, Scene scene, TaskTracker taskTracker)
        throws IOException {
        try (TaskTracker.Task task = taskTracker.task("Saving render dump", scene.width * scene.height)) {
            TileIterable tiles = new TileIterable(scene.getRenderBuffer(), TILE_SIZE);
            writeHeader(outputStream, scene);
            writeTiles(outputStream, scene, tiles, task);
        }
    }

    @Override
    public void merge(DataInputStream inputStream, Scene scene, TaskTracker taskTracker) throws IOException, IllegalStateException {
        try (TaskTracker.Task task = taskTracker.task("Merging render dump", scene.width * scene.height)) {
            readHeader(inputStream, scene);
            readTiles(inputStream, scene, true, task);
        }
    }

    static void readHeader(DataInputStream inputStream, Scene scene) throws IOException, IllegalStateException {
        int width = inputStream.readInt();
        int height = inputStream.readInt();

        if (width != scene.width || height != scene.height) {
            throw new IllegalStateException("Scene size does not match dump size");
        }

        scene.renderTime = inputStream.readLong();
    }

    static void writeHeader(DataOutputStream outputStream, Scene scene) throws IOException {
        outputStream.writeInt(scene.width);
        outputStream.writeInt(scene.height);
        outputStream.writeLong(scene.renderTime);
    }
}
