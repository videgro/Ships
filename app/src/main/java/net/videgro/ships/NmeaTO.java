package net.videgro.ships;

public class NmeaTO {
    public static final String SERVER="https://ships-ais-share.herokuapp.com";
    public static final String TOPIC_NMEA="net.videgro.ships.nmea";
    public static final String TOPIC_NMEA_RETRANSMIT="net.videgro.ships.nmea.retransmit";

    public static final String TOPIC_NMEA_SIGN_SEPARATOR="_";

    private String origin;
    private String timestamp;
    private String data;
    private String signature;

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
