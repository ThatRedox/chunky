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

package se.llbit.math.rt;

import se.llbit.chunky.world.Material;
import se.llbit.math.Vector2;
import se.llbit.math.Vector3;

public class IntersectionRecord {
  public double distance;
  public Material material;

  public final Vector3 normal;
  public final Vector2 texcoord;

  public IntersectionRecord(double distance, Material material) {
    this.distance = distance;
    this.material = material;
    this.normal = new Vector3();
    this.texcoord = new Vector2();
  }

  public IntersectionRecord(double distance, Material material, Vector3 normal, double u, double v) {
    this(distance, material);
    this.normal.set(normal);
    this.texcoord.set(u, v);
  }
}
