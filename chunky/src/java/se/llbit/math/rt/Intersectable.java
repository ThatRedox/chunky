/*
 * Copyright (c) 2014-2022 Chunky contributors
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

package se.llbit.math.rt;

import se.llbit.util.annotation.Nullable;

/**
 * Anything which can intersect a ray in space.
 */
public interface Intersectable {
  /**
   * Find the closest intersection between the ray and this.
   *
   * @param ray Ray to intersect
   * @param limit Maximum intersection distance
   * @return {@code IntersectionRecord} if there exists any intersection. {@code null} otherwise.
   */
  @Nullable IntersectionRecord closestIntersection(Ray ray, double limit);

  /**
   * Find the distance to the closest intersection between the ray and this.
   *
   * @return The distance if there exists any intersection. {@code Double.NaN} otherwise.
   */
  default double quickIntersection(Ray ray, double limit) {
    IntersectionRecord record = closestIntersection(ray, limit);
    if (record == null)
      return Double.NaN;
    return record.distance;
  }
}
