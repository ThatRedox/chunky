/* Copyright (c) 2021 Chunky contributors
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
package se.llbit.chunky.renderer.renderdump;

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderTile;
import se.llbit.math.Vector3;
import se.llbit.util.TaskTracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.IntConsumer;
import java.util.function.Supplier;


/**
 * This is the legacy dump format for 2.2 < Chunky <= 2.4.2
 * This stores the samples in a row major order.
 */
abstract class AbstractLegacyDumpFormat implements DumpFormat {
  /**
   * Aim for at least 65,536 pixels per tile
   */
  static final int MIN_PIXELS_PER_TILE = 1<<16;

  public static class Pixel {
    protected final int width;
    protected int x;
    protected int y;
    protected RenderTile tile;

    protected Pixel(int x, int y, int width, RenderTile tile) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.tile = tile;
    }

    public int getX() {
      return tile.getBufferX(x);
    }

    public int getY() {
      return tile.getBufferY(y);
    }

    public int getIndex() {
      return width*y + x;
    }

    public int getColor(Vector3 color) {
      return tile.getColor(x, y, color);
    }

    public void set(double r, double g, double b, int s) {
      tile.setPixel(x, y, r, g, b, s);
    }

    public void merge(double r, double g, double b, int s) {
      tile.mergeColor(x, y, r, g, b, s);
    }

