package se.llbit.chunky.renderer.scene.renderbuffer;

import java.util.concurrent.Future;

public interface WriteableRenderBuffer extends RenderBuffer {
    /**
     * Get a tile (potentially) asynchronously.
     *
     * @param x         Tile start x
     * @param y         Tile start y
     * @param width     Tile width
     * @param height    Tile height
     * @return Future which will resolve to the tile
     */
    @Override
    Future<? extends WriteableRenderTile> getTile(int x, int y, int width, int height);

    /**
     * Reset the render buffer and clear all samples.
     */
    void reset();
}
