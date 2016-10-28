package io.vantiq.client.internal;

import okhttp3.ResponseBody;
import org.omg.PortableServer.ThreadPolicyOperations;

/**
 * Interface for listening to VantiqSubscriber lifecycle events
 */
public interface VantiqSubscriberLifecycleListener {

    void onConnect();

    void onError(String message, ResponseBody body);

    void onFailure(Throwable t);

    void onClose();

}
