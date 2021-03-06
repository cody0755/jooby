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
package org.jooby.json;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import java.text.SimpleDateFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.jooby.BodyFormatter;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Parser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

/**
 * JSON support from the excellent <a href="https://github.com/FasterXML/jackson">Jackson</a>
 * library.
 *
 * This module provides a JSON {@link Parser} and {@link BodyFormatter}, but also an
 * {@link ObjectMapper}.
 *
 * <h1>usage</h1>
 *
 * <pre>
 * {
 *   use(new Jackson());
 *
 *   // sending
 *   get("/my-api", req {@literal ->} new MyObject());
 *
 *   // receiving a json body
 *   post("/my-api", req {@literal ->} {
 *     MyObject obj = req.body(MyObject.class);
 *     return obj;
 *   });
 *
 *   // receiving a json param from a multipart or form url encoded
 *   post("/my-api", req {@literal ->} {
 *     MyObject obj = req.param("my-object").to(MyObject.class);
 *     return obj;
 *   });
 * }
 * </pre>
 *
 * <h1>advanced configuration</h1> If you need a special setting or configuration for your
 * {@link ObjectMapper}:
 *
 * <pre>
 * {
 *   use(new Jackson().configure(mapper {@literal ->} {
 *     // setup your custom object mapper
 *   });
 * }
 * </pre>
 *
 * or provide an {@link ObjectMapper} instance:
 *
 * <pre>
 * {
 *   ObjectMapper mapper = ....;
 *   use(new Jackson(mapper));
 * }
 * </pre>
 *
 * It is possible to wire Jackson modules too:
 *
 * <pre>
 * {
 *
 *   use(new Jackson());
 *
 *   use((mode, config, binder) {@literal ->} {
 *     Multibinder.newSetBinder(binder, Module.class).addBinding()
 *       .to(MyJacksonModuleWiredByGuice.class);
 *   });
 * }
 * </pre>
 *
 * This is useful when your jackson module require some dependencies.
 *
 * @author edgar
 * @since 0.6.0
 */
public class Jackson implements Jooby.Module {

  private static class PostConfigurer {

    @Inject
    public PostConfigurer(final ObjectMapper mapper, final Set<Module> jacksonModules) {
      mapper.registerModules(jacksonModules);
    }

  }

  private static class BodyHandler implements BodyFormatter, Parser {

    private ObjectMapper mapper;
    private List<MediaType> types;
    private MediaType.Matcher matcher;

    public BodyHandler(final ObjectMapper mapper, final List<MediaType> types) {
      this.mapper = mapper;
      this.types = types;
      this.matcher = MediaType.matcher(types);
    }

    @Override
    public List<MediaType> types() {
      return types;
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return mapper.canSerialize(type);
    }

    @Override
    public Object parse(final TypeLiteral<?> type, final Parser.Context ctx) throws Exception {
      if (matcher.matches(ctx.type())) {
        JavaType javaType = mapper.constructType(type.getType());
        return ctx.body(body -> mapper.readValue(body.bytes(), javaType));
      }
      return ctx.next();
    }

    @Override
    public void format(final Object body, final BodyFormatter.Context writer) throws Exception {
      writer.text(out -> mapper.writeValue(out, body));
    }

    @Override
    public String toString() {
      return "json";
    }

  }

  private final ObjectMapper mapper;

  private final Set<Module> modules = new LinkedHashSet<>();

  private List<MediaType> types = ImmutableList.of(MediaType.json);

  public Jackson(final ObjectMapper mapper) {
    this.mapper = checkNotNull(mapper, "An object mapper is required.");
    this.modules.add(new Jdk8Module());
    // Java 8 dates
    this.modules.add(new JSR310Module());
  }

  public Jackson() {
    this(new ObjectMapper());
  }

  public Jackson types(final MediaType... types) {
    return types(ImmutableList.copyOf(types));
  }

  public Jackson types(final List<MediaType> types) {
    this.types = ImmutableList.copyOf(types);
    return this;
  }

  public Jackson types(final String... types) {
    return types(MediaType.valueOf(types));
  }

  public Jackson doWith(final Consumer<ObjectMapper> block) {
    requireNonNull(block, "A json block is required.").accept(mapper);
    return this;
  }

  @Override
  public void configure(final Env mode, final Config config, final Binder binder) {
    Locale locale = Locale.forLanguageTag(config.getString("application.lang").replace("_", "-"));
    // Jackson clone the date format in order to make dateFormat thread-safe
    mapper.setDateFormat(new SimpleDateFormat(config.getString("application.dateFormat"), locale));
    mapper.setLocale(locale);
    mapper.setTimeZone(TimeZone.getTimeZone(config.getString("application.tz")));

    // Jackson Modules from Guice
    Multibinder<Module> moduleBinder = Multibinder.newSetBinder(binder, Module.class);
    modules.forEach(m -> moduleBinder.addBinding().toInstance(m));

    binder.bind(ObjectMapper.class).toInstance(mapper);

    // Jackson Configurer (like a post construct)
    binder.bind(PostConfigurer.class).asEagerSingleton();

    // json body parser & formatter
    BodyHandler json = new BodyHandler(mapper, types);

    Multibinder.newSetBinder(binder, BodyFormatter.class)
        .addBinding()
        .toInstance(json);

    Multibinder.newSetBinder(binder, Parser.class)
        .addBinding()
        .toInstance(json);

    // direct access?
    binder.bind(Key.get(BodyFormatter.class, Names.named(json.toString()))).toInstance(json);
    binder.bind(Key.get(Parser.class, Names.named(json.toString()))).toInstance(json);

  }

}
