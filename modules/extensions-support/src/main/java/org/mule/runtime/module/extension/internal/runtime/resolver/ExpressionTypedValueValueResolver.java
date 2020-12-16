/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.api.util.ClassUtils.isInstance;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;

import javax.inject.Inject;

/**
 * A {@link ValueResolver} implementation and extension of {@link TypeSafeExpressionValueResolver } which evaluates expressions
 * and tries to ensure that the output is always of a certain type.
 * <p>
 * This {@link ValueResolver} will return the {@link TypedValue} of the expression evaluation result.
 *
 * @param <T>
 * @since 4.0
 */
public class ExpressionTypedValueValueResolver<T> extends ExpressionValueResolver<TypedValue<T>> implements Initialisable {

  private final DataType expectedType;
  private final boolean content;
  private final Class<T> expectedClass;
  private TypeSafeTransformer typeSafeTransformer;

  @Inject
  private TransformationService transformationService;

  public ExpressionTypedValueValueResolver(String expression, DataType expectedType) {
    this(expression, expectedType, false);
  }

  public ExpressionTypedValueValueResolver(String expression, DataType expectedType, boolean content) {
    super(expression, expectedType);
    this.expectedType = expectedType;
    expectedClass = (Class<T>) expectedType.getType();
    this.content = content;
  }

  @Override
  public TypedValue<T> resolve(ValueResolvingContext context) throws MuleException {
    TypedValue<T> typedValue = resolveTypedValue(context);
    T value = typedValue.getValue();
    if (!isInstance(expectedClass, value)) {
      DataType dataType = typedValue.getDataType();
      DataType requestedDataType =
          DataType.builder()
              .type(expectedClass)
              .mediaType(dataType.getMediaType())
              .build();
      return new TypedValue<>(typeSafeTransformer.transform(value, dataType, requestedDataType), requestedDataType);
    }
    return typedValue;
  }

  public void setTransformationService(TransformationService transformationService) {
    this.transformationService = transformationService;
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    this.typeSafeTransformer = new TypeSafeTransformer(transformationService);
  }

  @Override
  public boolean isContent() {
    return content;
  }
}
