package github.tylerjmcbride.direct.listeners;

import github.tylerjmcbride.direct.model.HostDevice;

/**
 * Interface for callback invocation on a client registration action.
 */
public interface ClientRegisteredListener {
    void onClientRegistered(HostDevice device);
    void onClientUnregistered(HostDevice device);
}
