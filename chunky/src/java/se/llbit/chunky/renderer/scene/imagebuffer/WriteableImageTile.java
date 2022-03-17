package se.llbit.chunky.renderer.scene.imagebuffer;

public interface WriteableImageTile extends ImageTile, AutoCloseable {
    /**
     * Set the color
     * @param x Buffer x
     * @param y Buffer y
     * @param r Red value
     * @param g Green value
     * @param b Blue value
     * @param a Alpha value if supported
     */
    void setColor(int x, int y, double r, double g, double b, double a);

    /**
     * Commit any changes made to the tile
     */
    void close();
}
