/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.typed.value;

import org.junit.Test;

public class ParameterExpectsDynamicJsonTestCase extends AbstractTypedValueTestCase {

  @Override
  protected String getConfigFile() {
    return "expects-dynamic-json-config.xml";
  }

  @Test
  public void expectsDynamicJson() throws Exception {
    String json = (String) flowRunner("typedValueForObject").run().getMessage().getPayload().getValue();
    System.out.println(json);
  }
}
