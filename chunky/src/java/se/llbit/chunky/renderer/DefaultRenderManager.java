/* Copyright (c) 2012-2021 Jesper Öqvist <jesper@llbit.se>
 * Copyright (c) 2012-2021 Chunky contributors
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

import se.llbit.chunky.PersistentSettings;
import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.plugin.PluginApi;
import se.llbit.chunky.renderer.scene.PathTracer;
import se.llbit.chunky.renderer.scene.PreviewRayTracer;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.renderbuffer.WriteableRenderBuffer;
import se.llbit.log.Log;
import se.llbit.util.TaskTracker;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * This class serves to interface {@code Renderer}s with Chunky.
 * It holds a pool of render workers and manages the scene state. It starts
 * the desired {@code Renderer} when rendering starts.
 *
 * <p>Scene state is kept in Scene objects. The render controls dialog
 * stores the scene state in its own Scene object, which is read by
 * the render manager through the SceneProvider interface.
 * The render manager keeps an internal copy of the scene state which
 * is ensured to be unmodified while render workers are rendering.
 *
 * <p>A snapshot of the current render can be accessed by calling withSnapshot().
 *
 * <p>All available final renderers are stored in {@code renderers} and preview renderers
 * are stored in {@code previewRenderers}.
 */
public class DefaultRenderManager extends Thread implements RenderManager {
  /**
   * Map containing all the final render {@code Renderer}s. The renderer corresponding to
   * {@code getRendererName()} is used when a render is requested.
   */
  @PluginApi
  public static final Map<String, Renderer> renderers = new HashMap<>();

  /**
   * Map containing all the preview render {@code Renderer}s. The renderer corresponding to
   * {@code getPreviewRendererName()} is used when a preview is needed.
   */
  @PluginApi
  public static final Map<String, Renderer> previewRenderers = new HashMap<>();

  /**
   * The ID's for the builtin path tracer and preview renderers.
   * DO NOT use these ID's other than for the builtin renderers.
   */
  public static final String ChunkyPathTracerID = "PathTracingRenderer";
  public static final String ChunkyPreviewID = "PreviewRenderer";

  static {
    addRenderer(new PathTracingRenderer(ChunkyPathTracerID, "Chunky Path Tracer",
        "A photorealistic Path Tracing renderer.", new PathTracer()));
    addPreviewRenderer(new PreviewRenderer(ChunkyPreviewID, "Chunky Preview",
        "A simple ray marching preview renderer.", new PreviewRayTracer()));
  }

  /**
   * The current renderer selections.
   */
  protected String renderer = ChunkyPathTracerID;
  protected String previewRenderer = ChunkyPreviewID;

  protected RenderPreview<?> preview = null;

  /**
   * This is a buffered scene which render workers should use while rendering.
   * The buffered scene is only updated when the workers are quiescent.
   *
   * Render workers should:
   *  * Increment {@code bufferedScene.spp} after rendering each frame
   *  * Merge the new frame with {@code bufferedScene.getSampleBuffer()}
   *
   * Render workers should not otherwise modify this.
   */
  public final Scene bufferedScene;

  /**
   * This stores the reset count of the bufferedScene. It is incremented whenever a reset occurs.
   */
  private int resetCount = 1;

  /**
   * This is the render worker pool {@code Renderer}s should use.
   *
   * {@code Renderer}s should submit small work-units to this pool. CPU usage is limited automatically, but if
   * each work-unit can take a long time, they should call {@code RenderWorkerPool.RenderWorker.workSleep()}
   * periodically to manage CPU usage.
   */
  public final RenderWorkerPool pool;

  protected int cpuLoad = 100;

  /**
   * The render canvas. This is redrawn on every frame (if applicable).
   */
  protected Repaintable canvas = () -> {};

  /**
   * Decides if the canvas is in view and every frame needs to be finalized. If not, only
   * the frames before a snapshot are finalized.
   */
  protected boolean finalizeAllFrames = false;

  /**
   * Listeners that need to be called on every frame when rendering.
   */
  private final Collection<RenderStatusListener> renderStatusListeners = new ArrayList<>();

  /**
   * Listeners that need to be called on every frame when previewing.
   */
  private final Collection<SceneStatusListener> sceneStatusListeners = new ArrayList<>();

