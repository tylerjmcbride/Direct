package github.tylerjmcbride.direct.registration.model;

import java.io.Serializable;

public class Handshake implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The device MAC address uniquely identifies a Wi-Fi p2p device
     */
    private String macAddress;

    /**
     * The device port which listens for data
     */
    private int port;

    public Handshake(String macAddress, int port) {
        this.macAddress = macAddress;
        this.port = port;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
