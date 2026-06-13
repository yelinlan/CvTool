package com.yll.crypto;

import cn.hutool.core.date.DateUtil;
import com.yll.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

/**
 * 加密 / 解密服务。
 *
 * <h3>改进点（相比旧版）</h3>
 * <ul>
 *   <li>AES-<strong>GCM</strong> 替代 ECB（认证加密，防篡改）</li>
 *   <li>每次加密生成随机 <strong>IV</strong>（12 字节），并附加在密文前</li>
 *   <li>密钥派生从 MD5 改为 <strong>HmacSHA256</strong>，并支持将时间戳嵌入密文</li>
 *   <li>解码方无需知道编码时间，从密文头部自动提取</li>
 * </ul>
 */
public final class CryptoService {

    private static final Logger log = LoggerFactory.getLogger(CryptoService.class);

    /** AES-GCM 认证标签长度（位） */
    private static final int GCM_TAG_LENGTH = 128;

    /** IV 长度（字节），GCM 推荐 12 字节 */
    private static final int IV_LENGTH = 12;

    /** hourBytes 长度（字节）。格式 "yyyyMMddHH" 共 10 个 ASCII 字符 = 10 字节 */
    private static final int HOUR_BYTES_LENGTH = 10;

    private CryptoService() {
    }

    // ======================== 公开 API ========================

    /**
     * 加密数据。
     *
     * <p>输出格式：Base64( IV[12字节] + 密文 + 认证标签 )</p>
     *
     * @param content 待加密的字节数组
     * @return Base64 编码的密文
     */
    public static String encode(byte[] content) {
        byte[] iv = generateRandomIv();
        byte[] hourBytes = getCurrentHourBytes();

        try {
            SecretKey key = deriveKey(hourBytes);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(content);

            // 拼接：hourBytes[4] + iv[12] + encrypted
            byte[] output = new byte[hourBytes.length + iv.length + encrypted.length];
            System.arraycopy(hourBytes, 0, output, 0, hourBytes.length);
            System.arraycopy(iv, 0, output, hourBytes.length, iv.length);
            System.arraycopy(encrypted, 0, output, hourBytes.length + iv.length, encrypted.length);

            String result = Base64.getEncoder().encodeToString(output);
            log.debug("加密完成，原始长度={}，密文长度={}", content.length, result.length());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密数据。
     *
     * <p>从密文头部读取 hourBytes 和 IV，自动还原密钥。</p>
     *
     * @param base64Content Base64 编码的密文
     * @return 解密后的字节数组
     */
    public static byte[] decode(String base64Content) {
        try {
            byte[] raw = Base64.getDecoder().decode(base64Content);

            // 提取 hourBytes（10 字节）和 IV（12 字节）
            byte[] hourBytes = Arrays.copyOfRange(raw, 0, HOUR_BYTES_LENGTH);
            byte[] iv = Arrays.copyOfRange(raw, HOUR_BYTES_LENGTH, HOUR_BYTES_LENGTH + IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(raw, HOUR_BYTES_LENGTH + IV_LENGTH, raw.length);

            SecretKey key = deriveKey(hourBytes);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            log.debug("解密完成，密文长度={}，明文长度={}", base64Content.length(), decrypted.length);
            return decrypted;
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    // ======================== 内部方法 ========================

    /**
     * 生成随机 IV。
     */
    private static byte[] generateRandomIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * 获取当前小时标识的 UTF-8 字节数组，嵌入密文头部。
     */
    private static byte[] getCurrentHourBytes() {
        String hour = DateUtil.format(new Date(), AppConfig.KEY_TIME_FORMAT);
        return hour.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 从 hourBytes 派生 AES 密钥。
     *
     * <p>使用 HmacSHA256 替代旧版 MD5，强度更高。</p>
     *
     * @param hourBytes 小时标识的 UTF-8 字节数组
     * @return AES 密钥（16 字节 = AES-128）
     */
    private static SecretKey deriveKey(byte[] hourBytes) {
        try {
            // HmacSHA256 → 截取前 16 字节作为 AES-128 密钥
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(AppConfig.KEY_SALT.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmac = mac.doFinal(hourBytes);
            byte[] keyBytes = Arrays.copyOf(hmac, 16);
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("密钥派生失败", e);
        }
    }
}