  private BiConsumer<Long, Integer> renderCompletionListener = (time, sps) -> {};
  private BiConsumer<Scene, Integer> frameCompletionListener = (scene, spp) -> {};

  protected TaskTracker.Task renderTask = TaskTracker.Task.NONE;

  private long frameStart;

  /**
   * Decides if render threads shut down after reaching the target SPP.
   */
  private final boolean headless;

  /**
   * Current renderer mode.
   */
  protected RenderMode mode = RenderMode.PREVIEW;

  /**
   * Current snapshot mode. Frame will always be post processed if a snapshot will happen after that frame.
   */
  protected SnapshotControl snapshotControl = SnapshotControl.DEFAULT;

  protected SceneProvider sceneProvider;
  public final RenderContext context;

  /**
   * This renderer does nothing. Is used when there is an invalid renderer.
   */
  protected static final Renderer EMPTY_RENDERER = new Renderer() {
    @Override public String getId() { return "Empty"; }
    @Override public String getName() { return "Empty"; }
    @Override public String getDescription() { return "Empty Renderer"; }
    @Override public void render(DefaultRenderManager manager, RenderStatusCallback statusCallback) { }
  };

  protected final RenderStatusCallback previewCallback;
  protected final RenderStatusCallback renderCallback;

  /**
   * @param headless {@code true} if rendering threads should be shut
   * down after reaching the render target.
   */
  public DefaultRenderManager(RenderContext context, boolean headless) {
    super("Internal Render Manager");

    this.context = context;
    this.headless = headless;
    this.bufferedScene = context.getChunky().getSceneFactory().newScene();

    // Create a new pool. Set the seed to the current time in milliseconds.
    this.pool = context.renderPoolFactory.create(context.numRenderThreads(), System.currentTimeMillis());
    this.setCPULoad(PersistentSettings.getCPULoad());

    // Initialize callbacks here since java will complain `bufferedScene` is not initialized yet.
    // (nothing important in the rest of the constructor)
    this.previewCallback = (complete, total) -> {
      sendSceneStatus(bufferedScene.sceneStatus());

      renderStatusListeners.forEach(listener -> {
        listener.setRenderTime(System.currentTimeMillis() - frameStart);
        listener.setSamplesPerSecond(0);
        listener.setSpp(0);
      });

      if (getPreviewRenderer().autoPostProcess())
        this.finalizeFrame(true);

      frameStart = System.currentTimeMillis();
      return !finalizeAllFrames || sceneProvider.pollSceneStateChange();
    };

    this.renderCallback = (complete, total) -> {
      long elapsedTime = System.currentTimeMillis() - frameStart;

      sceneProvider.withSceneProtected(scene -> {
        synchronized (bufferedScene) {
          bufferedScene.copyTransients(scene);
          updateRenderState(scene);
        }
      });

      synchronized (bufferedScene) {
        bufferedScene.renderTime += elapsedTime;

        this.finalizeFrame(getRenderer().autoPostProcess() && finalizeAllFrames);

        int spp = (int) ((complete * bufferedScene.getTargetSpp()) / total);
        frameCompletionListener.accept(bufferedScene, spp);
        updateRenderProgress(spp, complete, total);

        if (complete >= total) {
          renderCompletionListener.accept(bufferedScene.renderTime, samplesPerSecond(spp));
          return true;
        }
      }

      frameStart = System.currentTimeMillis();
      return mode == RenderMode.PAUSED || sceneProvider.pollSceneStateChange();
    };
  }

