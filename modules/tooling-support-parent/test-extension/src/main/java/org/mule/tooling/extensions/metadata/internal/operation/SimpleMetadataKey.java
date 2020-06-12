package org.mule.tooling.extensions.metadata.internal.operation;

import org.mule.runtime.extension.api.annotation.param.Parameter;

public class SimpleMetadataKey {

  @Parameter
  private String metadataKey;

  public String getMetadataKey() {
    return metadataKey;
  }

  public void setMetadataKey(String metadataKey) {
    this.metadataKey = metadataKey;
  }
}
