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

import com.google.gson.Gson;
import se.llbit.json.JsonObject;
import se.llbit.json.JsonParser;
import se.llbit.json.JsonValue;
import se.llbit.json.PrettyPrinter;
import se.llbit.log.Log;
import se.llbit.util.annotation.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class CacheObject implements Comparable<CacheObject> {
  public final String key;
  public final CachePriority priority;
  private final File file;
  private final ReentrantLock lock = new ReentrantLock();
  private boolean visible = true;

  protected CacheObject(String key, CachePriority priority, File file) {
    this.key = key;
    this.priority = priority;
    this.file = file;
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

    return Optional.of(new CacheObject(key, cachePriority, file));
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
  }

  protected JsonObject serialize() {
    JsonObject obj = new JsonObject();
    obj.add("priority", priority.toString());
    obj.add("file", file.toString());
    return obj;
  }

  private void deleteFile() {
    if (!file.delete()) {
      Log.errorf("Failed to delete cache file %s. Attempting to delete on exit.", file);
      file.deleteOnExit();
    }
  }

  /**
   * Delete this object from the cache.
   */
  public void delete() {
    Cache.remove(key);
    visible = false;
    if (lock.tryLock()) {
      try {
        deleteFile();
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * Get the underlying file.
   * @return              File guard with the file. While this guard is not closed, the wrapped file will be valid.
   * @throws IOException  Cache was deleted.
   */
  public FileGuard getFile() throws IOException {
    lock.lock();
    if (!visible)
      throw new IOException("Cache object with key \"" + key + "\" has been deleted.");
    return new FileGuard();
  }

  /**
   * @return Bytes in this cache.
   */
  public byte[] getBytes() throws IOException {
    try (FileGuard guard = getFile()) {
      File file = guard.get();

      // Create an output with a buffer hint of the file size
      ByteArrayOutputStream os = new ByteArrayOutputStream((int) file.length());

      // Copy from input to output with a buffer
      byte[] buffer = new byte[8192];
      try (InputStream is = Files.newInputStream(file.toPath())) {
        int len;
        do {
          len = is.read(buffer);
          os.write(buffer, 0, len);
        } while (len > 0);
      }

      return os.toByteArray();
    }
  }

  /**
   * Set the value of the cache.
   */
  public void setBytes(byte[] bytes) throws IOException {
    try (FileGuard guard = getFile()) {
      try (OutputStream os = Files.newOutputStream(guard.get().toPath())) {
        os.write(bytes);
      }
    }
  }

  /**
   * Get the value of the cache.
   */
  public String getString() throws IOException {
    return new String(getBytes());
  }

  /**
   * Set the value of the cache.
   */
  public void setString(String string) throws IOException {
    setBytes(string.getBytes());
  }

  public class FileGuard implements AutoCloseable {
    private FileGuard() {
      assert CacheObject.this.lock.isHeldByCurrentThread();
    }

    public File get() {
      return CacheObject.this.file;
    }

    public InputStream getInputStream() throws IOException {
      return new BufferedInputStream(Files.newInputStream(get().toPath()));
    }

    public OutputStream getOutputStream() throws IOException {
      return new BufferedOutputStream(Files.newOutputStream(get().toPath()));
    }

    @Override
    public void close() {
      boolean r = file.setLastModified(System.currentTimeMillis());
      assert r;

      if (!CacheObject.this.visible)
        deleteFile();
      CacheObject.this.lock.unlock();
    }
  }

  @Override
  public int compareTo(CacheObject other) {
    int cmp = this.priority.compareTo(other.priority);
    if (cmp != 0) return cmp;

    return Long.compare(this.file.lastModified(), other.file.lastModified());
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
