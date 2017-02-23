package github.tylerjmcbride.direct.listeners;

import github.tylerjmcbride.direct.model.Device;

/**
 * Interface for callback invocation on a client registration action.
 */
public interface ClientRegisteredListener {
    void onClientRegistered(Device device);
    void onClientUnregistered(Device device);
}
