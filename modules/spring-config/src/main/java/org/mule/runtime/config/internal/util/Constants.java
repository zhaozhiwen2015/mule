/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.util;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

public class Constants {

  public static String ENABLE_ENTITY_RESOLVER_LOGGING_PROPERTY = "mule.enableEntityResolverLogging";
  public static boolean ENABLE_ENTITY_RESOLVER_LOGGING =
      parseBoolean(getProperty(ENABLE_ENTITY_RESOLVER_LOGGING_PROPERTY, "false"));
}
