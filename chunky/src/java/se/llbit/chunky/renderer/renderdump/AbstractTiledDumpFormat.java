package se.llbit.chunky.renderer.renderdump;

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.renderbuffer.WriteableRenderTile;
import se.llbit.chunky.renderer.scene.renderbuffer.iteration.RenderBufferIterable;
import se.llbit.util.TaskTracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This is the newest dump format. This stores samples in a tiled format.
 */
public abstract class AbstractTiledDumpFormat implements DumpFormat {

    protected abstract void readTiles(DataInputStream inputStream, Scene scene, boolean merge, TaskTracker.Task progress) throws IOException;

    protected abstract void writeTiles(DataOutputStream outputStream, Scene scene, RenderBufferIterable<WriteableRenderTile> tiles, TaskTracker.Task progress) throws IOException;

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
            RenderBufferIterable<WriteableRenderTile> tiles = RenderBufferIterable.write(scene.getRenderBuffer());
            writeHeader(outputStream, scene, tiles.numTiles());
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

    static void writeHeader(DataOutputStream outputStream, Scene scene, long numTiles) throws IOException {
        outputStream.writeInt(scene.width);
        outputStream.writeInt(scene.height);
        outputStream.writeLong(scene.renderTime);
        outputStream.writeLong(numTiles);
    }
}
