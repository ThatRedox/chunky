package se.llbit.chunky.renderer.scene.renderbuffer;

import se.llbit.math.Vector3;

public interface WriteableRenderTile extends RenderTile, AutoCloseable {
    /**
     * Merge samples into a pixel.
     * @param x Buffer x
     * @param y Buffer y
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param s Number of samples
     */
    void mergeColor(int x, int y, double r, double g, double b, int s);

    /**
     * Merge samples into a pixel.
     * @param x     Buffer x
     * @param y     Buffer y
     * @param color Color
     * @param s     Number of samples
     */
    default void mergeColor(int x, int y, Vector3 color, int s) {
        mergeColor(x, y, color.x, color.y, color.z, s);
    }

    /**
     * Set a pixel.
     * @param x Buffer x
     * @param y Buffer y
     * @param r Red component
     * @param g Green component
     * @param b Blue component
     * @param s Number of samples
     */
    void setPixel(int x, int y, double r, double g, double b, int s);

    /**
     * Commit the changes made to this tile.
     */
    @Override
    void close();
}
