package github.tylerjmcbride.direct.registration;

import android.os.Handler;

import github.tylerjmcbride.direct.Direct;

public abstract class Registrar {

    protected static final int BUFFER_SIZE = 65536;
    protected Direct direct;
    protected Handler handler;

    public Registrar(Direct direct, Handler handler) {
        this.direct = direct;
        this.handler = handler;
    }
}
