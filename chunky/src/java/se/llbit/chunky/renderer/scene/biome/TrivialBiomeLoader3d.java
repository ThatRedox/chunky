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

package se.llbit.chunky.renderer.scene.biome;

import se.llbit.math.Vector3;
import se.llbit.math.Vector3i;

import java.util.Arrays;

public class TrivialBiomeLoader3d extends BiomeLoader3d {
  protected final BiomeStructure grass;
  protected final BiomeStructure foliage;
  protected final BiomeStructure water;

  protected final Object grassLock = new Object();
  protected final Object foliageLock = new Object();
  protected final Object waterLock = new Object();

  public TrivialBiomeLoader3d(BiomeStructure grass, BiomeStructure foliage, BiomeStructure water) {
    this.grass = grass;
    this.foliage = foliage;
    this.water = water;
  }

  @Override
  public boolean isThreadSafe() {
    return true;
  }

  @Override
  protected void setChunk(int cx, int cy, int cz, Vector3i origin, ChunkAccessor chunkGrass, ChunkAccessor chunkFoliage, ChunkAccessor chunkWater) {
    for (int x = 0; x < 16; x++) {
      for (int y = 0; y < 16; y++) {
        for (int z = 0; z < 16; z++) {
          int wx = cx*16 + x - origin.x;
          int wy = cy*16 + y - origin.y;
          int wz = cz*16 + z - origin.z;

          if (grass.isThreadSafe()) {
            grass.set(wx, wy, wz, Arrays.copyOf(chunkGrass.get(x, y, z), 3));
          } else {
            synchronized (grassLock) {
              grass.set(wx, wy, wz, Arrays.copyOf(chunkGrass.get(x, y, z), 3));
            }
          }

          if (water.isThreadSafe()) {
            water.set(wx, wy, wz, Arrays.copyOf(chunkGrass.get(x, y, z), 3));
          } else {
            synchronized (waterLock) {
              water.set(wx, wy, wz, Arrays.copyOf(chunkGrass.get(x, y, z), 3));
            }
          }

          if (foliage.isThreadSafe()) {
            foliage.set(wx, wy, wz, Arrays.copyOf(chunkGrass.get(x, y, z), 3));
          } else {
            synchronized (foliageLock) {
              foliage.set(wx, wy, wz, Arrays.copyOf(chunkGrass.get(x, y, z), 3));
            }
          }
        }
      }
    }
  }

  @Override
  public BiomeStructure buildGrass() {
    return grass;
  }

  @Override
  public BiomeStructure buildFoliage() {
    return foliage;
  }

  @Override
  public BiomeStructure buildWater() {
    return water;
  }
}