    protected void setPixel(int x, int y, RenderTile tile) {
      this.x = x;
      this.y = y;
      this.tile = tile;
    }
  }

  public static class BufferIterator implements Iterator<Pixel> {
    protected final RenderBuffer buffer;
    protected final Pixel pixel;

    protected final int tileWidth;
    protected final int tileHeight;

    protected int currentY;
    protected Future<RenderTile> tileFuture;
    protected RenderTile tile;

    public BufferIterator(RenderBuffer buffer) {
      this.buffer = buffer;

      tileWidth = buffer.getWidth();
      tileHeight = Math.min(MIN_PIXELS_PER_TILE / buffer.getWidth() + 1, buffer.getHeight());

      currentY = 0;
      tileFuture = null;
      nextTile();
      nextTile();

      pixel = new Pixel(0, 0, buffer.getWidth(), tile);
    }

    private void nextTile() {
      try {
        if (tileFuture != null) {
          tile = tileFuture.get();
        }

        int h = tileHeight;
        if (h + currentY >= buffer.getHeight()) {
          h = buffer.getHeight() - currentY;
        }
        tileFuture = buffer.getTile(0, currentY, tileWidth, h);
        currentY += h;
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean hasNext() {
      return pixel.getY() + 1 < buffer.getHeight() || pixel.getX() + 1 < buffer.getWidth();
    }

    @Override
    public Pixel next() {
      int pixelX = pixel.x + 1;
      int pixelY = pixel.y;

      if (pixelX >= buffer.getWidth()) {
        pixelX = 0;
        pixelY += 1;
      }
      if (pixelY >= tile.getTileHeight()) {
        pixelY = 0;
        nextTile();
      }

      pixel.setPixel(pixelX, pixelY, tile);
      return pixel;
    }
  }

  public static class BufferIterable implements Iterable<Pixel> {
    protected final Supplier<Iterator<Pixel>> supplier;

    public BufferIterable(Supplier<Iterator<Pixel>> supplier) {
      this.supplier = supplier;
    }

    @Override
    public Iterator<Pixel> iterator() {
      return supplier.get();
    }
  }

  @FunctionalInterface
  public interface PixelConsumer {
    /**
     * @param pixelIndex Index of pixel between 0 and canvas.width*canvas.height (* 3 for index in sampleBuffer)
     * @param r          Red pixel value
     * @param g          Green pixel value
     * @param b          Blue pixel value
     */
    void consume(int pixelIndex, double r, double g, double b);
  }

  /**
   * Read samples from the stream.
   *
   * @param inputStream   Stream to read samples from.
   * @param scene         Scene this dump is a part of. Do not modify.
   * @param consumer      Pixel consumer. Must be in row major order.
   * @param pixelProgress Progress consumer. Inputs to this must be increasing and end at
   *                      {@code scene.width * scene.height}.
   */
  protected abstract void readSamples(DataInputStream inputStream, Scene scene,
                                      PixelConsumer consumer, IntConsumer pixelProgress)
      throws IOException;

  /**
   * Write sample buffer to the stream.
   *
   * @param outputStream  Stream to write to.
   * @param scene         Scene to take the sample buffer from. Do not modify.
   * @param pixels        Pixel iterable. Iterates in a row major order.
   * @param pixelProgress Progress consumer. Inputs must be increasing and end at
   *                      {@code scene.width * scene.height}.
   */
  protected abstract void writeSamples(DataOutputStream outputStream, Scene scene,
                                       Iterable<Pixel> pixels, IntConsumer pixelProgress)
      throws IOException;

  public abstract int getVersion();

  public abstract String getName();

  public abstract String getDescription();

  public abstract String getId();

  @Override
  public void load(DataInputStream inputStream, Scene scene, TaskTracker taskTracker)
      throws IOException, IllegalStateException {
    Iterator<Pixel> pixelIterator = new BufferIterator(scene.getRenderBuffer());

    try (TaskTracker.Task task = taskTracker.task("Loading render dump", scene.width * scene.height)) {
      int spp = readHeader(inputStream, scene);
      readSamples(inputStream, scene, (index, r, g, b) -> {
        assert pixelIterator.hasNext();
        Pixel pixel = pixelIterator.next();
        assert pixel.getIndex() == index;
        pixel.set(r, g, b, spp);
      }, i -> task.updateInterval(i, scene.width));
    }
  }

  @Override
  public void save(DataOutputStream outputStream, Scene scene, TaskTracker taskTracker)
      throws IOException {
    try (TaskTracker.Task task = taskTracker.task("Saving render dump", scene.width * scene.height)) {
      BufferIterable pixels = new BufferIterable(() -> new BufferIterator(scene.getRenderBuffer()));
      writeHeader(outputStream, scene);
      writeSamples(outputStream, scene, pixels, i -> task.updateInterval(i, scene.width));
    }
  }

  @Override
  public void merge(DataInputStream inputStream, Scene scene, TaskTracker taskTracker)
      throws IOException, IllegalStateException {
    try (TaskTracker.Task task = taskTracker.task("Merging render dump", scene.width * scene.height)) {
      long previousRenderTime = scene.renderTime;
      Iterator<Pixel> pixelIterator = new BufferIterator(scene.getRenderBuffer());

      int spp = readHeader(inputStream, scene);
      readSamples(inputStream, scene, (index, r, g, b) -> {
        assert pixelIterator.hasNext();
        Pixel pixel = pixelIterator.next();
        assert pixel.getIndex() == index;
        pixel.merge(r, g, b, spp);
      }, i -> task.updateInterval(i, scene.width));

      scene.renderTime += previousRenderTime;
    }
  }

  static int readHeader(DataInputStream inputStream, Scene scene) throws IOException, IllegalStateException {
    int width = inputStream.readInt();
    int height = inputStream.readInt();

    if (width != scene.canvasWidth() || height != scene.canvasHeight()) {
      throw new IllegalStateException("Scene size does not match dump size");
    }

    int spp = inputStream.readInt();
    scene.renderTime = inputStream.readLong();
    return spp;
  }

  static void writeHeader(DataOutputStream outputStream, Scene scene) throws IOException {
    int spp;
    try {
      spp = scene.getRenderBuffer().getTile(0, 0, 1, 1).get().getColor(0, 0, new Vector3());
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }

    outputStream.writeInt(scene.width);
    outputStream.writeInt(scene.height);
    outputStream.writeInt(spp);
    outputStream.writeLong(scene.renderTime);
  }
}
