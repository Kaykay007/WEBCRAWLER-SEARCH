package com.udacity.webcrawler.profiler;

import lombok.extern.java.Log;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
@Log
/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;


  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

    @Profiled
    public boolean profiledState(Class<?> klass) throws IllegalArgumentException{
        Method[] meth = klass.getDeclaredMethods();
        if (meth.length == 0) return false;
        for (Method method : meth) {
            if (method.getAnnotation(Profiled.class) != null) {
                return true;
            }
        }
        return false;
    }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) throws IllegalArgumentException {
    Objects.requireNonNull(klass);

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.
 if (!profiledState(klass)) {
      throw new IllegalArgumentException(
              "Method not seen");
    }
     ProfilingMethodInterceptor interceptor = new ProfilingMethodInterceptor (
             this.clock,
             delegate,
             this.state,
             this.startTime
     );


     @SuppressWarnings("unchecked")
        Object startProxy = (T) Proxy.newProxyInstance(
                ProfilerImpl.class.getClassLoader(),
                new Class[]{klass}, interceptor);
    return (T) startProxy;
  }

  @Override
  public void writeData(Path path) throws IOException {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.

      Objects.requireNonNull(path);

      if (Files.notExists(path)){
          Files.createFile(path);
      }

      try(BufferedWriter bufferedWriter = Files.newBufferedWriter(path)) {
          writeData(bufferedWriter);
      } catch (IOException exception) {
          //  exception.printStackTrace();
          log.info("bfr stacktrace");

      }

  }



    @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
