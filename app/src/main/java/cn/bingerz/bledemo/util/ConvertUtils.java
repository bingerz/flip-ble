package cn.bingerz.bledemo.util;

import android.os.Build;
import android.util.SparseArray;

import java.nio.charset.StandardCharsets;

import cn.bingerz.flipble.utils.HexUtil;

/**
 * @author hanson
 */
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
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result = result | (bytes[i] & 0xff) << 8 * (bytes.length - 1 - i);
        }
        return result;
    }

    public static String toAsciiString(byte[] bytes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new String(bytes, StandardCharsets.UTF_8);
        } else {
            return HexUtil.encodeHexStr(bytes, false);
        }
    }

    public static byte[] byteReverse(byte[] input) {
        int length = input.length;
        byte[] temp = new byte[length];
        int i = 0;
        while (--length >= 0) {
            temp[length] = input[i];
            i++;
        }
        return temp;
    }
}
