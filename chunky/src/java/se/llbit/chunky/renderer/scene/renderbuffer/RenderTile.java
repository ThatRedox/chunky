package se.llbit.chunky.renderer.scene.renderbuffer;

import se.llbit.math.Vector3;

public interface RenderTile {
    /**
     * Get the color.
     * @param x     Tile x
     * @param y     Tile y
     * @param color Color is returned in this Vector
     * @return Number of samples
     */
    int getColor(int x, int y, Vector3 color);

    /**
     * Merge samples into a pixel.
     * @param x Tile x
     * @param y Tile y
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param s Number of samples
     */
    void mergeColor(int x, int y, double r, double g, double b, int s);

    /**
     * Merge samples into a pixel.
     * @param x     Tile x
     * @param y     Tile y
     * @param color Color
     * @param s     Number of samples
     */
    default void mergeColor(int x, int y, Vector3 color, int s) {
        mergeColor(x, y, color.x, color.y, color.z, s);
    }

    /**
     * Set a pixel.
     * @param x Tile x
     * @param y Tile y
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param s Number of samples
     */
    void setPixel(int x, int y, double r, double g, double b, int s);

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
