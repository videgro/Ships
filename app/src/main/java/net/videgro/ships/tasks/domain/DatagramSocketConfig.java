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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DatagramSocketConfig that = (DatagramSocketConfig) o;

        if (port != that.port) {
            return false;
        }
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        int result = address.hashCode();
        result = 31 * result + port;
        return result;
    }
}
