package cn.bingerz.flipble.scanner;

/**
 * @author hanson
 */
public class ScanFilterConfig {

    private String mServiceUUID = null;
    private String mDeviceName = null;
    private String mDeviceMac = null;

    public String getServiceUUID() {
        return mServiceUUID;
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public String getDeviceMac() {
        return mDeviceMac;
    }

    @Override
    public String toString() {
        return "ScanFilterConfig{" +
                "mServiceUUID='" + mServiceUUID + '\'' +
                ", mDeviceName='" + mDeviceName + '\'' +
                ", mDeviceMac='" + mDeviceMac + '\'' +
                '}';
    }

    public static class Builder {

        private String mServiceUUID = null;
        private String mDeviceName = null;
        private String mDeviceMac = null;

        public Builder setServiceUUID(String uuid) {
            this.mServiceUUID = uuid;
            return this;
        }

        public Builder setDeviceName(String name) {
            this.mDeviceName = name;
            return this;
        }

        public Builder setDeviceMac(String mac) {
            this.mDeviceMac = mac;
            return this;
        }

        void applyConfig(ScanFilterConfig config) {
            config.mServiceUUID = this.mServiceUUID;
            config.mDeviceName = this.mDeviceName;
            config.mDeviceMac = this.mDeviceMac;
        }

        public ScanFilterConfig build() {
            ScanFilterConfig config = new ScanFilterConfig();
            applyConfig(config);
            return config;
        }
    }
}
