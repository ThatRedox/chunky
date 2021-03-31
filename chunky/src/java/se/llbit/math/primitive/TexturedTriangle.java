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

import se.llbit.chunky.world.Material;
import se.llbit.math.AABB;
import se.llbit.math.Ray;
import se.llbit.math.Vector2;
import se.llbit.math.Vector3;

/**
 * A simple triangle primitive.
 *
 * @author Jesper Öqvist <jesper.oqvist@cs.lth.se>
 */
public class TexturedTriangle implements Primitive {

  private static final double EPSILON = 0.000001;

  /** Note: this is public for some plugins. Stability is not guaranteed. */
  public final Vector3 e1 = new Vector3(0, 0, 0);
  public final Vector3 e2 = new Vector3(0, 0, 0);
  public final Vector3 o = new Vector3(0, 0, 0);
  public final Vector3 n = new Vector3(0, 0, 0);
  public final AABB bounds;
  public final Vector2 t1;
  public final Vector2 t2;
  public final Vector2 t3;
  public final Material material;
  public final boolean doubleSided;

  /**
   * @param c1 first corner
   * @param c2 second corner
   * @param c3 third corner
   */
  public TexturedTriangle(Vector3 c1, Vector3 c2, Vector3 c3, Vector2 t1, Vector2 t2,
      Vector2 t3, Material material) {
    this(c1, c2, c3, t1, t2, t3, material, true);
  }

  /**
   * @param c1 first corner
   * @param c2 second corner
   * @param c3 third corner
   */
  public TexturedTriangle(Vector3 c1, Vector3 c2, Vector3 c3, Vector2 t1, Vector2 t2,
      Vector2 t3, Material material, boolean doubleSided) {
    e1.sub(c2, c1);
    e2.sub(c3, c1);
    o.set(c1);
    n.cross(e2, e1);
    n.normalize();
    this.t1 = new Vector2(t2);
    this.t2 = new Vector2(t3);
    this.t3 = new Vector2(t1);
    this.material = material;
    this.doubleSided = doubleSided;

    bounds = AABB.bounds(c1, c2, c3);
  }

  @Override public boolean intersect(Ray ray) {
    // Möller-Trumbore triangle intersection algorithm!
    Vector3 pvec = new Vector3();
    Vector3 qvec = new Vector3();
    Vector3 tvec = new Vector3();

    pvec.cross(ray.d, e2);
    double det = e1.dot(pvec);
    if (doubleSided) {
      if (det > -EPSILON && det < EPSILON) {
        return false;
      }
    } else if (det > -EPSILON) {
      return false;
    }
    double recip = 1 / det;

    tvec.sub(ray.o, o);

    double u = tvec.dot(pvec) * recip;

    if (u < 0 || u > 1) {
      return false;
    }

    qvec.cross(tvec, e1);

    double v = ray.d.dot(qvec) * recip;

    if (v < 0 || (u + v) > 1) {
      return false;
    }

    double t = e2.dot(qvec) * recip;

    if (t > EPSILON && t < ray.t) {
      double w = 1 - u - v;
      ray.u = t1.x * u + t2.x * v + t3.x * w;
      ray.v = t1.y * u + t2.y * v + t3.y * w;
      float[] color = material.getColor(ray.u, ray.v);
      if (color[3] > 0) {
        ray.color.set(color);
        ray.setCurrentMaterial(material);
        ray.t = t;
        ray.n.set(n);
        return true;
      }
    }
    return false;
  }

  @Override public AABB bounds() {
    return bounds;
  }

  @Override
  public Primitive pack() {
    Vector3 c2 = new Vector3(e1);
    c2.add(o);
    Vector3 c3 = new Vector3(e2);
    c3.add(o);

    return new PackedTexturedTriangle(o, c2, c3, t1, t2, t3, material);
  }

  private static class PackedTexturedTriangle implements Primitive {
    public final float c1x;
    public final float c1y;
    public final float c1z;

    public final float c2x;
    public final float c2y;
    public final float c2z;

    public final float c3x;
    public final float c3y;
    public final float c3z;

    public final float t1x;
    public final float t1y;

    public final float t2x;
    public final float t2y;

    public final float t3x;
    public final float t3y;

    public final Material material;

    public PackedTexturedTriangle(Vector3 c1, Vector3 c2, Vector3 c3, Vector2 t1, Vector2 t2, Vector2 t3, Material material) {
      c1x = (float) c1.x;
      c1y = (float) c1.y;
      c1z = (float) c1.z;

      c2x = (float) c2.x;
      c2y = (float) c2.y;
      c2z = (float) c2.z;

      c3x = (float) c3.x;
      c3y = (float) c3.y;
      c3z = (float) c3.z;

      t1x = (float) t1.x;
      t1y = (float) t1.y;

      t2x = (float) t2.x;
      t2y = (float) t2.y;

      t3x = (float) t3.x;
      t3y = (float) t3.y;

      this.material = material;
    }

    @Override
    public boolean intersect(Ray ray) {
      Vector3 e1 = new Vector3(c2x - c1x, c2y - c1y, c2z - c1z);
      Vector3 e2 = new Vector3(c3x - c1x, c3y - c1y, c3z - c1z);

      // Möller-Trumbore triangle intersection algorithm!
      Vector3 pvec = new Vector3();
      Vector3 qvec = new Vector3();
      Vector3 tvec = new Vector3();

      pvec.cross(ray.d, e2);
      double det = e1.dot(pvec);
      if (det > -EPSILON && det < EPSILON) {
        return false;
      }
      double recip = 1 / det;

      tvec.set(ray.o.x - c1x, ray.o.y - c1y, ray.o.z - c1z);

      double u = tvec.dot(pvec) * recip;

      if (u < 0 || u > 1) {
        return false;
      }

      qvec.cross(tvec, e1);

      double v = ray.d.dot(qvec) * recip;

      if (v < 0 || (u + v) > 1) {
        return false;
      }

      double t = e2.dot(qvec) * recip;

      if (t > EPSILON && t < ray.t) {
        double w = 1 - u - v;
        ray.u = t1x * u + t2x * v + t3x * w;
        ray.v = t1y * u + t2y * v + t3y * w;
        float[] color = material.getColor(ray.u, ray.v);
        if (color[3] > 0) {
          ray.color.set(color);
          ray.setCurrentMaterial(material);
          ray.t = t;
          ray.n.cross(e2, e1);
          ray.n.normalize();
          return true;
        }
      }
      return false;
    }

    @Override
    public AABB bounds() {
      return (new TexturedTriangle(new Vector3(c1x, c1y, c1z), new Vector3(c2x, c2y, c2z), new Vector3(c3x, c3y, c3z),
              new Vector2(t1x, t1y), new Vector2(t2x, t2y), new Vector2(t3x, t3y), material)).bounds();
    }
  }
}
