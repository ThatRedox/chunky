package se.llbit.chunky.renderer.scene.renderbuffer.iteration;

import se.llbit.chunky.renderer.scene.renderbuffer.*;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RenderBufferIterable<T extends RenderTile> implements Iterable<T> {
    protected final RenderBuffer buffer;

    protected RenderBufferIterable(RenderBuffer buffer) {
        this.buffer = buffer;
    }

    public static RenderBufferIterable<RenderTile> read(RenderBuffer buffer) {
        return new RenderBufferIterable<>(buffer);
    }

    public static RenderBufferIterable<WriteableRenderTile> write(WriteableRenderBuffer buffer) {
        return new RenderBufferIterable<>(buffer);
    }

    @Override
    public Iterator<T> iterator() {
        return new TileIterator<T>(buffer);
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliterator(this.iterator(), numTiles(),
            Spliterator.DISTINCT | Spliterator.IMMUTABLE |
                Spliterator.NONNULL | Spliterator.ORDERED |
                Spliterator.SIZED | Spliterator.SUBSIZED
        );
    }

    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<Pixel> pixelStream() {
        return this.stream().flatMap(RenderBufferIterable::pixelStream);
    }

    public static Stream<Pixel> pixelStream(RenderTile tile) {
        Iterator<Pixel> pixelIterator = new Iterator<Pixel>() {
            private int x;
            private int y;

            @Override
            public boolean hasNext() {
                return x < tile.getTileWidth() && y < tile.getTileHeight();
            }

            @Override
            public Pixel next() {
                Pixel pixel = Pixel.fromTile(tile, tile.getBufferX(x), tile.getBufferY(y));
                x += 1;
                if (x >= tile.getTileWidth()) {
                    x = 0;
                    y += 1;
                }
                return pixel;
            }
        };
        Spliterator<Pixel> pixelSpliterator = Spliterators.spliterator(pixelIterator,
            (long) tile.getTileWidth() * tile.getTileHeight(),
            Spliterator.DISTINCT | Spliterator.IMMUTABLE |
                Spliterator.NONNULL | Spliterator.ORDERED |
                Spliterator.SIZED | Spliterator.SUBSIZED
        );
        return StreamSupport.stream(pixelSpliterator, false);
    }

    public long numTiles() {
        long tilesV = buffer.getHeight() / RenderBuffer.TILE_SIZE;
        if (buffer.getHeight() % RenderBuffer.TILE_SIZE != 0) tilesV++;
        long tilesH = buffer.getWidth() / RenderBuffer.TILE_SIZE;
        if (buffer.getWidth() % RenderBuffer.TILE_SIZE != 0) tilesH++;
        return tilesV * tilesH;
    }

    public static class TileIterator<T extends RenderTile> implements Iterator<T> {
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
        @SuppressWarnings("unchecked")
        public T next() {
            if (tileFuture != null) {
                try {
                    RenderTile tile = tileFuture.get();
                    tileFuture = nextTile();
                    return (T) tile;
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalStateException("Attempted to iterate past the end of the tile iterator.");
            }
        }
    }
}
