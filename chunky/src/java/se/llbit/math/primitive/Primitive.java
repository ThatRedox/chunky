/* Copyright (c) 2014 Jesper Öqvist <jesper@llbit.se>
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
package se.llbit.math.primitive;

import se.llbit.math.AABB;
import se.llbit.math.Ray;

/**
 * An intersectable primitive piece of geometry
 *
 * @author Jesper Öqvist <jesper.oqvist@cs.lth.se>
 */
public interface Primitive {

  /**
   * Intersect the ray with this geometry.
   *
   * @return {@code true} if there was an intersection
   */
  boolean intersect(Ray ray);

  /**
   * @return axis-aligned bounding box for the primitive
   */
  AABB bounds();

  /**
   * Pack this primitive for lower memory at the expense of clarity / modifiability.
   *
   * @return A packed primitive.
   */
  default Primitive pack() {
    return this;
  }
}
