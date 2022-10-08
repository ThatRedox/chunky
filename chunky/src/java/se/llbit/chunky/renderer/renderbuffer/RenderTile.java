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

import se.llbit.math.Vector3;
import se.llbit.util.annotation.Nullable;

public interface RenderTile {
  /**
   * Get the color.
   * @param x     Screen x
   * @param y     Screen y
   * @param color Color is returned in this Vector
   * @return Number of samples
   */
  int getColor(int x, int y, @Nullable Vector3 color);

  /**
   * Get the screen-space X origin.
   */
  int getTileX();

  /**
   * Get the screen-space Y origin.
   */
  int getTileY();

  /**
   * Get the width of the tile.
   */
  int getTileWidth();

  /**
   * Get the height of the tile.
   */
  int getTileHeight();
}
