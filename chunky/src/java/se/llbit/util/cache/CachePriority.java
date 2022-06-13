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

package se.llbit.util.cache;

public enum CachePriority {
  /**
   * Lowest priority. First to be evicted.
   */
  LOWEST,
  /**
   * Low priority to be kept.
   */
  LOW,
  /**
   * Normal priority to be kept.
   */
  NORMAL,
  /**
   * High priority to be kept.
   */
  HIGH,
  /**
   * Very high priority to be kept.
   */
  HIGHEST,
  /**
   * Always keep.
   */
  NEVER,
}
