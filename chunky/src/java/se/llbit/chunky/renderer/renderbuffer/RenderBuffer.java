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

package se.llbit.chunky.renderer.renderbuffer;

import se.llbit.util.Registerable;

import java.util.concurrent.Future;

public interface RenderBuffer extends Registerable {
  /**
   * The preferred tile size.
   */
  int TILE_SIZE = 128;

  /**
   * Get a tile (potentially) asynchronously.
   *
   * @param x         Tile start x
   * @param y         Tile start y
   * @param width     Tile width
   * @param height    Tile height
   * @return Future which will resolve to the tile
   */
  Future<? extends RenderTile> getTile(int x, int y, int width, int height);

  /**
   * Get the width of this buffer.
   */
  int getWidth();

  /**
   * Get the height of this buffer.
   */
  int getHeight();
}
