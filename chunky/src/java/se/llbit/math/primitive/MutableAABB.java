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

import se.llbit.math.rt.IntersectionRecord;
import se.llbit.math.rt.Ray;
import se.llbit.math.Vector3;
import se.llbit.util.annotation.Nullable;

/**
 * Axis-Aligned Bounding Box. Does not compute intersection normals.
 *
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class MutableAABB implements Primitive {
  public double xmin;
  public double xmax;
  public double ymin;
  public double ymax;
  public double zmin;
  public double zmax;

  /**
   * Construct a new AABB with given bounds.
   */
  public MutableAABB(double xmin, double xmax, double ymin, double ymax, double zmin, double zmax) {
    this.xmin = xmin;
    this.xmax = xmax;
    this.ymin = ymin;
    this.ymax = ymax;
    this.zmin = zmin;
    this.zmax = zmax;
  }

  /**
   * Expand this AABB to enclose the given AABB.
   */
  public void expand(AABB p) {
    xmin = Math.max(p.xmin, xmin);
    ymin = Math.max(p.ymin, ymin);
    zmin = Math.max(p.zmin, zmin);
    xmax = Math.max(p.xmax, xmax);
    ymax = Math.max(p.ymax, ymax);
    zmax = Math.max(p.zmax, zmax);
  }

  @Override
  public AABB bounds() {
    return new AABB(xmin, xmax, ymin, ymax, zmin, zmax);
  }

  @Override
  public String toString() {
    return String.format("[ %.2f, %.2f, %.2f, %.2f, %.2f, %.2f]", xmin, xmax, ymin, ymax, zmin, zmax);
  }

  /**
   * Test if point is inside the bounding box.
   *
   * @return true if p is inside this BB.
   */
  public boolean inside(Vector3 p) {
    return (p.x >= xmin && p.x <= xmax) &&
        (p.y >= ymin && p.y <= ymax) &&
        (p.z >= zmin && p.z <= zmax);
  }

  /**
   * This is used in BVH construction heuristic.
   * @return surface area of the bounding box
   */
  public double surfaceArea() {
    double x = xmax - xmin;
    double y = ymax - ymin;
    double z = zmax - zmin;
    return 2 * (y * z + x * z + x * y);
  }

  @Nullable
  @Override
  public IntersectionRecord closestIntersection(Ray ray, double limit) {
    // TODO: This dosen't seem to be used?
    return bounds().closestIntersection(ray, limit);
  }
}
