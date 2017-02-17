package net.videgro.ships.tasks.domain;

public class DatagramSocketConfig{
    private String address;
    private int port;

    public DatagramSocketConfig(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return address+":"+port;
    }
}
