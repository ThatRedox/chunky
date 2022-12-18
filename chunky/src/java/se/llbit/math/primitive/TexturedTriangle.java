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

package se.llbit.math.primitive;

import se.llbit.chunky.world.Material;
import se.llbit.math.rt.IntersectionRecord;
import se.llbit.math.rt.Ray;
import se.llbit.math.Vector2;
import se.llbit.math.Vector3;
import se.llbit.util.annotation.Nullable;

public class TexturedTriangle extends Triangle {

  public final Material material;


  public TexturedTriangle(Vector3 c1, Vector3 c2, Vector3 c3, Vector2 t1, Vector2 t2, Vector2 t3,
                          boolean doubleSided, Material material) {
    super(c1, c2, c3, t1, t2, t3, doubleSided);
    this.material = material;
  }

  @Nullable
  @Override
  public IntersectionRecord closestIntersection(Ray ray, double limit) {
    IntersectionRecord record = super.closestIntersection(ray, limit);
    if (record != null) {
      record.material = this.material;
    }
    return record;
  }
}
