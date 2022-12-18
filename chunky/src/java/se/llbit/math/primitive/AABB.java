/*
 * Copyright (c) 2013-2022 Chunky contributors
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

import se.llbit.math.Vector3;
import se.llbit.math.rt.IntersectionRecord;
import se.llbit.math.rt.Ray;
import se.llbit.util.annotation.Nullable;

import java.util.Random;

/**
 * Axis Aligned Bounding Box for collision detection and Bounding Volume Hierarchy
 * construction.
 *
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class AABB implements Primitive {

  public double xmin;
  public double xmax;
  public double ymin;
  public double ymax;
  public double zmin;
  public double zmax;

  public double surfaceArea;

  public AABB(double xmin, double xmax, double ymin, double ymax, double zmin, double zmax) {
    this.xmin = xmin;
    this.xmax = xmax;
    this.ymin = ymin;
    this.ymax = ymax;
    this.zmin = zmin;
    this.zmax = zmax;

    double x = xmax - xmin;
    double y = ymax - ymin;
    double z = zmax - zmin;
    this.surfaceArea = 2 * (y * z + x * z + x * y);
  }

  @Override
  public AABB bounds() {
    return this;
  }

  @Nullable
  @Override
  public IntersectionRecord closestIntersection(Ray ray, double limit) {
    Vector3 invDir = ray.inverseDirection();

    double t1x = (xmin - ray.o.x) * invDir.x;
    double t1y = (ymin - ray.o.y) * invDir.y;
    double t1z = (zmin - ray.o.z) * invDir.z;

    double t2x = (xmax - ray.o.x) * invDir.x;
    double t2y = (ymax - ray.o.y) * invDir.y;
    double t2z = (zmax - ray.o.z) * invDir.z;

    double tmin = Math.max(Math.max(Math.min(t1x, t2x), Math.min(t1y, t2y)), Math.min(t1z, t2z));
    double tmax = Math.min(Math.min(Math.max(t1x, t2x), Math.max(t1y, t2y)), Math.max(t1z, t2z));

    if (tmax < tmin | tmin > limit) {
      return null;
    }

    Vector3 p = ray.at(tmin);
    IntersectionRecord record = new IntersectionRecord(tmin, null);
    record.normal.set(0, 1, 0);
    double dx = xmax - xmin, dy = ymax - ymin, dz = zmax - zmin;
    if (t1x == tmin) {
      record.texcoord.set(1 - (p.z - zmin) * dz, (p.y - ymin) * dy);
      record.normal.set(-1, 0, 0);
    }
    if (t2x == tmin) {
        record.texcoord.set((p.z - zmin) * dz, (p.y - ymin) * dy);
        record.normal.set(1, 0, 0);
    }
    if (t1y == tmin) {
        record.texcoord.set((p.x - xmin) * dx, 1 - (p.z - zmin) * dz);
        record.normal.set(0, -1, 0);
    }
    if (t2y == tmin) {
        record.texcoord.set((p.x - xmin) * dx, (p.z - zmin) * dz);
        record.normal.set(0, 1, 0);
    }
    if (t1z == tmin) {
        record.texcoord.set((p.x - xmin) * dx, (p.y - ymin) * dy);
        record.normal.set(0, 0, -1);
    }
    if (t2z == tmin) {
        record.texcoord.set(1 - (p.x - xmin) * dx, (p.y - ymin) * dy);
        record.normal.set(0, 0, 1);
    }
    return record;
  }

  @Override
  public double quickIntersection(Ray ray, double limit) {
    Vector3 invDir = ray.inverseDirection();

    double t1x = (xmin - ray.o.x) * invDir.x;
    double t1y = (ymin - ray.o.y) * invDir.y;
    double t1z = (zmin - ray.o.z) * invDir.z;

    double t2x = (xmax - ray.o.x) * invDir.x;
    double t2y = (ymax - ray.o.y) * invDir.y;
    double t2z = (zmax - ray.o.z) * invDir.z;

    double tmin = Math.max(Math.max(Math.min(t1x, t2x), Math.min(t1y, t2y)), Math.min(t1z, t2z));
    double tmax = Math.min(Math.min(Math.max(t1x, t2x), Math.max(t1y, t2y)), Math.max(t1z, t2z));

    return (tmax < tmin | tmin > limit) ? Double.NaN : tmin;
  }

  public void sampleFace(int face, Vector3 loc, Random rand) {
    double[] v = new double[3];
    face %= 6;
    int axis = face % 3;
    v[axis] = face > 2 ? 1 : 0;
    v[(axis + 1) % 3] = rand.nextDouble();
    v[(axis + 2) % 3] = rand.nextDouble();

    v[0] *= xmax - xmin;
    v[1] *= ymax - ymin;
    v[2] *= zmax - zmin;

    v[0] += xmin;
    v[1] += ymin;
    v[2] += zmin;

    loc.set(v[0], v[1], v[2]);
  }

  public double faceSurfaceArea(int face) {
    double[] minC = new double[3];
    double[] maxC = new double[3];

    minC[0] = xmin;
    minC[1] = ymin;
    minC[2] = zmin;

    maxC[0] = xmax;
    maxC[1] = ymax;
    maxC[2] = zmax;

    int a1 = (face + 1) % 3;
    int a2 = (face + 2) % 3;

    double sa = (maxC[a1] - minC[a1]) * (maxC[a2] - minC[a2]);

    return Math.abs(sa);
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
   * @return AABB rotated about the Y axis
   */
  public AABB getYRotated() {
    return new AABB(1 - zmax, 1 - zmin, ymin, ymax, xmin, xmax);
  }

  /**
   * @param x X translation
   * @param y Y translation
   * @param z Z translation
   * @return A translated copy of this AABB
   */
  public AABB getTranslated(double x, double y, double z) {
    return new AABB(xmin + x, xmax + x, ymin + y, ymax + y, zmin + z, zmax + z);
  }

  /**
   * @return an AABB which encloses all given vertices
   */
  public static AABB bounds(Vector3... c) {
    double xmin = Double.POSITIVE_INFINITY, xmax = Double.NEGATIVE_INFINITY,
        ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY,
        zmin = Double.POSITIVE_INFINITY, zmax = Double.NEGATIVE_INFINITY;
    for (Vector3 v : c) {
      if (v.x < xmin) {
        xmin = v.x;
      }
      if (v.x > xmax) {
        xmax = v.x;
      }
      if (v.y < ymin) {
        ymin = v.y;
      }
      if (v.y > ymax) {
        ymax = v.y;
      }
      if (v.z < zmin) {
        zmin = v.z;
      }
      if (v.z > zmax) {
        zmax = v.z;
      }
    }
    return new AABB(xmin, xmax, ymin, ymax, zmin, zmax);
  }

  public AABB expand(AABB bb) {
    return new AABB(Math.min(xmin, bb.xmin), Math.max(xmax, bb.xmax), Math.min(ymin, bb.ymin),
        Math.max(ymax, bb.ymax), Math.min(zmin, bb.zmin), Math.max(zmax, bb.zmax));
  }
}