  /**
   * This controls most of the render manager logic.
   */
  @Override
  public void run() {
    try {
      while (!isInterrupted()) {
        // Wait for a scene state change (e.g. user editing the scene)
        ResetReason reason = sceneProvider.awaitSceneStateChange();

        // Copy the new scene state to the bufferedScene
        synchronized (bufferedScene) {
          sceneProvider.withSceneProtected(scene -> {
            if (reason.overwriteState()) {
              bufferedScene.copyState(scene);
            }
            if (reason == ResetReason.MATERIALS_CHANGED || reason == ResetReason.SCENE_LOADED) {
              scene.importMaterials();
            }

            bufferedScene.copyTransients(scene);
            finalizeAllFrames = scene.shouldFinalizeBuffer();
            updateRenderState(scene);

            if (reason == ResetReason.SCENE_LOADED) {
              // Make sure frame is finalized
              if (preview != null) {
                preview.register(bufferedScene.getRenderBuffer());
                preview.withImageProtected(image ->
                    bufferedScene.getPostProcessingFilter().processFrame(preview.getPreview(), image,
                        bufferedScene.getExposure(), TaskTracker.Task.NONE));
              }

              // Reset the task
              if (mode == RenderMode.PAUSED) {
                updateRenderProgress(0, 0, 1);
              }

              // Notify the scene listeners.
              sendSceneStatus(bufferedScene.sceneStatus());
              canvas.repaint();
            }
          });
        }

        // Select the renderer from the scene
        setRenderer(bufferedScene.getRenderer());
        setPreviewRenderer(bufferedScene.getPreviewRenderer());

        // Ensure preview is registered
        if (preview != null) preview.register(bufferedScene.getRenderBuffer());

        // Notify renderers
        resetCount += 1;
        getRenderer().sceneReset(this, reason, resetCount);
        getPreviewRenderer().sceneReset(this, reason, resetCount);

        // Select the correct renderer
        Renderer render = mode == RenderMode.PREVIEW ? getPreviewRenderer() : getRenderer();

        frameStart = System.currentTimeMillis();
        if (mode == RenderMode.PREVIEW) {
          // Bail early if the preview is not visible
          if (finalizeAllFrames) {
            // Preview with no CPU limit
            pool.setCpuLoad(100);
            render.render(this, previewCallback);
            pool.setCpuLoad(cpuLoad);
          }
        } else {
          render.render(this, renderCallback);
        }

        if (headless) break;
      }
    } catch (InterruptedException e) {
      // Interrupted
    } catch (Throwable e) {
      Log.error("Unchecked exception in render manager.", e);
    }
  }

  /**
   * Redraw the GUI screen. This should be run after postprocessing.
   */
  @PluginApi
  public void redrawScreen() {
    canvas.repaint();
  }

  /**
   * Get if there is a need for this frame to be finalized.
   */
  @PluginApi
  public boolean shouldFinalize() {
    return finalizeAllFrames || snapshotControl.saveSnapshot(bufferedScene, bufferedScene.spp);
  }

  protected Renderer getRenderer() {
    return renderers.getOrDefault(renderer, EMPTY_RENDERER);
  }

  protected Renderer getPreviewRenderer() {
    return previewRenderers.getOrDefault(previewRenderer, EMPTY_RENDERER);
  }

  protected void setRenderer(String id) {
    if (renderers.containsKey(id))
      renderer = id;
    else
      renderer = ChunkyPathTracerID;
  }

  protected void setPreviewRenderer(String id) {
    if (previewRenderers.containsKey(id))
      previewRenderer = id;
    else
      previewRenderer = ChunkyPreviewID;
  }

  private void updateRenderState(Scene scene) {
    finalizeAllFrames = scene.shouldFinalizeBuffer();
    if (mode != scene.getMode()) {
      mode = scene.getMode();
      renderStatusListeners.forEach(listener -> listener.renderStateChanged(mode));
    }
  }

  /**
   * @return the current rendering speed in samples per second (SPS)
   */
  private int samplesPerSecond(int spp) {
    int canvasWidth = bufferedScene.canvasWidth();
    int canvasHeight = bufferedScene.canvasHeight();
    long pixelsPerFrame = (long) canvasWidth * canvasHeight;
    double renderTime = bufferedScene.renderTime / 1000.0;
    return (int) ((spp * pixelsPerFrame) / renderTime);
  }

  private void updateRenderProgress(int spp, long complete, long total) {
    double renderTime = bufferedScene.renderTime / 1000.0;

    // Notify progress listener.
    int target = bufferedScene.getTargetSpp();
    long etaSeconds = (long) (((total - complete) * renderTime) / complete);
    if (etaSeconds > 0) {
      int seconds = (int) ((etaSeconds) % 60);
      int minutes = (int) ((etaSeconds / 60) % 60);
      int hours = (int) (etaSeconds / 3600);
      String eta = String.format("%d:%02d:%02d", hours, minutes, seconds);
      renderTask.update("Rendering", target, spp, eta);
    } else {
      renderTask.update("Rendering", target, spp, "");
    }

    synchronized (this) {
      // Update render status display.
      renderStatusListeners.forEach(listener -> {
        listener.setRenderTime(bufferedScene.renderTime);
        listener.setSamplesPerSecond(samplesPerSecond(spp));
        listener.setSpp(spp);
      });
    }
  }

