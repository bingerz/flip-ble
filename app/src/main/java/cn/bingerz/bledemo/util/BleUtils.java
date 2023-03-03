package cn.bingerz.bledemo.util;

import static cn.bingerz.flipble.utils.GeneralUtil.extractServiceUUID;

import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.SparseArray;

import java.util.Map;
import java.util.Set;

import cn.bingerz.flipble.scanner.ScanRecord;
import cn.bingerz.flipble.utils.HexUtil;

public class BleUtils {

    public static byte[] extractBytes(byte[] inputBytes, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(inputBytes, start, bytes, 0, length);
        return bytes;
    }

    public static String parseAdvType(ScanRecord scanRecord) {
        String advType = "";
        if (scanRecord != null) {
            //Parsing the ServiceData UUID in the Bluetooth broadcast
            String serviceDataUUID = parseAdvServiceDataUUID(scanRecord);
            //Parsing the ManufacturerSpecificData Key in the Bluetooth broadcast
            String manufacturerKey = parseAdvManufacturerDataKey(scanRecord);

            String manufacturerData = BleUtils.parseAdvManufacturerData(scanRecord);
            //FMN Protocol Type
            if (!TextUtils.isEmpty(serviceDataUUID) && "FD44".equalsIgnoreCase(serviceDataUUID)) {
                advType = "FMN UnPaired";
            } else if (!TextUtils.isEmpty(manufacturerKey) && "004C".equalsIgnoreCase(manufacturerKey)) {
                if (!TextUtils.isEmpty(manufacturerData) && manufacturerData.length() == 8) {
                    advType = "FMN Nearby";
                } else {
                    advType = "FMN Separated";
                }
            }
        }
        return advType;
    }

    public static String parseAdvServiceDataUUID(ScanRecord scanRecord) {
        String serviceDataUUID = "";
        if (scanRecord != null) {
            Map<ParcelUuid, byte[]> serviceDataMap = scanRecord.getServiceData();
            if (serviceDataMap != null && !serviceDataMap.isEmpty()) {
                Set<ParcelUuid> parcelUuidSet = serviceDataMap.keySet();
                for (ParcelUuid parcelUuid : parcelUuidSet) {
                    String extractUUID = extractServiceUUID(parcelUuid.toString());
                    extractUUID = extractUUID.toUpperCase();
                    if (TextUtils.isEmpty(serviceDataUUID)) {
                        serviceDataUUID = extractUUID;
                    } else {
                        serviceDataUUID = serviceDataUUID + "/" + extractUUID;
                    }
                }
            }
        }
        return serviceDataUUID;
    }

    public static String parseAdvServiceData(ScanRecord scanRecord) {
        String serviceData = "";
        if (scanRecord != null) {
            Map<ParcelUuid, byte[]> serviceDataMap = scanRecord.getServiceData();
            if (serviceDataMap != null && !serviceDataMap.isEmpty()) {
                Set<ParcelUuid> parcelUuidSet = serviceDataMap.keySet();
                for (ParcelUuid parcelUuid : parcelUuidSet) {
                    byte[] serviceDataBytes = serviceDataMap.get(parcelUuid);
                    String encodeData = HexUtil.encodeHexStr(serviceDataBytes, false);
                    if (TextUtils.isEmpty(serviceData)) {
                        serviceData = encodeData;
                    } else {
                        serviceData = serviceData + "/" + encodeData;
                    }
                }
            }
        }
        return serviceData;
    }

    public static String parseAdvManufacturerDataKey(ScanRecord scanRecord) {
        String manufacturerDataKey = "";
        if (scanRecord != null) {
            SparseArray<byte[]> manufacturerDataArray = scanRecord.getManufacturerSpecificData();
            if (manufacturerDataArray.size() > 0) {
                int key = manufacturerDataArray.keyAt(0);
                manufacturerDataKey = String.format("%04X", key);
            }
        }
        return manufacturerDataKey;
    }

    public static String parseAdvManufacturerData(ScanRecord scanRecord) {
        String manufacturerData = "";
        if (scanRecord != null) {
            SparseArray<byte[]> manufacturerDataArray = scanRecord.getManufacturerSpecificData();
            if (manufacturerDataArray.size() > 0) {
                int md = ConvertUtils.parseSparseArray(manufacturerDataArray, 0);
                manufacturerData = String.format("%X", md);
            }
        }
        return manufacturerData;
    }
}
