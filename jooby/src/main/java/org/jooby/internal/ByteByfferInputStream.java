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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteByfferInputStream extends InputStream {

  private ByteBuffer buffer;

  public ByteByfferInputStream(final ByteBuffer buffer) {
    this.buffer = buffer.asReadOnlyBuffer();
    if (this.buffer.position() > 0) {
      this.buffer.rewind();
    }
  }

  @Override
  public int available() {
    return buffer.remaining();
  }

  @Override
  public int read() throws IOException {
    return buffer.hasRemaining() ? buffer.get() & 0xFF : -1;
  }

  @Override
  public int read(final byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(final byte[] bytes, final int off, final int len) throws IOException {
    if (!buffer.hasRemaining()) {
      return -1;
    }
    int count = Math.min(len, buffer.remaining());
    buffer.get(bytes, off, count);
    return count;
  }

  @Override
  public void reset() throws IOException {
    buffer.rewind();
  }

  @Override
  public void close() throws IOException {
    buffer.clear();
  }

}
