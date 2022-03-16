package se.llbit.chunky.renderer.scene.renderbuffer.iteration;

import se.llbit.chunky.renderer.scene.renderbuffer.Pixel;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderTile;
import se.llbit.math.Vector3;
import se.llbit.util.annotation.NotNull;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RenderBufferTiledIterable {
    protected final RenderBuffer buffer;

    public RenderBufferTiledIterable(RenderBuffer buffer) {
        this.buffer = buffer;
    }

    public Iterable<RenderTile> tiles() {
        return () -> new TileIterator(buffer);
    }

    public Iterable<Pixel> pixels() {
        return new Iterable<Pixel>() {
            @NotNull
            @Override
            public Iterator<Pixel> iterator() {
                return new Iterator<Pixel>() {
                    private final TileIterator iterator = new TileIterator(buffer);
                    private RenderTile tile = iterator.hasNext() ? iterator.next() : null;
                    private int tileIndex = 0;

                    @Override
                    public boolean hasNext() {
                        return tile != null && (iterator.hasNext() || (tileIndex < tile.getTileWidth() * tile.getTileHeight()));
                    }

                    @Override
                    public Pixel next() {
                        if (tileIndex >= tile.getTileWidth() * tile.getTileHeight()) {
                            tile = iterator.next();
                            tileIndex = 0;
                        }
                        int x = tileIndex % tile.getTileWidth();
                        int y = tileIndex / tile.getTileWidth();
                        tileIndex++;

                        Vector3 color = new Vector3();
                        int samples = tile.getColor(x, y, color);
                        return new Pixel(color, samples, x, y);
                    }
                };
            }
        };
    }

    public static class TileIterator implements Iterator<RenderTile> {
        private final RenderBuffer buffer;
        private int x = 0;
        private int y = 0;
        private Future<? extends RenderTile> tileFuture;

        protected TileIterator(RenderBuffer buffer) {
            this.buffer = buffer;
            this.tileFuture = nextTile();
        }

        private Future<? extends RenderTile> nextTile() {
            int tileW = Math.min(RenderBuffer.TILE_SIZE, buffer.getWidth() - x);
            int tileH = Math.min(RenderBuffer.TILE_SIZE, buffer.getHeight() - y);
            if (y >= buffer.getHeight()) {
                return null;
            }
            Future<? extends RenderTile> tile = buffer.getTile(x, y, tileW, tileH);

            x += RenderBuffer.TILE_SIZE;
            if (x >= buffer.getWidth()) {
                x = 0;
                y += RenderBuffer.TILE_SIZE;
            }
            return tile;
        }

        @Override
        public boolean hasNext() {
            return tileFuture != null;
        }

        @Override
        public RenderTile next() {
            if (tileFuture != null) {
                try {
                    RenderTile tile = tileFuture.get();
                    tileFuture = nextTile();
                    return tile;
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalStateException("Attempted to iterate past the end of the tile iterator.");
            }
        }
    }
}
