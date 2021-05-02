/*
 * Copyright (c) 2016 Jesper Öqvist <jesper@llbit.se>
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
package se.llbit.chunky.renderer;

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.resources.BitmapImage;
import se.llbit.util.TaskTracker;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A renderer renders to a buffered image which is displayed by a render canvas.
 *
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public interface RenderManager {
  void setSceneProvider(SceneProvider sceneProvider);

  void setCanvas(Repaintable canvas);

  /**
   * Get all available {@code Renderer} names.
   */
  Collection<String> getRenderers();

  /**
   * Set the current {@code Renderer} by name.
   */
  void setRenderer(String value);

  /**
   * Get the current {@code Renderer} name.
   */
  String getRendererName();

  /**
   * Get all available preview {@code Renderer} names.
   */
  Collection<String> getPreviewRenderers();

  /**
   * Set the current preview {@code Renderer} by name.
   */
  void setPreviewRenderer(String value);

  /**
   * Get the current preview {@code Renderer} name.
   */
  String getPreviewRendererName();

  /**
   * Instructs the renderer to change its CPU load.
   */
  void setCPULoad(int loadPercent);

  /**
   * Set a listener for render completion.
   *
   * @param listener a listener which is passed the total rendering
   * time and average samples per second.
   */
  void setOnRenderCompleted(BiConsumer<Long, Integer> listener);

  /**
   * Set a listener for frame completion.
   *
   * @param listener a listener which is called when a frame completes
   * with the current scene and the current samples per pixel.
   */
  void setOnFrameCompleted(BiConsumer<Scene, Integer> listener);

  /**
   * Set the snapshot control mode.
   * Postprocessing should always occur if {@code SnapshotControl.saveSnapshot(...)} is true.
   */
  void setSnapshotControl(SnapshotControl callback);

  /**
   * Set the render taskTracker.
   */
  void setRenderTask(TaskTracker.Task task);

  /**
   * Add a listener that is called on every frame.
   */
  void addRenderListener(RenderStatusListener listener);
  void removeRenderListener(RenderStatusListener listener);

  /**
   * Run something with the buffered image (unsynchronized).
   */
  void withBufferedImage(Consumer<BitmapImage> bitmap);

  /**
   * Add a listener for the scene status tooltip.
   * This is called after every preview render.
   */
  void addSceneStatusListener(SceneStatusListener listener);
  void removeSceneStatusListener(SceneStatusListener listener);

  RenderStatus getRenderStatus();

  /**
   * Start up the renderer.
   * This should start all worker threads used by the renderer.
   */
  void start();

  /**
   * Wait for the renderer to terminate. This should only be done
   * in headless rendering, otherwise the renderer will not automatically
   * shut down after the render completes.
   */
  void join() throws InterruptedException;

  /**
   * Run something with the sample buffer (synchronized).
   */
  void withSampleBufferProtected(SampleBufferConsumer consumer);

  /**
   * Shut down the renderer.
   * This should interrupt all worker threads used by the renderer.
   */
  void shutdown();

  interface SampleBufferConsumer {
    void accept(double[] samples, int width, int height);
  }
}
