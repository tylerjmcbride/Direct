package github.tylerjmcbride.direct.registration;

import github.tylerjmcbride.direct.Direct;

public abstract class Registrar {
    protected static final int BUFFER_SIZE = 65536;
    protected Direct direct;

    public Registrar(Direct direct) {
        this.direct = direct;
    }
}
