/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.typed.value.extension.extension.datasense;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;

public class JsonInputTypeResolver implements InputTypeResolver<Void> {

  @Override
  public MetadataType getInputMetadata(MetadataContext context, Void key) {
    ObjectTypeBuilder schemaBuilder = BaseTypeBuilder.create(JAVA).objectType();
    schemaBuilder.addField().key("name").value().stringType();
    schemaBuilder.addField().key("age").value().numberType();

    return schemaBuilder.build();
  }

  @Override
  public String getCategoryName() {
    return "json";
  }
}
