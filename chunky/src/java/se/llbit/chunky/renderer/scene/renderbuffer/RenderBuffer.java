package se.llbit.chunky.renderer.scene.renderbuffer;

import se.llbit.util.Registerable;

import java.util.concurrent.Future;

public interface RenderBuffer extends Registerable {
    interface Factory {
        RenderBuffer create(long width, long height);
    }

    /**
     * Get a tile (potentially) asynchronously.
     *
     * @param x         Tile start x
     * @param y         Tile start y
     * @param width     Tile width
     * @param height    Tile height
     * @return Future which will resolve to the tile
     */
    Future<RenderTile> getTile(long x, long y, long width, long height);

    /**
     * Get the width of this buffer.
     */
    long getWidth();

    /**
     * Get the height of this buffer.
     */
    long getHeight();
}
