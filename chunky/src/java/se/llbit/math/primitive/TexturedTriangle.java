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

import org.apache.commons.math3.util.FastMath;
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
  public final double t1u;
  public final double t1v;
  public final double t2u;
  public final double t2v;
  public final double t3u;
  public final double t3v;
  public final Material material;
  public final boolean doubleSided;

  /**
   * Create a double sided textured triangle.
   *
   * @param c1 First corner
   * @param c2 Second corner
   * @param c3 Third corner
   * @param t1 First corner UV
   * @param t2 Second corner UV
   * @param t3 Third corner UV
   * @param material Material
   */
  public TexturedTriangle(Vector3 c1, Vector3 c2, Vector3 c3, Vector2 t1, Vector2 t2,
      Vector2 t3, Material material) {
    this(c1, c2, c3, t1, t2, t3, material, true);
  }

  /**
   * @param c1 First corner
   * @param c2 Second corner
   * @param c3 Third corner
   * @param t1 First corner UV
   * @param t2 Second corner UV
   * @param t3 Third corner UV
   * @param material Material
   * @param doubleSided If this textured triangle should be intersectable from both sides
   */
  public TexturedTriangle(Vector3 c1, Vector3 c2, Vector3 c3, Vector2 t1, Vector2 t2,
      Vector2 t3, Material material, boolean doubleSided) {
    e1.sub(c2, c1);
    e2.sub(c3, c1);
    o.set(c1);
    n.cross(e2, e1);
    n.normalize();
    t1u = t2.x;
    t1v = t2.y;
    t2u = t3.x;
    t2v = t3.y;
    t3u = t1.x;
    t3v = t1.y;
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
      ray.u = t1u * u + t2u * v + t3u * w;
      ray.v = t1v * u + t2v * v + t3v * w;
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

  /**
   * Pack this textured triangle. The resulting {@code Primitive} takes ~1/5th the memory of the whole triangle.
   */
  @Override
  public Primitive pack() {
    Vector3 c2 = new Vector3(e1);
    c2.add(o);
    Vector3 c3 = new Vector3(e2);
    c3.add(o);

    return new PackedTexturedTriangle(o, c2, c3, t1u, t1v, t2u, t2v, t3u, t3v, material);
  }

  /**
   * A packed version of {@Code TexturedTriangle}. This object takes ~1/5th the memory of the whole triangle.
   * Vectors are unwrapped into their components and floats are used where possible.
   */
  private static class PackedTexturedTriangle implements Primitive {
    /** First corner */
    private final float c1x;
    private final float c1y;
    private final float c1z;

    /** Second corner */
    private final float c2x;
    private final float c2y;
    private final float c2z;

    /** Third corner */
    private final float c3x;
    private final float c3y;
    private final float c3z;

    /** First corner UV mapping */
    private final float t1x;
    private final float t1y;

    /** Second corner UV mapping */
    private final float t2x;
    private final float t2y;

    /** Third corner UV mapping */
    private final float t3x;
    private final float t3y;

    private final Material material;

    /**
     * @param c1 First corner
     * @param c2 Second corner
     * @param c3 Third corner
     * @param t1u First corner U
     * @param t1v First corner V
     * @param t2u Second corner U
     * @param t2v Second corner V
     * @param t3u Third corner U
     * @param t3v Third corner V
     * @param material Material
     */
    public PackedTexturedTriangle(Vector3 c1, Vector3 c2, Vector3 c3, double t1u, double t1v, double t2u, double t2v, double t3u, double t3v, Material material) {
      c1x = (float) c1.x;
      c1y = (float) c1.y;
      c1z = (float) c1.z;

      c2x = (float) c2.x;
      c2y = (float) c2.y;
      c2z = (float) c2.z;

      c3x = (float) c3.x;
      c3y = (float) c3.y;
      c3z = (float) c3.z;

      t1x = (float) t1u;
      t1y = (float) t1v;

      t2x = (float) t2u;
      t2y = (float) t2v;

      t3x = (float) t3u;
      t3y = (float) t3v;

      this.material = material;
    }

    /**
     * This is the same algorithm (Möller-Trumbore) as the unpacked triangle except Vectors are unwrapped where possible.
     */
    @Override
    public boolean intersect(Ray ray) {
      // Möller-Trumbore triangle intersection algorithm!
      float e1x = c2x - c1x;
      float e1y = c2y - c1y;
      float e1z = c2z - c1z;

      float e2x = c3x - c1x;
      float e2y = c3y - c1y;
      float e2z = c3z - c1z;

      float px, py, pz;
      float qx, qy, qz;
      float tx, ty, tz;

      // pvec = ray.d × e2
      px = (float) (ray.d.y * e2z - ray.d.z * e2y);
      py = (float) (ray.d.z * e2x - ray.d.x * e2z);
      pz = (float) (ray.d.x * e2y - ray.d.y * e2x);

      // det = e1 · pvec
      float det = e1x * px + e1y * py + e1z * pz;
      if (det > -EPSILON && det < EPSILON) {
        return false;
      }
      float recip = 1 / det;

      // tvec = ray.o - c1;
      tx = (float) (ray.o.x - c1x);
      ty = (float) (ray.o.y - c1y);
      tz = (float) (ray.o.z - c1z);

      // u = (tvec · pvec) / det
      float u = (tx * px + ty * py + tz * pz) * recip;

      if (u < 0 || u > 1) {
        return false;
      }

      // qvec = tvec × e1
      qx = ty * e1z - tz * e1y;
      qy = tz * e1x - tx * e1z;
      qz = tx * e1y - ty * e1x;

      // v = (ray.d · qvec) / det
      float v = (float) (ray.d.x * qx + ray.d.y * qy + ray.d.z * qz) * recip;

      if (v < 0 || (u + v) > 1) {
        return false;
      }

      // t = (e2 · qvec) / det
      float t = (e2x * qx + e2y * qy + e2z * qz) * recip;

      if (t > EPSILON && t < ray.t) {
        double w = 1 - u - v;
        ray.u = t1x * u + t2x * v + t3x * w;
        ray.v = t1y * u + t2y * v + t3y * w;
        float[] color = material.getColor(ray.u, ray.v);
        if (color[3] > 0) {
          ray.color.set(color);
          ray.setCurrentMaterial(material);
          ray.t = t;
          // ray.n = e2 × e1
          ray.n.set(
                  e2y * e1z - e2z * e1y,
                  e2z * e1x - e2x * e1z,
                  e2x * e1y - e2y * e1x
          );
          ray.n.normalize();
          return true;
        }
      }
      return false;
    }

    @Override
    public AABB bounds() {
      return new AABB(min3(c1x, c2x, c3x), max3(c1x, c2x, c3x),
                      min3(c1y, c2y, c3y), max3(c1y, c2y, c3y),
                      min3(c1z, c2z, c3z), max3(c1z, c2z, c3z));
    }

    // Calculate the maximum of 3 floats
    private static float max3(float a, float b, float c) {
      return FastMath.max(a, FastMath.max(b, c));
    }

    // Calculate the minimum of 3 floats
    private static float min3(float a, float b, float c) {
      return FastMath.min(a, FastMath.min(b, c));
    }
  }
}
