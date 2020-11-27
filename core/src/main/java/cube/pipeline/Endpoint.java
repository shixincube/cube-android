package cube.pipeline;

/**
 * 描述通信对端的类。
 *
 * @author LiuFeng
 * @data 2020/8/26 14:24
 */
public class Endpoint {
    private String name;
    private String address;
    private int port;

    public Endpoint(String name, String address, int port) {
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
