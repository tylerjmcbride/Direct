package github.tylerjmcbride.direct.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.util.Objects;

@JsonObject
public class Device {
    /**
     * The device name is a user friendly string to identify a Wi-Fi p2p device
     */
    @JsonField
    public String deviceName;

    /**
     * The device MAC address uniquely identifies a Wi-Fi p2p device
     */
    @JsonField
    public String deviceAddress;

    /**
     * The device IP address
     */
    @JsonField
    public int ipAddress;

    /**
     * The device port to transfer data
     */
    @JsonField
    public int port;

    /**
     * Default constructor required for {@link com.bluelinelabs.logansquare.LoganSquare}.
     */
    public Device() {

    }

    /**
     * Constructor for {@link Device}.
     * @param deviceName The {@link Device#deviceName}.
     * @param deviceAddress The {@link Device#deviceAddress}.
     * @param ipAddress The {@link Device#ipAddress}.
     * @param port The {@link Device#port}.
     */
    public Device(String deviceName, String deviceAddress, int ipAddress, int port) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Copy constructor
     * @param source The {@link Device} to copy.
     */
    public Device(Device source) {
        if (source == null) {
            throw new NullPointerException();
        }

        this.deviceName = source.deviceName;
        this.deviceAddress = source.deviceAddress;
        this.ipAddress = source.ipAddress;
        this.port = source.port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Device)) return false;

        Device other = (Device) obj;
        if (other == null || other.deviceAddress == null) {
            return (deviceAddress == null);
        }
        return other.deviceAddress.equals(deviceAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceAddress);
    }
}
