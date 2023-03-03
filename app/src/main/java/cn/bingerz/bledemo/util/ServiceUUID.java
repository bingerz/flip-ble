package cn.bingerz.bledemo.util;


import android.text.TextUtils;

/**
 * @author hanson
 */
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



    /**
     * Find My network service
     */
    public static final String SERVICE_FIND_MY_NETWORK = buildUUID("FD44");

    /**
     * Pairing Control Point
     */
    public static final String PAIRING_CONTROL_POINT= "4f860001-943b-49ef-bed4-2f730304427a";

    /**
     * Configuration Control Point
     */
    public static final String CONFIGURATION_CONTROL_POINT = "4f860002-943b-49ef-bed4-2f730304427a";

    /**
     * Non Owner Control Point
     */
    public static final String NON_OWNER_CONTROL_POINT = "4f860003-943b-49ef-bed4-2f730304427a";

    /**
     * Paired owner Information Control Point
     */
    public static final String PAIRED_OWNER_INFORMATION_CONTROL_POINT = "4f860004-943b-49ef-bed4-2f730304427a";



    /**
     * Find My Accessory Information service:
     * Apple Find My Accessory Information service
     */
    public static final String SERVICE_ACCESSORY_INFORMATION = "87290102-3c51-43b1-a1a9-11b9dc38478b";

    /**
     * ProductData characteristic uuid
     */
    public static final String ACCESSORY_INFO_PRODUCT_DATA = "6aa50001-6352-4d57-a7b4-003a416fbb0b";

    /**
     * ManufacturerName characteristic uuid
     */
    public static final String ACCESSORY_INFO_MANUFACTURER_NAME = "6aa50002-6352-4d57-a7b4-003a416fbb0b";

    /**
     * ModelName characteristic uuid
     */
    public static final String ACCESSORY_INFO_MODEL_NAME = "6aa50003-6352-4d57-a7b4-003a416fbb0b";

    /**
     * AccessoryCategory characteristic uuid
     */
    public static final String ACCESSORY_INFO_ACCESSORY_CATEGORY = "6aa50005-6352-4d57-a7b4-003a416fbb0b";

    /**
     * AccessoryCapability characteristic uuid
     */
    public static final String ACCESSORY_INFO_ACCESSORY_CAPABILITY = "6aa50006-6352-4d57-a7b4-003a416fbb0b";

    /**
     * FirmwareVersion characteristic uuid
     */
    public static final String ACCESSORY_INFO_FIRMWARE_VERSION = "6aa50007-6352-4d57-a7b4-003a416fbb0b";

    /**
     * FindMyVersion characteristic uuid
     */
    public static final String ACCESSORY_INFO_FINDMY_VERSION = "6aa50008-6352-4d57-a7b4-003a416fbb0b";

    /**
     * BatteryType characteristic uuid
     */
    public static final String ACCESSORY_INFO_BATTERY_TYPE = "6aa50009-6352-4d57-a7b4-003a416fbb0b";

    /**
     * BatteryLevel characteristic uuid
     */
    public static final String ACCESSORY_INFO_BATTERY_LEVEL = "6aa5000a-6352-4d57-a7b4-003a416fbb0b";

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
