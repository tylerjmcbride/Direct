package github.tylerjmcbride.direct.listeners;

import github.tylerjmcbride.direct.model.Device;

public interface RegisteredWithServerListener {
    void onSuccess(Device host);
    void onFailure();
}
