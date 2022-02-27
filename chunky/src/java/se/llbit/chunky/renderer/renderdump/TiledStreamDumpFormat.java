package se.llbit.chunky.renderer.renderdump;

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderTile;
import se.llbit.log.Log;
import se.llbit.math.Vector3;
import se.llbit.util.IsolatedOutputStream;
import se.llbit.util.TaskTracker;

import java.io.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

public class TiledStreamDumpFormat extends AbstractTiledDumpFormat {
    public static final TiledStreamDumpFormat UNCOMPRESSED = new TiledStreamDumpFormat(8,
        is -> is, os -> os, "Tiled Uncompressed Dump", "Tiled uncompressed dump format.", "TiledUncompressedDumpFormat");

    protected final int version;
    protected final String name;
    protected final String description;
    protected final String id;

    protected final Function<InputStream, InputStream> inputStreamSupplier;
    protected final Function<OutputStream, OutputStream> outputStreamSupplier;

    public TiledStreamDumpFormat(int version,
                                 Function<InputStream, InputStream> inputStreamSupplier,
                                 Function<OutputStream, OutputStream> outputStreamSupplier,
                                 String name, String description, String id) {
        this.version = version;
        this.name = name;
        this.description = description;
        this.id = id;

        this.inputStreamSupplier = inputStreamSupplier;
        this.outputStreamSupplier = outputStreamSupplier;
    }

    @Override
    protected void readTiles(DataInputStream inputStream, Scene scene, boolean merge, TaskTracker.Task progress) throws IOException {
        DataInputStream in = new DataInputStream(inputStreamSupplier.apply(inputStream));
        RenderBuffer renderBuffer = scene.getRenderBuffer();

        // Read number of tiles
        long numTiles = in.readLong();

        long tilesComplete = 0;
        progress.update((int) numTiles, (int) tilesComplete);

        for (long i = 0; i < numTiles; i++) {
            // Read the tile coordinates
            int tileX = in.readInt();
            int tileY = in.readInt();

            // Read the tile size
            int tileWidth = in.readInt();
            int tileHeight = in.readInt();

            // Fetch the tile
            Future<RenderTile> tileFuture = renderBuffer.getTile(tileX, tileY, tileWidth, tileHeight);

            // Read the spp
            int[] spp = new int[tileWidth * tileHeight];
            for (int y = 0; y < tileHeight; y++) {
                for (int x = 0; x < tileWidth; x++) {
                    spp[y*tileWidth + x] = in.readInt();
                }
            }

            // Get the tile
            RenderTile tile;
            try {
                tile = tileFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            // Read the samples
            for (int y = 0; y < tileHeight; y++) {
                for (int x = 0; x < tileWidth; x++) {
                    double r = in.readDouble();
                    double g = in.readDouble();
                    double b = in.readDouble();
                    int s = spp[y*tileWidth + x];

                    // Set the pixel
                    if (merge) {
                        tile.mergeColor(x, y, r, g, b, s);
                    } else {
                        tile.setPixel(x, y, r, g, b, s);
                    }
                }
            }

            // Update progress (int cast should fail at about 35 trillion pixels, so it's fine for now)
            progress.update((int) numTiles, (int) (tilesComplete++));
        }
    }

    @Override
    protected void writeTiles(DataOutputStream outputStream, Scene scene, TileIterable tiles, TaskTracker.Task progress) throws IOException {
        Vector3 color = new Vector3();
        long tilesComplete = 0;
        long numTiles = tiles.numTiles();
        progress.update((int) numTiles, (int) tilesComplete);

        try (DataOutputStream out = new DataOutputStream(outputStreamSupplier.apply(new IsolatedOutputStream(outputStream)))) {
            // Write number of tiles
            out.writeLong(numTiles);

            for (RenderTile tile : tiles) {
                // Write tile coordinates
                out.writeInt(tile.getBufferX(0));
                out.writeInt(tile.getBufferY(0));

                // Write the tile dimensions
                out.writeInt(tile.getTileWidth());
                out.writeInt(tile.getTileHeight());

                // Write the tile spp
                for (int y = 0; y < tile.getTileHeight(); y++) {
                    for (int x = 0; x < tile.getTileWidth(); x++) {
                        out.writeInt(tile.getColor(x, y, null));
                    }
                }

                // Write the tile samples
                for (int y = 0; y < tile.getTileHeight(); y++) {
                    for (int x = 0; x < tile.getTileWidth(); x++) {
                        tile.getColor(x, y, color);

                        out.writeDouble(color.x);
                        out.writeDouble(color.y);
                        out.writeDouble(color.z);
                    }
                }

                // Update progress (int cast should fail at about 35 trillion pixels, so it's fine for now)
                progress.update((int) numTiles, (int) (tilesComplete++));
            }

            if (numTiles != tilesComplete) {
                Log.errorf("Not all tiles written. %d / %d written.", tilesComplete, numTiles);
            }
        }
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getId() {
        return id;
    }
}
