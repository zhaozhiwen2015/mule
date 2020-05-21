/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.component.execution.ExecutionService;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MuleExecutionServiceTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private final Location executableElementLocation = mock(Location.class);
  private final Location nonExecutableElementLocation = mock(Location.class);
  private final Location missingLocation = mock(Location.class);
  private final Location processorLocation = mock(Location.class);

  private final Component processor = mock(Component.class, withSettings().extraInterfaces(Processor.class, Lifecycle.class));

  private ExecutionService executionService;

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    ConfigurationComponentLocator configurationComponentLocator = mock(ConfigurationComponentLocator.class);

    ComponentLocation processorComponentLocation = mock(ComponentLocation.class);
    when(processorComponentLocation.getLocation()).thenReturn("main/processors/0");

    when(((Processor)processor).getProcessingType()).thenCallRealMethod();
    when(((Processor)processor).apply(any())).thenCallRealMethod();
    when(processor.getLocation()).thenReturn(processorComponentLocation);

    try {
      when(((Processor) processor).process(any(CoreEvent.class))).thenAnswer(inv -> inv.getArgument(0));
    }catch (MuleException e) {
      throw new RuntimeException(e);
    }

    ExecutableComponent executableComponent = mock(ExecutableComponent.class);
    when(executableComponent.execute(any(Event.class))).thenAnswer(inv -> completedFuture(inv.getArgument(0)));

    Component component = mock(Component.class);

    when(configurationComponentLocator.find(executableElementLocation)).thenReturn(of(executableComponent));
    when(configurationComponentLocator.find(nonExecutableElementLocation)).thenReturn(of(component));
    when(configurationComponentLocator.find(missingLocation)).thenReturn(empty());
    when(configurationComponentLocator.find(processorLocation)).thenReturn(of(processor));
    when(configurationComponentLocator.find(any(ComponentIdentifier.class))).thenReturn(emptyList());

    return singletonMap(REGISTRY_KEY, configurationComponentLocator);
  }

  @Before
  public void setUp() throws Exception {
    executionService = new MuleExecutionService();
    muleContext.getInjector().inject(executionService);
  }

  @Test
  public void componentNotFoundRaisesException() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage("not found");
    executionService.execute(null, missingLocation);
  }

  @Test
  public void nonExecutableComponentRaisesException() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage("Located component is not executable");
    executionService.execute(null, nonExecutableElementLocation);
  }

  @Test
  public void foundComponentIsExecuted() throws Exception {
    final Event event = mock(Event.class);
    CompletableFuture<Event> resultFuture = executionService.execute(event, executableElementLocation);
    assertThat(resultFuture.get(), is(equalTo(event)));
  }

  @Test
  public void processorsLifecycleOnlyOnce() throws Exception{
    final TypedValue<String> payload = TypedValue.of("PAYLOAD");
    final Message message = mock(Message.class);
    final Event event = mock(Event.class);
    when(message.<String>getPayload()).thenReturn(payload);
    when(event.getMessage()).thenReturn(message);
    when(event.getError()).thenReturn(empty());
    when(event.getVariables()).thenReturn(emptyMap());
    executionService.execute(event, singletonList(processorLocation));
    verify((Lifecycle)processor, times(1)).initialise();
    verify((Lifecycle)processor, times(1)).start();
    //verify((Lifecycle)processor, times(1)).stop();
    //verify((Lifecycle)processor, times(1)).dispose();
  }


}
