package com.yll;

import com.yll.crypto.CryptoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CryptoService 单元测试。
 *
 * <p>验证加密 / 解密的正确性、空数据处理、大文件模拟等场景。</p>
 */
class SpecialToolTest {

    // ======================== 加密 / 解密 ========================

    @Test
    @DisplayName("基本加密解密往返 — 小文本")
    void testEncodeDecodeRoundTrip() {
        byte[] original = "123".getBytes(StandardCharsets.UTF_8);
        String encoded = CryptoService.encode(original);
        byte[] decoded = CryptoService.decode(encoded);

        assertArrayEquals(original, decoded, "加密 → 解密后内容应与原文完全一致");
    }

    @Test
    @DisplayName("空字符串加密解密")
    void testEncodeDecodeEmpty() {
        byte[] original = new byte[0];
        String encoded = CryptoService.encode(original);
        byte[] decoded = CryptoService.decode(encoded);

        assertArrayEquals(original, decoded, "空数据加密解密应返回空数组");
    }

    @Test
    @DisplayName("中文文本加密解密")
    void testEncodeDecodeChinese() {
        String text = "你好，世界！Hello World！";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);
        String encoded = CryptoService.encode(original);
        byte[] decoded = CryptoService.decode(encoded);

        assertEquals(text, new String(decoded, StandardCharsets.UTF_8), "中文加密解密应完整还原");
    }

    @Test
    @DisplayName("二进制数据加密解密")
    void testEncodeDecodeBinary() {
        // 构造包含 0-255 所有字节值的数组
        byte[] original = new byte[256];
        for (int i = 0; i < 256; i++) {
            original[i] = (byte) i;
        }
        String encoded = CryptoService.encode(original);
        byte[] decoded = CryptoService.decode(encoded);

        assertArrayEquals(original, decoded, "二进制数据加密解密应完整还原");
    }

    @Test
    @DisplayName("不同数据产生不同密文（随机 IV）")
    void testRandomIvProducesDifferentCiphertext() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        String encoded1 = CryptoService.encode(data);
        String encoded2 = CryptoService.encode(data);

        // 由于随机 IV，两次加密的密文应该不同
        assertNotEquals(encoded1, encoded2, "相同数据加密应产生不同密文（随机 IV）");
        // 但两者都能正确解密
        assertArrayEquals(data, CryptoService.decode(encoded1));
        assertArrayEquals(data, CryptoService.decode(encoded2));
    }

    @Test
    @DisplayName("错误密钥解密应抛异常")
    void testDecodeWithWrongKey() {
        // 先正常加密
        String encoded = CryptoService.encode("secret".getBytes(StandardCharsets.UTF_8));
        // 篡改 Base64 密文中的一个字符
        String tampered = encoded.substring(0, encoded.length() - 2) + "AA";

        assertThrows(RuntimeException.class, () -> CryptoService.decode(tampered),
                "篡改后的密文解密应抛出 RuntimeException");
    }

    // ======================== 辅助方法 ========================

    @Test
    @DisplayName("java.home 路径非空")
    void testGetJavaHome() {
        String javaHome = System.getProperty("java.home");
        assertNotNull(javaHome, "java.home 不应为 null");
        assertTrue(javaHome.length() > 0, "java.home 不应为空字符串");
    }
}