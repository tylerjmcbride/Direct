package github.tylerjmcbride.direct.model;

import java.net.InetAddress;

public class WifiP2pDeviceInfo {

    private String macAddress;
    private InetAddress ipAddress;
    private int port = 0;

    public WifiP2pDeviceInfo(String macAddress, InetAddress ipAddress, int port) {
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public WifiP2pDeviceInfo(WifiP2pDeviceInfo source) {
        this.macAddress = source.getMacAddress();
        this.ipAddress = source.getIpAddress();
        this.port = source.getPort();
    }

    public WifiP2pDeviceInfo(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WifiP2pDeviceInfo)) return false;

        WifiP2pDeviceInfo other = (WifiP2pDeviceInfo) obj;
        if (other == null || other.macAddress == null) {
            return (this.macAddress == null);
        }

        return this.macAddress.equals(other.macAddress);
    }

    @Override
    public int hashCode() {
        return macAddress.hashCode();
    }
}
