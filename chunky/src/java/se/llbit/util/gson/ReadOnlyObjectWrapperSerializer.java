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

package se.llbit.util.gson;

import com.google.gson.*;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.lang.reflect.Type;

public class ReadOnlyObjectWrapperSerializer<T> implements JsonSerializer<ReadOnlyObjectWrapper<T>>, JsonDeserializer<ReadOnlyObjectWrapper<T>> {
  private final Class<T> typeOfT;

  public ReadOnlyObjectWrapperSerializer(Class<T> typeOfT) {
    this.typeOfT = typeOfT;
  }

  @Override
  public ReadOnlyObjectWrapper<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    T obj = context.deserialize(json, this.typeOfT);
    return new ReadOnlyObjectWrapper<>(obj);
  }

  @Override
  public JsonElement serialize(ReadOnlyObjectWrapper<T> src, Type typeOfSrc, JsonSerializationContext context) {
    return context.serialize(src.get(), typeOfT);
  }
}
