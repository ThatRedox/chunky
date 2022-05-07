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

import se.llbit.chunky.renderer.postprocessing.PreviewFilter;
import se.llbit.chunky.renderer.scene.Camera;
import se.llbit.chunky.renderer.scene.RayTracer;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderBuffer;
import se.llbit.math.Ray;
import se.llbit.util.TaskTracker;

public class PreviewRenderer extends TileBasedRenderer {
    protected final String id;
    protected final String name;
    protected final String description;
    protected RayTracer tracer;

    public PreviewRenderer(String id, String name, String description, RayTracer tracer) {
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
    public boolean autoPostProcess() {
        return false;
    }

    @Override
    public void render(DefaultRenderManager manager, RenderStatusCallback callback) throws InterruptedException {
        TaskTracker.Task task = manager.getRenderTask();
        task.update("Preview", 2, 0, "");

        RenderPreview<?> preview = manager.getPreview();
        initTiles(preview.getPreview(), RenderBuffer.TILE_SIZE);

        Scene scene = manager.bufferedScene;
        int width = preview.getPreview().getWidth();
        int height = preview.getPreview().getHeight();

        Camera cam = scene.camera();
        double halfWidth = width / (2.0 * height);
        double invHeight = 1.0 / height;

        Ray target = new Ray();
        boolean hit = scene.traceTarget(target);
        int tx = (int) Math.floor(target.o.x + target.d.x * Ray.OFFSET);
        int ty = (int) Math.floor(target.o.y + target.d.y * Ray.OFFSET);
        int tz = (int) Math.floor(target.o.z + target.d.z * Ray.OFFSET);

        renderTiles(manager, preview.getPreview(), state -> {
            if (state.x == width / 2 && (state.y >= height / 2 - 5 && state.y <= height / 2 + 5) ||
                state.y == height / 2 && (state.x >= width / 2 - 5 && state.x <= width / 2 + 5)) {
                state.tile.setPixel(state.x, state.y, 1, 1, 1, 1);
                return true;
            }

            cam.calcViewRay(state.ray, state.random,
                -halfWidth + state.x * invHeight,
                -0.5 + state.y * invHeight);
            scene.rayTrace(tracer, state);

            int rx = (int) Math.floor(state.ray.o.x + state.ray.d.x * Ray.OFFSET);
            int ry = (int) Math.floor(state.ray.o.y + state.ray.d.y * Ray.OFFSET);
            int rz = (int) Math.floor(state.ray.o.z + state.ray.d.z * Ray.OFFSET);
            if (hit && tx == rx && ty == ry && tz == rz) {
                state.ray.color.x = 1 - state.ray.color.x;
                state.ray.color.y = 1 - state.ray.color.y;
                state.ray.color.z = 1 - state.ray.color.z;
                state.ray.color.w = 1;
            }

            state.tile.setPixel(state.x, state.y, state.ray.color.x, state.ray.color.y, state.ray.color.z, 1);
            return true;
        });
        task.update(2, 1);

        preview.withImageProtected(image -> PreviewFilter.INSTANCE
            .processFrame(preview.getPreview(), image, scene.getExposure(), TaskTracker.Task.NONE));
        task.update(2, 2);

        manager.redrawScreen();
    }
}
