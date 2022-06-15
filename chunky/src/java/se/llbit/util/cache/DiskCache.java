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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import se.llbit.chunky.PersistentSettings;
import se.llbit.chunky.main.Chunky;
import se.llbit.log.Log;
import se.llbit.util.gson.FileSerializer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class DiskCache implements Cache {
  private static final Gson GSON = new GsonBuilder()
    .disableJdkUnsafe()
    .registerTypeAdapter(File.class, new FileSerializer())
    .create();

  public static final DiskCache INSTANCE = new DiskCache(PersistentSettings.cacheDirectory());

  private final HashMap<String, File> entryMap = new HashMap<>();
  private final ReentrantLock lock = new ReentrantLock();
  private final AtomicBoolean dirty = new AtomicBoolean();
  private final File cacheDirectory;
  private final File cacheFile;
  private final File cacheBackup;

  public DiskCache(File cacheDirectory) {
    this.cacheDirectory = cacheDirectory;
    this.cacheFile = new File(cacheDirectory, "cache.json");
    this.cacheBackup = new File(cacheDirectory, "cache.json.backup");

    HashMap<String, File> diskMap = null;
    Type type = new TypeToken<HashMap<String, File>>() {}.getType();
    try (BufferedReader reader = Files.newBufferedReader(cacheFile.toPath())) {
      diskMap = GSON.fromJson(reader, type);
    } catch (IOException | JsonSyntaxException ignored) {
    }

    if (diskMap == null) {
      try (BufferedReader reader = Files.newBufferedReader(cacheBackup.toPath())) {
        diskMap = GSON.fromJson(reader, type);
      } catch (IOException | JsonSyntaxException ignored) {
      }
    }

    if (diskMap != null) {
      entryMap.putAll(diskMap);
    }
  }

  @Override
  public Optional<byte[]> getBytes(String key) {
    lock.lock();
    try {
      File entry = entryMap.get(key);
      if (entry == null) return Optional.empty();

      boolean r = entry.setLastModified(System.currentTimeMillis());
      assert r;

      // Create an output stream with a buffer hint of the file size
      ByteArrayOutputStream os = new ByteArrayOutputStream((int) entry.length());

      // Copy from the file to the output stream with a buffer
      byte[] buffer = new byte[8192];
      try (InputStream is = Files.newInputStream(entry.toPath())) {
        int len;
        do {
          len = is.read(buffer);
          if (len > 0)
            os.write(buffer, 0, len);
        } while (len > 0);
      } catch (IOException e) {
        Log.info("Failed to read cache object: ", e);
        return Optional.empty();
      }

      return Optional.of(os.toByteArray());
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void put(String key, byte[] entry) {
    lock.lock();
    dirty.set(true);
    try {
      File entryFile = entryMap.get(key);
      if (entryFile == null) {
        entryFile = new File(cacheDirectory, randomName(8));
      }

      try (OutputStream os = Files.newOutputStream(entryFile.toPath())) {
        os.write(entry);
        entryMap.put(key, entryFile);
      } catch (IOException e) {
        Log.info("Failed to write cache object: ", e);
      }
      scheduleFlush();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void invalidate(String key) {
    lock.lock();
    dirty.set(true);
    try {
      File entry = entryMap.remove(key);
      if (entry != null) {
        if (!entry.delete()) {
          Log.warnf("Failed to delete cache object: %s", key);
          entry.deleteOnExit();
        }
      }
      scheduleFlush();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void flush() {
    lock.lock();
    dirty.set(false);
    try {
      if (cacheFile.exists()) {
        Files.copy(cacheFile.toPath(), cacheBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
      try (BufferedWriter writer = Files.newBufferedWriter(cacheFile.toPath())) {
        writer.write(GSON.toJson(this.entryMap));
      }
    } catch (IOException e) {
      Log.warn("Failed to flush cache to disk: ", e);
    } finally {
      lock.unlock();
    }
  }

  protected void scheduleFlush() {
    Chunky.getCommonThreads().submit(this::checkedFlush);
  }

  private void checkedFlush() {
    lock.lock();
    try {
      if (dirty.compareAndSet(true, false)) {
        flush();
      }
    } finally {
      lock.unlock();
    }
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
