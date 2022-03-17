package se.llbit.chunky.renderer.scene.imagebuffer;

import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;

import java.util.concurrent.Future;

public interface ImageBuffer {
    /**
     * The preferred tile size.
     */
    int TILE_SIZE = RenderBuffer.TILE_SIZE;

    interface Factory<T extends ImageBuffer> {
        T create(int width, int height, boolean alpha);
    }

    /**
     * Get a read-only tile (potentially) asynchronously.
     *
     * @param x         Tile start x
     * @param y         Tile start y
     * @param width     Tile width
     * @param height    Tile height
     * @return Future which will resolve to the tile
     */
    Future<ReadableImageTile> getReadTile(int x, int y, int width, int height);

    /**
     * Get a write-only tile.
     *
     * @param x         Tile start x
     * @param y         Tile start y
     * @param width     Tile width
     * @param height    Tile height
     * @return The tile
     */
    WriteableImageTile getWriteTile(int x, int y, int width, int height);

    /**
     * Get a read-write tile (potentially) asynchronously.
     *
     * @param x         Tile start x
     * @param y         Tile start y
     * @param width     Tile width
     * @param height    Tile height
     * @return Future which will resolve to the tile
     */
    Future<ReadWriteImageTile> getReadWriteTile(int x, int y, int width, int height);

    /**
     * Get the width of the buffer.
     */
    int getWidth();

    /**
     * Get the height of the buffer.
     */
    int getHeight();
}
