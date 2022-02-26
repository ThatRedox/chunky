package se.llbit.chunky.renderer.scene.renderbuffer;

import se.llbit.util.Registerable;

import java.util.concurrent.Future;

public interface RenderBuffer extends Registerable {
    /**
     * The preferred tile size.
     */
    int TILE_SIZE = 128;

    interface Factory {
        RenderBuffer create(int width, int height);
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
    Future<RenderTile> getTile(int x, int y, int width, int height);

    /**
     * Get the width of this buffer.
     */
    int getWidth();

    /**
     * Get the height of this buffer.
     */
    int getHeight();

    /**
     * Set the preferred preview resolution.
     * @param perfWidth  preferred width
     * @param perfHeight preferred height
     */
    void setPreviewResolution(int perfWidth, int perfHeight);

    /**
     * Get the render preview.
     */
    RenderPreview getPreview();
}
