package se.llbit.chunky.renderer.scene.imagebuffer;

import se.llbit.math.Vector3;
import se.llbit.util.annotation.Nullable;

public interface ReadableImageTile extends ImageTile {
    /**
     * Get the color.
     * @param x     Buffer x
     * @param y     Buffer y
     * @param color Color is returned in this Vector
     * @return Alpha value between 0.0 and 1.0 if supported. 1.0 if not.
     */
    double getColor(int x, int y, @Nullable Vector3 color);
}
