package se.llbit.chunky.renderer.scene.imagebuffer;

public interface ImageTile {
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
