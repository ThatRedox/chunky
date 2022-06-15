/* Copyright (c) 2022 Chunky contributors
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

import java.util.Optional;
import java.util.function.Supplier;

public interface Cache {
  /**
   * Get the bytes entry corresponding to a given key. If there is an entry, an Optional
   * wrapping the entry is returned. If not, an empty Optional is returned.
   */
  Optional<byte[]> getBytes(String key);

  /**
   * Get the string entry corresponding to a given key. If there is an entry, an Optional
   * wrapping the entry is returned. If not, an empty Optional is returned.
   */
  default Optional<String> getString(String key) {
    return getBytes(key).map(String::new);
  }

  /**
   * Get the bytes entry corresponding to a given key. If there is no entry, the given
   * loader is called and it's value is entered into the cache and returned.
   */
  default byte[] computeBytesIfAbsent(String key, Supplier<byte[]> loader) {
    Optional<byte[]> entry = getBytes(key);
    if (entry.isPresent()) return entry.get();
    byte[] out = loader.get();
    put(key, out);
    return out;
  }

  /**
   * Get the String entry corresponding to a given key. If there is no entry, the given
   * loader is called and it's value is entered into the cache and returned.
   */
  default String computeStringIfAbsent(String key, Supplier<String> loader) {
    Optional<String> entry = getString(key);
    if (entry.isPresent()) return entry.get();
    String out = loader.get();
    put(key, out);
    return out;
  }

  /**
   * Insert a bytes entry with the given key. If there already is an entry, the new value
   * will overwrite the previous entry.
   */
  void put(String key, byte[] entry);

  /**
   * Insert a String entry with the given key. If there is already an entry, the new value
   * will overwrite the previous entry.
   */
  default void put(String key, String entry) {
    put(key, entry.getBytes());
  }

  /**
   * Invalidate the entry with the given key.
   */
  void invalidate(String key);

  /**
   * Run any maintenence operations if applicable.
   */
  void flush();
}
