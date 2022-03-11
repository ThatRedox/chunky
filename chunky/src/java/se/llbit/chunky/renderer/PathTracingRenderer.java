/* Copyright (c) 2021-2022 Chunky contributors
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

import se.llbit.chunky.renderer.scene.Camera;
import se.llbit.chunky.renderer.scene.RayTracer;
import se.llbit.chunky.renderer.scene.Scene;

import java.util.concurrent.atomic.AtomicLong;

public class PathTracingRenderer extends TileBasedRenderer {
  protected final String id;
  protected final String name;
  protected final String description;
  protected RayTracer tracer;

  public PathTracingRenderer(String id, String name, String description, RayTracer tracer) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.tracer = tracer;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void doRender(DefaultRenderManager manager) throws InterruptedException {
    Scene scene = manager.bufferedScene;
    int width = scene.width;
    int height = scene.height;

    int sppPerPass = manager.context.sppPerPass();
    Camera cam = scene.camera();
    double halfWidth = width / (2.0 * height);
    double invHeight = 1.0 / height;

    do {
      long samples = renderTiles(manager, state -> {
        double sr = 0;
        double sg = 0;
        double sb = 0;
        int s = Math.min(sppPerPass, scene.getTargetSpp() - state.tile.getColor(state.x, state.y, null));

        for (int k = 0; k < s; k++) {
          double ox = state.random.nextDouble();
          double oy = state.random.nextDouble();

          cam.calcViewRay(state.ray, state.random,
              -halfWidth + (state.x + ox) * invHeight,
              -0.5 + (state.y + oy) * invHeight);
          scene.rayTrace(tracer, state);

          sr += state.ray.color.x;
          sg += state.ray.color.y;
          sb += state.ray.color.z;
        }

        state.tile.mergeColor(state.x, state.y, sr, sg, sb, s);

        return state.tile.getColor(state.x, state.y, null) >= scene.getTargetSpp();
      });

      scene.spp = Math.toIntExact(samples / ((long) scene.width * scene.height));
    } while (!postRender.getAsBoolean() && !isComplete());
  }
}
