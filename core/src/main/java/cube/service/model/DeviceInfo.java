package cube.service.model;

import java.io.Serializable;

/**
 * 设备信息
 *
 * @author workerinchina@163.com
 */
public class DeviceInfo implements Serializable {
    private String cubeId;
    private String name;
    private String version;
    private String platform;
    private String userAgent;
    private String deviceId;

    public String getCubeId() {
        return cubeId;
    }

    public void setCubeId(String cubeId) {
        this.cubeId = cubeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof DeviceInfo) {
            DeviceInfo that = (DeviceInfo) o;
            if (cubeId != null ? !cubeId.equals(that.cubeId) : that.cubeId != null) {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null) {
                return false;
            }
            return deviceId != null ? deviceId.equals(that.deviceId) : that.deviceId == null;
        }
        return false;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" + "cubeId='" + cubeId + '\'' + ", name='" + name + '\'' + ", version='" + version + '\'' + ", platform='" + platform + '\'' + ", userAgent='" + userAgent + '\'' + ", deviceId='" + deviceId + '\'' + '}';
    }
}
