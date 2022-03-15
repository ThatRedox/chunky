package se.llbit.chunky.renderer.scene.renderbuffer;

import se.llbit.math.Vector3;
import se.llbit.util.annotation.Nullable;

public interface RenderTile {
    /**
     * Get the color.
     * @param x     Buffer x
     * @param y     Buffer y
     * @param color Color is returned in this Vector
     * @return Number of samples
     */
    int getColor(int x, int y, @Nullable Vector3 color);

    /**
     * Get the buffer coordinates of a pixel.
     * @param x Tile x
     * @return  Buffer x
     */
    int getBufferX(int x);

    /**
     * Get the buffer coordinates of a pixel.
     * @param y Tile y
     * @return  Buffer y
     */
    int getBufferY(int y);

    /**
     * Get the width of the buffer.
     */
    int getBufferWidth();

    /**
     * Get the height of the buffer.
     */
    int getBufferHeight();

    /**
     * Get the width of the tile.
     */
    int getTileWidth();

    /**
     * Get the height of the tile.
     */
    int getTileHeight();
}
