/*
 * Copyright (c) 2022 Chunky contributors
 *
 * This file is part of Chunky.
 *
 * Chunky is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Chunky is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Chunky.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.llbit.chunky.renderer.renderbuffer.iteration;

import se.llbit.chunky.renderer.renderbuffer.RenderBuffer;
import se.llbit.chunky.renderer.renderbuffer.RenderTile;
import se.llbit.chunky.renderer.renderbuffer.WriteableRenderBuffer;
import se.llbit.chunky.renderer.renderbuffer.WriteableRenderTile;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RenderBufferIterable<T extends RenderTile> implements Iterable<T> {
  public static RenderBufferIterable<RenderTile> read(RenderBuffer buffer) {
    return new RenderBufferIterable<>(buffer);
  }

  public static RenderBufferIterable<WriteableRenderTile> write(WriteableRenderBuffer buffer) {
    return new RenderBufferIterable<>(buffer);
  }

  protected final RenderBuffer buffer;

  protected RenderBufferIterable(RenderBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public Iterator<T> iterator() {
    return new TileIterator();
  }

  @Override
  public Spliterator<T> spliterator() {
    return Spliterators.spliterator(this.iterator(), numTiles(),
      Spliterator.DISTINCT | Spliterator.IMMUTABLE |
        Spliterator.NONNULL | Spliterator.ORDERED |
        Spliterator.SIZED | Spliterator.SUBSIZED);
  }

  public Stream<T> stream() {
    return StreamSupport.stream(this.spliterator(), false);
  }

  public long numTiles() {
    long tilesV = buffer.getHeight() / RenderBuffer.TILE_SIZE;
    if (buffer.getHeight() % RenderBuffer.TILE_SIZE != 0) tilesV++;
    long tilesH = buffer.getWidth() / RenderBuffer.TILE_SIZE;
    if (buffer.getWidth() % RenderBuffer.TILE_SIZE != 0) tilesH++;
    return tilesV * tilesH;
  }

  protected class TileIterator implements Iterator<T> {
    private int x = 0;
    private int y = 0;
    private Future<? extends RenderTile> tileFuture;

    protected TileIterator() {
      tileFuture = nextTile();
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
