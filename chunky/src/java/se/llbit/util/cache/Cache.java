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

import se.llbit.chunky.PersistentSettings;
import se.llbit.chunky.main.Chunky;
import se.llbit.json.JsonMember;
import se.llbit.json.JsonObject;
import se.llbit.json.JsonParser;
import se.llbit.json.PrettyPrinter;
import se.llbit.log.Log;
import se.llbit.util.annotation.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Cache {
  private static final ConcurrentHashMap<String, CacheObject> objects = new ConcurrentHashMap<>();
  private static File cacheDirectory;
  private static final AtomicBoolean needFlush = new AtomicBoolean(false);

  static { init(); }
  private static void init() {
    cacheDirectory = PersistentSettings.cacheDirectory();

    File cacheJson = new File(PersistentSettings.cacheDirectory(), "cache.json");
    if (!cacheJson.exists()) cacheJson = new File(PersistentSettings.cacheDirectory(), "cache.json.backup");
    if (!cacheJson.exists()) return;

    JsonObject obj;
    try (InputStream is = new BufferedInputStream(Files.newInputStream(cacheJson.toPath()));
         JsonParser parser = new JsonParser(is)) {
      obj = parser.parse().asObject();
    } catch (IOException | JsonParser.SyntaxError e) {
      Log.warn("Error reading cache.json", e);
      return;
    }

    for (JsonMember entry : obj.get("objects").asObject()) {
      String key = entry.getName();
      CacheObject.deserialize(key, entry.getValue().asObject())
          .ifPresent(cacheObject -> objects.put(key, cacheObject));
    }
  }

  private static void checkedFlush() {
    if (needFlush.compareAndSet(true, false)) {
      try {
        flush();
      } catch (IOException e) {
        Log.warn("Failed to flush changes to cache: ", e);
      }
    }
  }

  private static void scheduleFlush() {
    needFlush.set(true);
    Chunky.getCommonThreads().submit(Cache::checkedFlush);
  }

  /**
   * Get a cache object from it's key.
   * @param key   Cache object's key.
   * @return      Optional containing the CacheObject if it exists.
   */
  public static Optional<CacheObject> get(String key) {
    return Optional.ofNullable(objects.getOrDefault(key, null));
  }

  /**
   * Remove a cache object.
   * @param key   Cache object's key.
   */
  protected static void remove(String key) {
    objects.remove(key);
    scheduleFlush();
  }

  /**
   * Create a new cache object.
   * @param key       Cache object's key.
   * @param priority  Eviction priority of the object.
   * @param suffix    File name suffix.
   * @return          The created cache object.
   */
  public static Optional<CacheObject> create(String key, CachePriority priority, @Nullable String suffix) {
    try {
      CacheObject obj = new CacheObject(key, priority, suffix, cacheDirectory);
      objects.put(key, obj);
      scheduleFlush();
      return Optional.of(obj);
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  /**
   * Flush any changes to the cache to disk.
   */
  public static void flush() throws IOException {
    JsonObject cacheObjects = new JsonObject();
    for (Map.Entry<String, CacheObject> entry : objects.entrySet()) {
      cacheObjects.add(entry.getKey(), entry.getValue().serialize());
    }

    JsonObject base = new JsonObject();
    base.add("objects", cacheObjects);

    File cacheJson = new File(cacheDirectory, "cache.json");
    File backupJson = new File(cacheDirectory, "cache.json.backup");

    if (cacheJson.exists()) {
      Files.copy(cacheJson.toPath(), backupJson.toPath());
    }

    try (PrettyPrinter printer = new PrettyPrinter("", new PrintStream(
        new BufferedOutputStream(Files.newOutputStream(cacheJson.toPath()))))) {
      base.prettyPrint(printer);
    }
  }
}
