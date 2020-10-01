package org.mule.runtime.core.api.policy;

import org.mule.runtime.api.message.Message;

public interface PolicyIsolationTransformer {

  /**
   * From Message with Attributes to Message with HTTPMessage
   *
   * @param message
   * @return
   */
  Message isolate(Message message);

  /**
   * From Message with HTTPMessage to Message with Attributes
   *
   * @param message
   * @return
   */
  Message desolate(Message message);

}
