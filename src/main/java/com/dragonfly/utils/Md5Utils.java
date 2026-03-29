package com.dragonfly.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5加密工具类
 *
 * @author 蜻蜓大王
 * @date 2026/3/29 16:23
 */
public class Md5Utils {

    protected static char hexDigits[] = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    protected static MessageDigest messageDigest = null;

    static {
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println(Md5Utils.class.getName() + "初始化失败，MessageDigest不支持MD5算法。");
            nsae.printStackTrace();
        }
    }

    /**
     * 生成字符串的md5校验值
     */
    public static String getMD5String(String s) {
        return getMD5String(s.getBytes());
    }

    /**
     * 生成字节数组的md5校验值
     */
    public static String getMD5String(byte[] bytes) {
        if (bytes == null) return null;

        byte[] md5Bytes = messageDigest.digest(bytes);
        StringBuilder sb = new StringBuilder(md5Bytes.length * 2);
        for (byte b : md5Bytes) {
            sb.append(hexDigits[(b >> 4) & 0xf]);
            sb.append(hexDigits[b & 0xf]);
        }
        return sb.toString();
    }

    /**
     * 判断字符串的md5校验码是否与已知md5码相匹配
     */
    public static boolean checkPassword(String password, String md5Password) {
        if (password == null || md5Password == null) return false;
        return getMD5String(password).equalsIgnoreCase(md5Password);
    }
}