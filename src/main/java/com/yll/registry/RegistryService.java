package com.yll.registry;

import cn.hutool.core.io.FileUtil;
import com.yll.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Windows 注册表文件生成服务。
 *
 * <p>生成右键菜单所需的 .reg 文件，支持添加 / 删除菜单项。</p>
 */
public final class RegistryService {

    private static final Logger log = LoggerFactory.getLogger(RegistryService.class);

    private RegistryService() {
    }

    // ======================== 公开 API ========================

    /**
     * 生成 C 端（加密端）的注册表文件。
     *
     * @return 生成的文件路径
     */
    public static String generateEncryptRegFiles() {
        String addReg = buildAddReg(
                "HKEY_CLASSES_ROOT\\*\\shell\\toBase64Text",
                AppConfig.MENU_NAME_ENCRYPT,
                "0"
        );
        writeRegFile("C部-添加右键菜单.reg", addReg);
        log.info("C部 reg 文件生成完成");

        String delReg = buildDeleteReg("HKEY_CLASSES_ROOT\\*\\shell\\toBase64Text");
        writeRegFile("C部-删除右键菜单.reg", delReg);

        return "C部-添加右键菜单.reg";
    }

    /**
     * 生成 P 端（解密端）的注册表文件。
     *
     * @return 生成的文件路径
     */
    public static String generateDecryptRegFiles() {
        String addReg = buildAddReg(
                "HKEY_CLASSES_ROOT\\*\\shell\\toRealFile",
                AppConfig.MENU_NAME_DECRYPT,
                "1"
        );
        writeRegFile("P部-添加右键菜单.reg", addReg);
        log.info("P部 reg 文件生成完成");

        String delReg = buildDeleteReg("HKEY_CLASSES_ROOT\\*\\shell\\toRealFile");
        writeRegFile("P部-删除右键菜单.reg", delReg);

        return "P部-添加右键菜单.reg";
    }

    // ======================== 内部方法 ========================

    /**
     * 构建"添加右键菜单"的 reg 内容。
     */
    private static String buildAddReg(String keyPath, String menuName, String flag) {
        String javaExe = getJavaExePath().replace("\\", "\\\\");
        String jarPath = getJarPath();

        return "Windows Registry Editor Version 5.00\n\n"
                + "[" + keyPath + "]\n"
                + "@=\"" + menuName + "\"\n"
                + "\"Icon\"=\"java.exe\"\n\n"
                + "[" + keyPath + "\\command]\n"
                + "@=\"\\\"" + javaExe + "\\\" -jar \\\"" + jarPath + "\\\" \\\"%1\\\" " + flag + "\"\n";
    }

    /**
     * 构建"删除右键菜单"的 reg 内容。
     */
    private static String buildDeleteReg(String keyPath) {
        return "Windows Registry Editor Version 5.00\n\n"
                + "[-" + keyPath + "]\n";
    }

    /**
     * 写入 reg 文件。
     */
    private static void writeRegFile(String fileName, String content) {
        try {
            FileUtil.writeUtf8String(content, fileName);
            log.debug("reg 文件写入成功: {}", fileName);
        } catch (Exception e) {
            log.error("生成 reg 文件失败: {}", fileName, e);
        }
    }

    /**
     * 获取当前 jar 包路径。
     */
    static String getJarPath() {
        try {
            String path = RegistryService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            return new File(path).getAbsolutePath().replace("\\", "\\\\");
        } catch (Exception e) {
            throw new RuntimeException("获取 jar 包路径失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取 java.exe 路径（Windows）。
     */
    public static String getJavaExePath() {
        return System.getProperty("java.home") + "\\bin\\java.exe";
    }
}