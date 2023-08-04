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

import java.util.Random;

public class BlackmanHarrisPixelFilter extends SymmetricSeparableDistributionPixelFilter {
  public static final BlackmanHarrisPixelFilter DEFAULT = new BlackmanHarrisPixelFilter(1.5, 1024);

  protected BlackmanHarrisPixelFilter(double radius, int resolution) {
    super(radius, resolution);
  }

  @Override
  protected double cdf(double x) {
    // The Blackman-Harris window is defined on [0, 1] as:
    // 0.35875 - 0.48829 * cos(2pi x) + 0.14128 * cos(4pi x) - 0.01168 * cos(6pi x)
    // The CDF can be determined to be:
    // 0.35875 x - 0.0777138 * sin(2pi x) + 0.0112427 sin(4pi x) - 0.000619643 sin(6pi x)
    // Then we can take the first half, normalize, and scale it.
    return x - 0.216624 * Math.sin(Math.PI * x)
             + 0.0313385 * Math.sin(2 * Math.PI * x)
             - 0.00172723 * Math.sin(3 * Math.PI * x);
  }

  @Override
  public double weight(double x, double y) {
    return 1.0;
  }
}
