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

import se.llbit.json.JsonObject;
import se.llbit.log.Log;
import se.llbit.util.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;

public class CacheObject implements Comparable<CacheObject> {
  public final String key;
  public final CachePriority priority;
  private long lastAccessed;
  private final File file;

  protected CacheObject(String key, CachePriority priority, File file, long lastAccessed) {
    this.key = key;
    this.priority = priority;
    this.file = file;
    this.lastAccessed = lastAccessed;
  }

  protected static Optional<CacheObject> deserialize(String key, JsonObject obj) {
    String priority = obj.get("priority").asString(null);
    if (priority == null) return Optional.empty();
    CachePriority cachePriority;
    try {
      cachePriority = CachePriority.valueOf(priority);
    } catch (IllegalArgumentException e) {
      cachePriority = CachePriority.LOWEST;
    }

    String filename = obj.get("file").asString(null);
    if (filename == null) return Optional.empty();
    File file = new File(filename);
    if (!file.exists()) return Optional.empty();

    long lastAccessed = obj.get("lastAccessed").asLong(0);
    if (lastAccessed == 0) return Optional.empty();

    return Optional.of(new CacheObject(key, cachePriority, file, lastAccessed));
  }

  protected CacheObject(String key, CachePriority priority, @Nullable String suffix, File baseDir) throws IOException {
    File file;
    do {
      String name = randomName(8);
      if (suffix != null) name += suffix;
      file = new File(baseDir, name);
    } while (!file.createNewFile());

    this.key = key;
    this.priority = priority;
    this.file = file;
    this.lastAccessed = System.currentTimeMillis();
  }

  protected JsonObject serialize() {
    JsonObject obj = new JsonObject();
    obj.add("priority", priority.toString());
    obj.add("file", file.toString());
    obj.add("lastAccessed", lastAccessed != 0 ? lastAccessed : 1);
    return obj;
  }

  protected void delete() {
    if (!file.delete()) {
      Log.infof("Failed to delete cache file %s. Attempting to delete on exit.", file);
      file.deleteOnExit();
    }
  }

  public File get() {
    lastAccessed = System.currentTimeMillis();
    return file;
  }

  @Override
  public int compareTo(CacheObject other) {
    int cmp = this.priority.compareTo(other.priority);
    if (cmp != 0) return cmp;

    return Long.compare(this.lastAccessed, other.lastAccessed);
  }

  private static final Random NAME_RANDOM = new Random();
  private static final String NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456790_";
  public static String randomName(int length) {
    StringBuilder sb = new StringBuilder(Math.max(0, length));
    synchronized (NAME_RANDOM) {
      for (int i = 0; i < length; i++) {
        int index = NAME_RANDOM.nextInt(NAME_CHARS.length());
        sb.append(NAME_CHARS.charAt(index));
      }
    }
    return sb.toString();
  }
}
