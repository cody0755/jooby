/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.jooby.BodyFormatter;
import org.jooby.MediaType;
import org.jooby.Parser;
import org.jooby.View;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.inject.TypeLiteral;

public class BuiltinBodyConverter {

  public static BodyFormatter formatStream = new BodyFormatter() {
    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.octetstream);
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return InputStream.class.isAssignableFrom(type);
    }

    @Override
    public void format(final Object body, final BodyFormatter.Context writer) throws Exception {
      InputStream in = (InputStream) body;
      try {
        writer.bytes(out -> ByteStreams.copy(in, out));
      } finally {
        Closeables.closeQuietly(in);
      }
    }

    @Override
    public String toString() {
      return "Formatter for: " + InputStream.class.getName();
    }
  };

  public static BodyFormatter formatByteArray = new BodyFormatter() {
    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.octetstream);
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return type.isArray() && type.getComponentType() == byte.class;
    }

    @Override
    public void format(final Object body, final BodyFormatter.Context writer) throws Exception {
      writer.bytes(out -> ByteStreams.copy(new ByteArrayInputStream((byte[]) body), out));
    }

    @Override
    public String toString() {
      return "Formatter for: byte[]";
    }
  };

  public static BodyFormatter formatByteBuffer = new BodyFormatter() {
    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.octetstream);
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return ByteBuffer.class.isAssignableFrom(type);
    }

    @Override
    public void format(final Object body, final BodyFormatter.Context writer) throws Exception {
      ByteBuffer buffer = (ByteBuffer) body;
      if (buffer.hasArray()) {
        formatByteArray.format(buffer.array(), writer);
      } else {
        writer.bytes(out -> ByteStreams.copy(new ByteByfferInputStream(buffer), out));
      }
    }

    @Override
    public String toString() {
      return "Formatter for: " + ByteBuffer.class.getName();
    }
  };

  public static BodyFormatter formatReader = new BodyFormatter() {

    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.html);
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return Readable.class.isAssignableFrom(type);
    }

    @Override
    public void format(final Object body, final BodyFormatter.Context writer) throws Exception {
      try {
        Readable in = (Readable) body;
        writer.text(out -> CharStreams.copy(in, out));
      } finally {
        if (body instanceof Closeable) {
          Closeables.close((Closeable) body, true);
        }
      }
    }

    @Override
    public String toString() {
      return "Formatter for: " + Readable.class.getName();
    }
  };

  public static BodyFormatter formatAny = new BodyFormatter() {

    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.html);
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return !View.class.isAssignableFrom(type);
    }

    @Override
    public void format(final Object body, final BodyFormatter.Context writer) throws Exception {
      writer.text(out -> out.write(body.toString()));
    }

    @Override
    public String toString() {
      return "Formatter for: Object.toString()";
    }
  };

  public static Parser parseBytes = new Parser() {

    @Override
    public Object parse(final TypeLiteral<?> type, final Context ctx)
        throws Exception {
      if (type.getRawType() == byte[].class) {
        return ctx.body(body -> body.bytes());
      }
      return ctx.next();
    }

    @Override
    public String toString() {
      return "Parser for: byte[]";
    }
  };

}
