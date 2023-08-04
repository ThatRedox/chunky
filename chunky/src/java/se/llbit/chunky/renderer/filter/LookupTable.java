/*
 * Copyright (c) 2023 Chunky contributors
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

package se.llbit.chunky.renderer.filter;

import se.llbit.math.QuickMath;

public class LookupTable {
  protected final double[] table;

  public LookupTable(double[] table) {
    this.table = table;
  }

  /**
   * Sample the table at the given value.
   *
   * @param value Value to sample at, in the range 0 to 1.
   * @return The linearly interpolated value of the table at the given value.
   */
  public double sample(double value) {
    double pos = value * (table.length - 1);
    int indexLow = QuickMath.clamp((int) Math.floor(pos), 0, table.length - 1);
    int indexHigh = indexLow + 1;
    if (indexHigh >= table.length) {
      return table[indexLow];
    }

    double dxl = pos - indexLow;
    double dxh = indexHigh - pos;
    return table[indexLow] * dxh + table[indexHigh] * dxl;
  }
}
