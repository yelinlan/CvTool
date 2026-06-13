package com.yll;

import com.yll.crypto.CryptoService;
import com.yll.registry.RegistryService;

/**
 * 向后兼容入口（已废弃）。
 *
 * @deprecated 请使用 {@link CvToolApplication} 作为主入口，
 *             加密使用 {@link com.yll.crypto.CryptoService}，
 *             注册表使用 {@link com.yll.registry.RegistryService}。
 */
@Deprecated
public class SpecialTool {

    private SpecialTool() {
    }

    /**
     * @deprecated 使用 {@link CvToolApplication#main(String[])}
     */
    @Deprecated
    public static void main(String[] args) {
        CvToolApplication.main(args);
    }

    /**
     * @deprecated 使用 {@link CryptoService#encode(byte[])}
     */
    @Deprecated
    public static String encode(byte[] content) {
        return CryptoService.encode(content);
    }

    /**
     * @deprecated 使用 {@link CryptoService#decode(String)}
     */
    @Deprecated
    public static byte[] decode(String content) {
        return CryptoService.decode(content);
    }

    /**
     * @deprecated 使用 {@link com.yll.config.AppConfig} 中的常量
     */
    @Deprecated
    @SuppressWarnings("unused")
    public static byte[] generateKey() {
        throw new UnsupportedOperationException(
                "generateKey() 已废弃，请使用 CryptoService 的 encode/decode 方法");
    }

    /**
     * @deprecated 使用 {@link RegistryService#getJavaExePath()}
     */
    @Deprecated
    public static String getJavaExePath() {
        return RegistryService.getJavaExePath();
    }
}