package se.llbit.chunky.model;

import se.llbit.chunky.resources.Texture;
import se.llbit.math.*;
import se.llbit.math.rt.Ray;

public class SlimeBlockModel {
    private static final Quad[] quads = {
            new Quad(
                    new Vector3(13 / 16.0, 13 / 16.0, 3 / 16.0),
                    new Vector3(3 / 16.0, 13 / 16.0, 3 / 16.0),
                    new Vector3(13 / 16.0, 13 / 16.0, 13 / 16.0),
                    new Vector4(3 / 16.0, 13 / 16.0, 3 / 16.0, 13 / 16.0)
            ),
            new Quad(
                    new Vector3(3 / 16.0, 3 / 16.0, 3 / 16.0),
                    new Vector3(13 / 16.0, 3 / 16.0, 3 / 16.0),
                    new Vector3(3 / 16.0, 3 / 16.0, 13 / 16.0),
                    new Vector4(3 / 16.0, 13 / 16.0, 3 / 16.0, 13 / 16.0)
            ),
            new Quad(
                    new Vector3(13 / 16.0, 3 / 16.0, 13 / 16.0),
                    new Vector3(13 / 16.0, 3 / 16.0, 3 / 16.0),
                    new Vector3(13 / 16.0, 13 / 16.0, 13 / 16.0),
                    new Vector4(3 / 16.0, 13 / 16.0, 3 / 16.0, 13 / 16.0)
            ),
            new Quad(
                    new Vector3(3 / 16.0, 3 / 16.0, 3 / 16.0),
                    new Vector3(3 / 16.0, 3 / 16.0, 13 / 16.0),
                    new Vector3(3 / 16.0, 13 / 16.0, 3 / 16.0),
                    new Vector4(3 / 16.0, 13 / 16.0, 3 / 16.0, 13 / 16.0)
            ),
            new Quad(
                    new Vector3(13 / 16.0, 3 / 16.0, 3 / 16.0),
                    new Vector3(3 / 16.0, 3 / 16.0, 3 / 16.0),
                    new Vector3(13 / 16.0, 13 / 16.0, 3 / 16.0),
                    new Vector4(3 / 16.0, 13 / 16.0, 3 / 16.0, 13 / 16.0)
            ),
            new Quad(
                    new Vector3(3 / 16.0, 3 / 16.0, 13 / 16.0),
                    new Vector3(13 / 16.0, 3 / 16.0, 13 / 16.0),
                    new Vector3(3 / 16.0, 13 / 16.0, 13 / 16.0),
                    new Vector4(3 / 16.0, 13 / 16.0, 3 / 16.0, 13 / 16.0)
            ),
            new Quad(
                    new Vector3(16 / 16.0, 16 / 16.0, 0 / 16.0),
                    new Vector3(0 / 16.0, 16 / 16.0, 0 / 16.0),
                    new Vector3(16 / 16.0, 16 / 16.0, 16 / 16.0),
                    new Vector4(0 / 16.0, 16 / 16.0, 0 / 16.0, 16 / 16.0)
            ),
            new Quad(
                    new Vector3(0 / 16.0, 0 / 16.0, 0 / 16.0),
                    new Vector3(16 / 16.0, 0 / 16.0, 0 / 16.0),
                    new Vector3(0 / 16.0, 0 / 16.0, 16 / 16.0),
                    new Vector4(0 / 16.0, 16 / 16.0, 0 / 16.0, 16 / 16.0)
            ),
            new Quad(
                    new Vector3(16 / 16.0, 0 / 16.0, 16 / 16.0),
                    new Vector3(16 / 16.0, 0 / 16.0, 0 / 16.0),
                    new Vector3(16 / 16.0, 16 / 16.0, 16 / 16.0),
                    new Vector4(0 / 16.0, 16 / 16.0, 0 / 16.0, 16 / 16.0)
            ),
            new Quad(
                    new Vector3(0 / 16.0, 0 / 16.0, 0 / 16.0),
                    new Vector3(0 / 16.0, 0 / 16.0, 16 / 16.0),
                    new Vector3(0 / 16.0, 16 / 16.0, 0 / 16.0),
                    new Vector4(0 / 16.0, 16 / 16.0, 0 / 16.0, 16 / 16.0)
            ),
            new Quad(
                    new Vector3(16 / 16.0, 0 / 16.0, 0 / 16.0),
                    new Vector3(0 / 16.0, 0 / 16.0, 0 / 16.0),
                    new Vector3(16 / 16.0, 16 / 16.0, 0 / 16.0),
                    new Vector4(0 / 16.0, 16 / 16.0, 0 / 16.0, 16 / 16.0)
            ),
            new Quad(
                    new Vector3(0 / 16.0, 0 / 16.0, 16 / 16.0),
                    new Vector3(16 / 16.0, 0 / 16.0, 16 / 16.0),
                    new Vector3(0 / 16.0, 16 / 16.0, 16 / 16.0),
                    new Vector4(0 / 16.0, 16 / 16.0, 0 / 16.0, 16 / 16.0)
            ),
    };

    private static final Texture[] tex = {
            Texture.slime, Texture.slime, Texture.slime, Texture.slime, Texture.slime, Texture.slime,

            Texture.slime, Texture.slime, Texture.slime, Texture.slime, Texture.slime, Texture.slime
    };

    public static boolean intersect(Ray ray) {
        ray.t = Double.POSITIVE_INFINITY;
        boolean hit = false;
        Vector4 oldColor = new Vector4(ray.color);
        for (int i = 0; i < 6; ++i) {
            Quad quad = quads[i];
            if (quad.intersect(ray)) {
                float[] color = tex[i].getColor(ray.u, ray.v);
                if (color[3] > Ray.EPSILON) {
                    ColorUtil.overlayColor(ray.color, color);
                    ray.setNormal(quad.n);
                    ray.t = ray.tNext;
                    hit = true;
                }
            }
        }
        boolean innerHit = hit;
        Vector4 innerColor = hit ? new Vector4(ray.color) : null;

        ray.color.set(oldColor);
        hit = false;

        for (int i = 6; i < quads.length; ++i) {
            Quad quad = quads[i];
            if (quad.intersect(ray)) {
                float[] color = tex[i].getColor(ray.u, ray.v);
                if (color[3] > Ray.EPSILON) {
                    ColorUtil.overlayColor(ray.color, color);
                    ray.setNormal(quad.n);
                    ray.t = ray.tNext;
                    hit = true;
                }
            }
        }
        if (hit) {
            ray.distance += ray.t;
            ray.o.scaleAdd(ray.t, ray.d);
            if (innerHit) {
                ColorUtil.overlayColor(ray.color, innerColor);
            }
        }
        return hit;
    }
}
