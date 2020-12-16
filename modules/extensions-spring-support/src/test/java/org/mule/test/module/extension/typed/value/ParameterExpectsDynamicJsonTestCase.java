/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.typed.value;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.metadata.TypedValue;

import org.junit.Test;

public class ParameterExpectsDynamicJsonTestCase extends AbstractTypedValueTestCase {

  @Override
  protected String getConfigFile() {
    return "expects-dynamic-json-config.xml";
  }

  @Test
  public void expectsJsonParameter() throws Exception {
    String json = (String) flowRunner("typedValueForObject").run().getMessage().getPayload().getValue();
    assertJson(json);
  }

  @Test
  public void expectsJsonParameterInTypedValue() throws Exception {
    TypedValue<String> json = flowRunner("expectTypedValueJsonParameter").run().getMessage().getPayload();
    assertJson(json.getValue());
  }

  private void assertJson(String json) {
    assertThat(json, equalTo("{\n" +
        "  \"name\": \"John Doe\",\n" +
        "  \"age\": 37\n" +
        "}"));
  }
}
