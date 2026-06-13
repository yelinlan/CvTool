package com.yll.config;

/**
 * 应用配置常量。
 *
 * <p>集中管理所有硬编码常量，便于后续外部化配置。</p>
 */
public final class AppConfig {

    private AppConfig() {
        // 工具类禁止实例化
    }

    // ==================== 加密相关 ====================

    /**
     * AES 密钥派生使用的盐值。
     */
    public static final String KEY_SALT = "KAGOME";

    /**
     * 密钥时间格式（与 {@link cn.hutool.core.date.DateUtil} 配合使用）。
     */
    public static final String KEY_TIME_FORMAT = "yyyyMMddHH";

    // ==================== 文件相关 ====================

    /**
     * 单文件大小上限（20MB）。
     */
    public static final long MAX_FILE_SIZE = 1024L * 1024 * 20;

    /**
     * 剪贴板内容分隔符，用于分隔加密后的文件名和文件内容。
     * 使用双冒号避免与 Base64 编码冲突。
     */
    public static final String CLIPBOARD_SEPARATOR = "::";

    // ==================== 注册表菜单相关 ====================

    /**
     * 加密操作的右键菜单名称。
     */
    public static final String MENU_NAME_ENCRYPT = "toText";

    /**
     * 解密操作的右键菜单名称。
     */
    public static final String MENU_NAME_DECRYPT = "toFile";
}