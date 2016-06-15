/*
 * Copyright 2016 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import okio.ByteString;

/** An Android-specific {@link Message} which adds support for {@link Parcelable}. */
public abstract class AndroidMessage<M extends Message<M, B>, B extends Message.Builder<M, B>>
    extends Message<M, B> implements Parcelable {
  /** Creates a new {@link Parcelable.Creator} using {@code adapter} for serialization. */
  public static <E> Parcelable.Creator<E> newCreator(ProtoAdapter<E> adapter) {
    return new ProtoAdapterCreator<>(adapter);
  }

  /** Creates a new {@link Parcelable.Creator} for the supplied enum type. */
  public static <E extends Enum<E> & WireEnum> Parcelable.Creator<E> newCreator(Class<E> type) {
    return new EnumCreator<>(type);
  }

  protected AndroidMessage(ProtoAdapter<M> adapter, ByteString unknownFields) {
    super(adapter, unknownFields);
  }

  @Override public final void writeToParcel(Parcel dest, int flags) {
    dest.writeByteArray(encode());
  }

  @Override public final int describeContents() {
    return 0;
  }

  private static final class ProtoAdapterCreator<M> implements Creator<M> {
    private final ProtoAdapter<M> adapter;

    ProtoAdapterCreator(ProtoAdapter<M> adapter) {
      this.adapter = adapter;
    }

    @Override public M createFromParcel(Parcel in) {
      try {
        return adapter.decode(in.createByteArray());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override public M[] newArray(int size) {
      //noinspection unchecked
      return (M[]) Array.newInstance(adapter.javaType, size);
    }
  }

  private static final class EnumCreator<E> implements Creator<E> {
    private final Class<E> type;
    private Method fromValueMethod;

    private EnumCreator(Class<E> type) {
      this.type = type;
    }

    private Method getFromValueMethod() {
      Method method = this.fromValueMethod;
      if (method != null) {
        return method;
      }
      try {
        return fromValueMethod = type.getMethod("fromValue", int.class);
      } catch (NoSuchMethodException e) {
        throw new AssertionError(e);
      }
    }

    @Override public E createFromParcel(Parcel source) {
      try {
        //noinspection unchecked
        return (E) getFromValueMethod().invoke(source.readInt());
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new AssertionError(e);
      }
    }

    @Override public E[] newArray(int size) {
      //noinspection unchecked
      return (E[]) Array.newInstance(type, size);
    }
  }
}
