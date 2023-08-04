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

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.RiddersSolver;

import java.util.Random;

public abstract class SymmetricSeparableDistributionPixelFilter implements PixelFilter {
  /**
   * Calculate the CDF at the given point. This should be the CDF of the first half of the distribution, scaled to
   * range from 0 to 1.
   * @param x The point to calculate the CDF at. Ranges from 0 to 1.
   */
  protected abstract double cdf(double x);

  private static final double relativeAccuracy = 1e-12;
  private static final double absoluteAccuracy = 1e-8;
  private final LookupTable lookupTable;
  private final double radius;
  protected SymmetricSeparableDistributionPixelFilter(double radius, int resolution) {
    RiddersSolver solver = new RiddersSolver(relativeAccuracy, absoluteAccuracy);

    double[] table = new double[resolution];
    for (int i = 0; i < table.length; i++) {
      double value = i / (double) (table.length - 1);
      UnivariateFunction function = x -> cdf(x) - value;
      table[i] = solver.solve(100, function, 0, 1);
    }

    lookupTable = new LookupTable(table);
    this.radius = radius;
  }

  public double sample(Random random) {
    double value = random.nextDouble() * 2;
    boolean side = value > 1;
    if (side) {
      value = value - 1;
    }

    double sample = lookupTable.sample(value);
    double result;
    if (side) {
      result = 2 - sample;
    } else {
      result = sample;
    }
    return (result - 1) * radius;
  }

  @Override
  public double sampleX(Random random) {
    return sample(random);
  }

  @Override
  public double sampleY(Random random) {
    return sample(random);
  }
}
