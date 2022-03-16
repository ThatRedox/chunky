package se.llbit.chunky.renderer.scene.renderbuffer.iteration;

import se.llbit.chunky.renderer.scene.renderbuffer.Pixel;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderTile;
import se.llbit.math.Vector3;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RenderBufferRowMajorIterator implements Iterator<Pixel> {
    protected RenderBuffer buffer;
    protected Future<? extends RenderTile> tileFuture;
    protected RenderTile tile;
    protected int x = 0;
    protected int y = 0;
    protected int bufferY = 0;

    public RenderBufferRowMajorIterator(RenderBuffer buffer) {
        this.buffer = buffer;
        try {
            this.tile = buffer.getTile(0, 0, buffer.getWidth(), Math.min(RenderBuffer.TILE_SIZE, buffer.getHeight())).get();
            this.bufferY += this.tile.getTileHeight();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    protected RenderTile nextTile() {
        try {
            if (tileFuture == null)
                return null;
            RenderTile tile = tileFuture.get();

            int nextHeight = Math.min(RenderBuffer.TILE_SIZE, buffer.getHeight() - y);
            if (nextHeight <= 0) {
                tileFuture = null;
            }
            tileFuture = buffer.getTile(0, bufferY, buffer.getWidth(), nextHeight);
            bufferY += nextHeight;

            return tile;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return this.x < buffer.getWidth() || y < buffer.getHeight();
    }

    @Override
    public Pixel next() {
        Vector3 color = new Vector3();
        int s = tile.getColor(x, y, color);
        Pixel out = new Pixel(color, s, x, y);

        x += 1;
        if (x >= tile.getBufferWidth()) {
            x = 0;
            y += 1;
        }
        if (y >= tile.getBufferHeight()) {
            tile = nextTile();
        }

        return out;
    }
}
