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
import se.llbit.chunky.renderer.scene.renderbuffer.WriteableRenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.WriteableRenderTile;
import se.llbit.log.Log;
import se.llbit.math.Vector3;
import se.llbit.util.IsolatedOutputStream;
import se.llbit.util.TaskTracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This is the legacy dump format for <= Chunky 2.2.
 * <p>
 * The format is a GZIP stream containing some canvas information followed by the render dump
 * written in column major order.
 */
public class ClassicDumpFormat implements DumpFormat {
  public static final ClassicDumpFormat INSTANCE = new ClassicDumpFormat();

  public static class BufferIterator implements Iterator<AbstractLegacyDumpFormat.Pixel> {
    protected final WriteableRenderBuffer buffer;
    protected final AbstractLegacyDumpFormat.Pixel pixel;

    protected final int tileWidth;
    protected final int tileHeight;

    protected int currentX;
    protected Future<? extends WriteableRenderTile> tileFuture;
    protected WriteableRenderTile tile;

    public BufferIterator(WriteableRenderBuffer buffer) {
      this.buffer = buffer;

      tileWidth = Math.min(AbstractLegacyDumpFormat.MIN_PIXELS_PER_TILE / buffer.getHeight() + 1, buffer.getWidth());
      tileHeight = buffer.getHeight();

      currentX = 0;
      tileFuture = null;
      nextTile();
      nextTile();

      pixel = new AbstractLegacyDumpFormat.Pixel(0, -1, buffer.getWidth(), tile);
    }

    private void nextTile() {
      try {
        if (tileFuture != null) {
          tile = tileFuture.get();
        }

        int w = tileWidth;
        if (w + currentX >= buffer.getWidth()) {
          w = buffer.getWidth() - currentX;
        }
        tileFuture = buffer.getTile(currentX, 0, w, tileHeight);
        currentX += w;
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public AbstractLegacyDumpFormat.Pixel next() {
      int pixelX = pixel.x;
      int pixelY = pixel.y + 1;

      if (pixelY >= buffer.getHeight()) {
        pixelY = 0;
        pixelX += 1;
      }
      if (pixelX >= tile.getTileWidth()) {
        pixelX = 0;
        nextTile();
      }

      pixel.setPixel(pixelX, pixelY, tile);
      return pixel;
    }

    @Override
    public boolean hasNext() {
      return pixel.getY() + 1 < buffer.getHeight() || pixel.getX() + 1 < buffer.getWidth();
    }
  }

  private ClassicDumpFormat() {}

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public String getName() {
    return "Classic";
  }

  @Override
  public String getDescription() {
    return "Legacy dump format from Chunky <= 2.2";
  }

  @Override
  public String getId() {
    return "ClassicDumpFormat";
  }

  @Override
  public void load(DataInputStream inputStream, Scene scene, TaskTracker taskTracker)
      throws IOException, IllegalStateException {
    DataInputStream in = new DataInputStream(new GZIPInputStream(inputStream));
    try (TaskTracker.Task task = taskTracker.task("Loading render dump", scene.width * scene.height)) {
      int spp = LegacyStreamDumpFormat.readHeader(in, scene);

      int done = 0;
      Iterator<AbstractLegacyDumpFormat.Pixel> iterator = new BufferIterator(scene.getRenderBuffer());
      while (iterator.hasNext()) {
        iterator.next().set(
            in.readDouble(),
            in.readDouble(),
            in.readDouble(),
            spp
        );

        task.updateInterval(done++, scene.height);
      }
    }
  }

  @Override
  public void save(DataOutputStream outputStream, Scene scene, TaskTracker taskTracker) throws IOException {
    Log.warn("Saving classic dump format loses sample information!");

    try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new IsolatedOutputStream(outputStream)))) {
      try (TaskTracker.Task task = taskTracker.task("Saving render dump", scene.width * scene.height)) {
        AbstractLegacyDumpFormat.writeHeader(outputStream, scene);

        int done = 0;
        Vector3 color = new Vector3();
        Iterator<AbstractLegacyDumpFormat.Pixel> iterator = new BufferIterator(scene.getRenderBuffer());
        while (iterator.hasNext()) {
          iterator.next().getColor(color);

          out.writeDouble(color.x);
          out.writeDouble(color.y);
          out.writeDouble(color.z);

          task.updateInterval(done++, scene.height);
        }
      }
    }
  }

  @Override
  public void merge(DataInputStream inputStream, Scene scene, TaskTracker taskTracker)
      throws IOException, IllegalStateException {
    DataInputStream in = new DataInputStream(new GZIPInputStream(inputStream));
    try (TaskTracker.Task task = taskTracker.task("Merging render dump", scene.width * scene.height)) {
      long previousRenderTime = scene.renderTime;
      int spp = LegacyStreamDumpFormat.readHeader(in, scene);
      scene.renderTime += previousRenderTime;

      int done = 0;
      Iterator<AbstractLegacyDumpFormat.Pixel> iterator = new BufferIterator(scene.getRenderBuffer());
      while (iterator.hasNext()) {
        iterator.next().merge(
            in.readDouble(),
            in.readDouble(),
            in.readDouble(),
            spp
        );

        task.updateInterval(done++, scene.height);
      }
    }
  }
}
