package cn.bingerz.bledemo.util;


import android.text.TextUtils;

public class ServiceUUID {

    /**
     * Bluetooth standard service:
     * org.bluetooth.service.device_information device info service
     */
    public static final String SERVER_DEVICE_INFO = buildUUID("180A");

    /**
     * device_information characteristic uuid system id
     */
    public static final String SYSTEM_ID = buildUUID("2A23");

    /**
     * device_information characteristic uuid firmware version
     */
    public static final String FIRMWARE_VERSION = buildUUID("2A26");

    /**
     * device_information characteristic uuid hardware version
     */
    public static final String HARDWARE_VERSION = buildUUID("2A27");

    /**
     * device_information characteristic uuid manufacture name
     */
    public static final String MANUFACTURE_NAME = buildUUID("2A29");

    /**
     * device_information characteristic uuid PnP ID
     */
    public static final String PNP_ID = buildUUID("2A50");



    /**
     * Bluetooth standard service:
     * org.bluetooth.service.battery_service battery service
     */
    public static final String SERVICE_BATTERY = buildUUID("180F");

    /**
     * battery level characteristic uuid
     */
    public static final String BATTERY_LEVEL  = buildUUID("2A19");


    public static String buildUUID(String uuid) {
       return String.format("0000%s-0000-1000-8000-00805f9b34fb", uuid);
    }

    public static String extractUUID(String uuid) {
        String result = "null";
        if (TextUtils.isEmpty(uuid)) {
            return result;
        }
        try {
            result = uuid.substring(4, 8);
        } catch (Exception e) {
            //ignore
        }
        return result;
    }
}
