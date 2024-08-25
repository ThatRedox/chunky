/*
 * Copyright (c) 2024 Chunky contributors
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

package se.llbit.math;

import java.util.ArrayDeque;
import java.util.Random;

public class AliasTable {
  private final double[] qp;
  private final int[] aliases;

  public AliasTable(double[] weights) {
    double sum = 0.0;
    for (double weight : weights) {
      sum += weight;
    }

    this.qp = new double[weights.length*2];
    this.aliases = new int[weights.length];
    for (int i = 0; i < weights.length; i++) {
      // q = weights[i] / sum
      this.qp[i*2] = weights[i] / sum;
    }

    ArrayDeque<Outcome> under = new ArrayDeque<>();
    ArrayDeque<Outcome> over = new ArrayDeque<>();
    for (int i = 0; i < weights.length; i++) {
      double pHat = this.qp[i*2] * weights.length;
      if (pHat < 1)
        under.push(new Outcome(pHat, i));
      else
        over.push(new Outcome(pHat, i));
    }

    while (!under.isEmpty() && !over.isEmpty()) {
      Outcome un = under.pop();
      Outcome ov = over.pop();

      this.qp[un.index*2 + 1] = un.pHat;
      this.aliases[un.index] = ov.index;

      double excess = un.pHat + ov.pHat - 1;
      if (excess < 1)
        under.push(new Outcome(excess, ov.index));
      else
        over.push(new Outcome(excess, ov.index));
    }

    while (!over.isEmpty()) {
      Outcome ov = over.pop();
      this.qp[ov.index*2 + 1] = 1;
      this.aliases[ov.index] = -1;
    }
    while (!under.isEmpty()) {
      Outcome un = under.pop();
      this.qp[un.index*2 + 1] = 1;
      this.aliases[un.index] = -1;
    }
  }

  public int sample(Random random) {
    int offset = random.nextInt(aliases.length);
    double up = random.nextFloat();

    if (up < qp[offset*2]) {
      return aliases[offset];
    } else {
      return offset;
    }
  }

  private static class Outcome {
    double pHat;
    int index;

    public Outcome(double pHat, int index) {
      this.pHat = pHat;
      this.index = index;
    }
  }
}
