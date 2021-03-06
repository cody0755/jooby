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

import static java.util.Objects.requireNonNull;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import org.jooby.BodyFormatter;
import org.jooby.util.ExSupplier;

import com.google.common.collect.ImmutableMap;

public class BodyFormatterContext implements BodyFormatter.Context {

  private Charset charset;

  private ExSupplier<OutputStream> stream;

  private ExSupplier<Writer> writer;

  private Map<String, Object> locals;

  public BodyFormatterContext(final Charset charset, final Map<Object, Object> locals,
      final ExSupplier<OutputStream> stream, final ExSupplier<Writer> writer) {
    this.charset = requireNonNull(charset, "A charset is required.");
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    requireNonNull(locals, "Request locals are required.").forEach((k, v) -> {
      if (k instanceof String) {
        builder.put((String) k, v);
      }
    });
    this.locals = builder.build();
    this.stream = requireNonNull(stream, "A stream is required.");
    this.writer = requireNonNull(writer, "A writer is required.");
  }

  public BodyFormatterContext(final Charset charset, final ExSupplier<OutputStream> stream,
      final ExSupplier<Writer> writer) {
    this(charset, Collections.emptyMap(), stream, writer);
  }

  @Override
  public Map<String, Object> locals() {
    return locals;
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public void text(final Text text) throws Exception {
    Writer writer = this.writer.get();
    // don't close on errors
    text.write(new WriterNoClose(writer));
    writer.close();
  }

  @Override
  public void bytes(final Bytes bin) throws Exception {
    OutputStream out = this.stream.get();
    // don't close on errors
    bin.write(new OutputStreamNoClose(out));
    out.close();
  }

}
