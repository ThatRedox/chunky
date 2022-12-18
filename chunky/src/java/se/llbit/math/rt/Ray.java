/*
 * Copyright (c) 2012-2022 Chunky contributors
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

import se.llbit.math.Vector3;

/**
 * The ray representation used for ray tracing.
 *
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class Ray {

  public static final double EPSILON = 0.00000005;

  public static final double OFFSET = 0.000001;

  /**
   * Ray origin.
   */
  public final Vector3 o = new Vector3();

  /**
   * Ray direction.
   */
  public final Vector3 d = new Vector3();

  protected final Vector3 dVal = new Vector3();
  protected final Vector3 invD = new Vector3();

  /**
   * Builds an uninitialized ray.
   */
  public Ray() {
  }

  /**
   * Get the point a certain distance away from this ray.
   */
  public Vector3 at(double distance) {
    Vector3 out = new Vector3(o);
    out.scaleAdd(distance, d);
    return out;
  }

  public Vector3 inverseDirection() {
    if (!dVal.equals(d)) {
      invD.set(1 / d.x, 1 / d.y, 1 / d.z);
      dVal.set(d);
    }
    return invD;
  }
}
