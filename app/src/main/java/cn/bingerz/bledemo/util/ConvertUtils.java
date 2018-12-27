package cn.bingerz.bledemo.util;

import android.util.SparseArray;

public class ConvertUtils {

    //解析product值
    public static int parseSparseArray(SparseArray<byte[]> arrays, int index) {
        int value = -1;
        if (arrays == null || arrays.size() == 0) {
            value = 0;
        } else if (index >= 0 && index < arrays.size()){
            byte[] data = arrays.valueAt(index);
            value = byteArrayToInt(data);
        }
        return value;
    }

    public static int byteArrayToInt(byte[] bytes) {
        if (bytes.length == 4) {
            return bytes[0] << 24 | (bytes[1] & 0xff) << 16 | (bytes[2] & 0xff) << 8
                    | (bytes[3] & 0xff);
        } else if (bytes.length == 2) {
            return (bytes[0] & 0xff) << 8 | (bytes[1] & 0xff);
        }
        return 0;
    }
}
