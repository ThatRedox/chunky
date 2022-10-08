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

public interface WriteableRenderTile extends RenderTile, AutoCloseable {
  /**
   * Set a pixel color.
   * @param x Buffer x
   * @param y Buffer y
   * @param r Red component
   * @param g Green component
   * @param b Blue component
   * @param s Number of samples
   */
  void setColor(int x, int y, double r, double g, double b, int s);

  /**
   * Commit the changes made to this tile.
   */
  @Override
  void close();

  /**
   * Merge samples into a pixel.
   * @param x Screen x
   * @param y Screen y
   * @param r Red component
   * @param g Green component
   * @param b Blue component
   * @param s Number of samples
   */
  default void mergeColor(int x, int y, double r, double g, double b, int s) {
    Vector3 color = new Vector3();
    int samples = getColor(x, y, color);
    double sinv = 1.0 / (samples + s);
    setColor(x, y,
      (r + color.x) * sinv,
      (g + color.y) * sinv,
      (b + color.z) * sinv,
      samples + s
    );
  }

  /**
   * Merge samples into a pixel.
   * @param x     Screen x
   * @param y     Screen y
   * @param color Color
   * @param s     Number of samples
   */
  default void mergeColor(int x, int y, Vector3 color, int s) {
    mergeColor(x, y, color.x, color.y, color.z, s);
  }
}