  /**
   * Finalize the frame if necessary. This uses the internal {@code RenderWorkerPool}.
   */
  protected void finalizeFrame(boolean force) {
    if (force || snapshotControl.saveSnapshot(bufferedScene, bufferedScene.spp)) {
      Chunky.getCommonThreads().submit(() -> {
        if (preview != null) {
          preview.withImageProtected(image -> bufferedScene.getPostProcessingFilter().processFrame(
              preview.getPreview(), image, bufferedScene.getExposure(), TaskTracker.Task.NONE
          ));
        }
        redrawScreen();
      });
    }
  }

  /**
   * Sends scene status text to the render preview tooltip.
   */
  private synchronized void sendSceneStatus(String status) {
    sceneStatusListeners.forEach(listener -> listener.sceneStatus(status));
  }

  @Override
  public Collection<Renderer> getRenderers() {
    return renderers.values();
  }

  @Override
  public Collection<Renderer> getPreviewRenderers() {
    return previewRenderers.values();
  }

  @Override
  public synchronized void addRenderListener(RenderStatusListener listener) {
    renderStatusListeners.add(listener);
  }

  @Override
  public void removeRenderListener(RenderStatusListener listener) {
    renderStatusListeners.remove(listener);
  }

  @Override
  public void setPreview(RenderPreview<?> preview) {
    this.preview = preview;
  }

  @Override
  public RenderPreview<?> getPreview() {
    return preview;
  }

  @Override
  public void setSceneProvider(SceneProvider sceneProvider) {
    this.sceneProvider = sceneProvider;
  }

  @Override
  public void setCanvas(Repaintable canvas) {
    this.canvas = canvas;
  }

  @Override
  public void setOnRenderCompleted(BiConsumer<Long, Integer> listener) {
    renderCompletionListener = listener;
  }

  @Override
  public void setOnFrameCompleted(BiConsumer<Scene, Integer> listener) {
    frameCompletionListener = listener;
  }

  @Override
  public SnapshotControl getSnapshotControl() {
    return this.snapshotControl;
  }

  @Override
  public void setSnapshotControl(SnapshotControl callback) {
    this.snapshotControl = callback;
  }

  @Override
  public void setRenderTask(TaskTracker.Task task) {
    renderTask = task;
  }

  public TaskTracker.Task getRenderTask() {
    return renderTask;
  }

  /**
   * Set CPU load percentage.
   *
   * @param value new load percentage.
   */
  @Override
  public void setCPULoad(int value) {
    this.cpuLoad = value;
    pool.setCpuLoad(value);
  }

  @Override
  public synchronized void addSceneStatusListener(SceneStatusListener listener) {
    sceneStatusListeners.add(listener);
  }

  @Override
  public synchronized void removeSceneStatusListener(SceneStatusListener listener) {
    sceneStatusListeners.remove(listener);
  }

  @Override
  public RenderStatus getRenderStatus() {
    RenderStatus status;
    synchronized (bufferedScene) {
      status = new RenderStatus(bufferedScene.renderTime, bufferedScene.spp);
    }
    return status;
  }

  @Override
  public void withRenderBufferProtected(Consumer<WriteableRenderBuffer> consumer) {
    synchronized (bufferedScene) {
      consumer.accept(bufferedScene.getRenderBuffer());
    }
  }

  @Override
  public void shutdown() {
    pool.interrupt();
    interrupt();
  }

  /**
   * Add a new renderer.
   * Do not use, use {@code Chunky.addRenderer()}.
   */
  public static void addRenderer(Renderer renderer) {
    renderers.put(renderer.getId(), renderer);
  }

  /**
   * Add a new preview renderer.
   * Do not use, use {@code Chunky.addPreviewRenderer()}.
   */
  public static void addPreviewRenderer(Renderer renderer) {
    previewRenderers.put(renderer.getId(), renderer);
  }
}
