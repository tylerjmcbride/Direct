package github.tylerjmcbride.direct.model.data;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

@JsonObject(fieldDetectionPolicy = JsonObject.FieldDetectionPolicy.NONPRIVATE_FIELDS_AND_ACCESSORS)
public class HandshakeData extends Data {

    /**
     * The device MAC address uniquely identifies a Wi-Fi p2p device
     */
    @JsonField

    private String macAddress;

    /**
     * The device port which listens for data
     */
    @JsonField
    private int port;

    public HandshakeData() {

    }

    public HandshakeData(String macAddress, int port) {
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
