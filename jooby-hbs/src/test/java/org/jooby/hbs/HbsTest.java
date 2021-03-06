package org.jooby.hbs;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.jooby.Env;
import org.jooby.BodyFormatter;
import org.jooby.MockUnit;
import org.jooby.View;
import org.jooby.View.Engine;
import org.jooby.internal.hbs.HbsEngine;
import org.jooby.internal.hbs.HbsHelpers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.github.jknack.handlebars.Handlebars;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hbs.class, Multibinder.class })
public class HbsTest {

  @SuppressWarnings("unchecked")
  @Test
  public void configure() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
        .expect(unit -> {
          AnnotatedBindingBuilder<Handlebars> hABB = unit.mock(AnnotatedBindingBuilder.class);
          hABB.toInstance(isA(Handlebars.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Handlebars.class)).andReturn(hABB);
        })
        .expect(unit -> {
          Binder binder = unit.get(Binder.class);

          Multibinder<Object> mbinder = unit.mock(Multibinder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Object.class, Names.named("hbs.helpers")))
              .andReturn(mbinder);

          LinkedBindingBuilder<BodyFormatter> fLBB = unit.mock(LinkedBindingBuilder.class);
          fLBB.toInstance(isA(HbsEngine.class));

          Multibinder<BodyFormatter> mfbinder = unit.mock(Multibinder.class);
          expect(mfbinder.addBinding()).andReturn(fLBB);
          expect(Multibinder.newSetBinder(binder, BodyFormatter.class))
              .andReturn(mfbinder);

          LinkedBindingBuilder<Engine> neLBB = unit.mock(LinkedBindingBuilder.class);
          neLBB.toInstance(isA(HbsEngine.class));

          expect(binder.bind(Key.get(View.Engine.class, Names.named("hbs")))).andReturn(neLBB);

          AnnotatedBindingBuilder<HbsHelpers> hhABB = unit.mock(AnnotatedBindingBuilder.class);
          hhABB.asEagerSingleton();

          expect(binder.bind(HbsHelpers.class)).andReturn(hhABB);
        })
        .expect(unit -> {

        })
        .run(unit -> {
          new Hbs()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }
}
