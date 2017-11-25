package net.videgro.ships.tasks.domain;


public class SocketIoConfig {

    private final String androidId;
    private final String server;
    private final String topic;

    public SocketIoConfig(String androidId, String server, String topic) {
        this.androidId = androidId;
        this.server = server;
        this.topic = topic;
    }

    public String getAndroidId() {
        return androidId;
    }

    public String getServer() {
        return server;
    }

    public String getTopic() {
        return topic;
    }
}
