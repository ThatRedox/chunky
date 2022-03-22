package se.llbit.chunky.renderer.scene.renderbuffer;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

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

    /**
     * Callbacks that are called whenever a tile is closed.
     */
    Set<Consumer<RenderTile>> getTileCallbacks();

    /**
     * Consumer of a single pixel.
     */
    interface RawPixelConsumer {
        void accept(int x, int y, double r, double g, double b, int samples);
    }

    /**
     * Callbacks that are called whenever a pixel is set.
     */
    Set<RawPixelConsumer> getPixelCallbacks();
}
