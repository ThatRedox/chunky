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

import org.apache.commons.math3.util.FastMath;
import se.llbit.chunky.renderer.scene.renderbuffer.RenderTile;
import se.llbit.chunky.renderer.scene.renderbuffer.WriteableRenderBuffer;
import se.llbit.chunky.renderer.scene.renderbuffer.WriteableRenderTile;
import se.llbit.math.Ray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A tile based renderer. Simply call {@code submitTiles} to submit a frame's worth of tiles to the work queue.
 * Call {@code manager.pool.awaitEmpty()} to block until all tiles are finished rendering.
 * Call {@code postRender.getAsBoolean()} after each frame (and terminate if it returns {@code true}).
 *
 * Implementation detail: Tiles are cached for faster rendering.
 */
public abstract class TileBasedRenderer implements Renderer {
    protected BooleanSupplier postRender = () -> true;

    private final ArrayList<Tile> tiles = new ArrayList<>();
    private ArrayBlockingQueue<Tile> tilesQueue = null;

    protected static class Tile {
        public long sampleCount = 0;
        public boolean complete = false;
        public int x0, x1;
        public int y0, y1;

        public Tile(int x0, int x1, int y0, int y1) {
            this.x0 = x0;
            this.x1 = x1;
            this.y0 = y0;
            this.y1 = y1;
        }

        public Future<? extends WriteableRenderTile> getTile(WriteableRenderBuffer buffer) {
            return buffer.getTile(this.x0, this.y0, this.x1-this.x0, this.y1-this.y0);
        }
    }

    @Override
    public void setPostRender(BooleanSupplier callback) {
        postRender = callback;
    }

    @Override
    public void render(DefaultRenderManager manager) throws InterruptedException {
        tiles.clear();
        int width = manager.bufferedScene.getRenderBuffer().getWidth();
        int height = manager.bufferedScene.getRenderBuffer().getHeight();
        int tileWidth = manager.context.tileWidth();

        for (int i = 0; i < width; i += tileWidth) {
            for (int j = 0; j < height; j += tileWidth) {
                tiles.add(new Tile(i, FastMath.min(i + tileWidth, width),
                    j, FastMath.min(j + tileWidth, height)));
            }
        }

        doRender(manager);
    }

    public abstract void doRender(DefaultRenderManager manager) throws InterruptedException;

    /**
     * Create and submit tiles to the rendering pool.
     * Await for these tiles to finish rendering with {@code manager.pool.awaitEmpty()}.
     *
     * @param perPixel This is called on every pixel. The first argument is the worker state.
     *                 The second argument is the current pixel (x, y). Return {@code true} if this pixel has
     *                 been rendered to completion.
     * @return Total samples rendered so far.
     */
    protected long renderTiles(DefaultRenderManager manager, Predicate<WorkerState> perPixel) {
        if (tilesQueue != null) {
            tilesQueue.clear();
        }
        if (tilesQueue == null || tilesQueue.remainingCapacity() < tiles.size()) {
            tilesQueue = new ArrayBlockingQueue<>(tiles.size());
        }
        List<Tile> tilesList = tiles.stream()
            .filter(t -> !t.complete)
            .collect(Collectors.toList());
        Collections.shuffle(tilesList);
        tilesQueue.addAll(tilesList);

        AtomicLong samplesCount = new AtomicLong(
            tiles.stream()
                .filter(t -> t.complete)
                .mapToLong(t -> t.sampleCount)
                .sum()
        );

        IntStream.range(0, manager.pool.threads).mapToObj(i -> manager.pool.submit(worker -> {
            Tile nextTile = tilesQueue.poll();
            if (nextTile == null) return;
            Future<? extends RenderTile> tileFuture = nextTile.getTile(manager.bufferedScene.getRenderBuffer());
            WorkerState state = new WorkerState();
            state.ray = new Ray();
            state.ray.setNormal(0, 0, -1);
            state.random = worker.random;

            while (tileFuture != null) {
                Tile managerTile = nextTile;
                RenderTile tile = tileFuture.get();

                nextTile = tilesQueue.poll();
                tileFuture = nextTile == null ? null : nextTile.getTile(manager.bufferedScene.getRenderBuffer());
                state.tile = (WriteableRenderTile) tile;

                do {
                    boolean complete = true;
                    for (int x = 0; x < tile.getTileWidth(); x++) {
                        for (int y = 0; y < tile.getTileHeight(); y++) {
                            state.x = tile.getBufferX(x);
                            state.y = tile.getBufferY(y);

                            complete &= perPixel.test(state);
                        }
                    }
                    managerTile.complete = complete;
                    worker.workSleep();
                } while (!managerTile.complete && tileFuture != null && !tileFuture.isDone());

                long sum = 0;
                for (int x = 0; x < tile.getTileWidth(); x++) {
                    for (int y = 0; y < tile.getTileHeight(); y++) {
                        sum += tile.getColor(
                            tile.getBufferX(x),
                            tile.getBufferY(y),
                            null
                        );
                    }
                }
                if (managerTile.complete) {
                    managerTile.sampleCount = sum;
                }
                samplesCount.addAndGet(sum);
            }
        })).collect(Collectors.toList()).forEach(j -> {
            try {
                j.awaitFinish();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        return samplesCount.get();
    }

    /**
     * @return {@code true} if all the tiles are done rendering.
     */
    protected boolean isComplete() {
        return tiles.stream().allMatch(t -> t.complete);
    }
}
