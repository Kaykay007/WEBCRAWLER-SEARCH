package com.udacity.webcrawler.profiler;


import lombok.EqualsAndHashCode;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
@EqualsAndHashCode


final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object target;
  private final ProfilingState state;
  private final ZonedDateTime timeStart;





  // TODO: You will need to add more instance fields and constructor arguments to this class.
  public <T> ProfilingMethodInterceptor(Clock clock,  Object target, ProfilingState state, ZonedDateTime timeStart) {
    this.clock = Objects.requireNonNull(clock);
    this.target = Objects.requireNonNull(target);
    this.state = Objects.requireNonNull(state);
    this.timeStart = Objects.requireNonNull(timeStart);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the instant time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.

    Object o = null;
    Instant instant = null;
    boolean profiled = method.getAnnotation(Profiled.class) != null;
    if (profiled) {
      instant = clock.instant();
    }
    try {
      o = method.invoke(this.target, args);
    } catch (InvocationTargetException targetException) {
      throw targetException.getTargetException();
      //handle UndeclaredThrowableException
    } catch (IllegalArgumentException e){
      throw new RuntimeException();
    }
    finally {
      if (profiled) {
        Duration DD = Duration.between(instant, clock.instant());
        state.record(this.target.getClass(), method, DD);
      }
    }

    return o;
  }

}
