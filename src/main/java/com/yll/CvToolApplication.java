package com.yll;

import cn.hutool.core.io.FileUtil;
import com.yll.clipboard.ClipboardService;
import com.yll.config.AppConfig;
import com.yll.crypto.CryptoService;
import com.yll.registry.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;

/**
 * CvTool 应用主入口。
 *
 * <p>职责：命令行参数解析、流程编排。具体的加密、剪贴板、注册表操作由各自的 Service 处理。</p>
 */
public final class CvToolApplication {

    private static final Logger log = LoggerFactory.getLogger(CvToolApplication.class);

    private CvToolApplication() {
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                chooseSideAndGenerateReg();
            } else if (args.length == 2) {
                String path = args[0];
                String flag = args[1];
                executeByFlag(path, flag);
            } else {
                System.err.println("参数错误！用法：");
                System.err.println("1. 无参数：选择C部/P部（生成reg文件）");
                System.err.println("2. 带参数：java -jar xxx.jar <路径> <0(加密)/1(解密)>");
            }
        } catch (Exception e) {
            log.error("程序执行失败", e);
            System.err.println("程序执行失败：" + e.getMessage());
        }
    }

    // ======================== 命令行交互 ========================

    /**
     * 无参数时：让用户选择 C 部 / P 部，生成对应 reg 文件。
     */
    private static void chooseSideAndGenerateReg() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请选择操作：");
        System.out.println("1. C部（生成添加/删除右键菜单的reg文件）");
        System.out.println("2. P部（生成添加/删除右键菜单的reg文件）");
        System.out.print("输入选项（1/2）：");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                RegistryService.generateEncryptRegFiles();
                System.out.println("C部 reg 文件生成完成！");
                break;
            case "2":
                RegistryService.generateDecryptRegFiles();
                System.out.println("P部 reg 文件生成完成！");
                break;
            default:
                System.err.println("无效选项，请输入1或2！");
        }
        scanner.close();
    }

    // ======================== 核心业务逻辑 ========================

    /**
     * 根据 flag 执行加密或解密。
     *
     * @param path 文件 / 文件夹路径
     * @param flag "0"=加密并复制到剪贴板，"1"=从剪贴板解密并写入文件
     */
    private static void executeByFlag(String path, String flag) {
        if ("0".equals(flag)) {
            encryptAndCopyToClipboard(path);
        } else if ("1".equals(flag)) {
            decryptFromClipboardAndWrite(path);
        } else {
            System.err.println("参数错误！flag必须为0（加密）或1（解密）");
        }
    }

    /**
     * 加密文件并复制到剪贴板。
     */
    private static void encryptAndCopyToClipboard(String filePath) {
        File file = new File(filePath);
        if (!FileUtil.isFile(filePath)) {
            throw new IllegalArgumentException("路径不是有效文件：" + filePath);
        }
        if (file.length() > AppConfig.MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("文件过大（%dMB > %dMB限制）：%s",
                            file.length() / 1024 / 1024, AppConfig.MAX_FILE_SIZE / 1024 / 1024, filePath));
        }

        try {
            // 加密文件名
            String encodedName = CryptoService.encode(file.getName().getBytes(StandardCharsets.UTF_8));
            // 加密文件内容
            String encodedContent = CryptoService.encode(Files.readAllBytes(file.toPath()));

            // 拼接并写入剪贴板
            ClipboardService.setText(encodedName + AppConfig.CLIPBOARD_SEPARATOR + encodedContent);
            System.out.println("加密成功，已复制到剪贴板：" + filePath);
        } catch (Exception e) {
            log.error("加密文件失败: {}", filePath, e);
            System.err.println("加密文件失败：" + e.getMessage());
        }
    }

    /**
     * 从剪贴板解密并写入文件。
     */
    private static void decryptFromClipboardAndWrite(String targetPath) {
        File target = new File(targetPath);
        String targetDir = target.isDirectory() ? targetPath : target.getParent();
        if (targetDir == null) {
            throw new IllegalArgumentException("目标路径无效：" + targetPath);
        }

        // 读取剪贴板内容
        String clipboardText = ClipboardService.getText();
        if (clipboardText == null || !clipboardText.contains(AppConfig.CLIPBOARD_SEPARATOR)) {
            throw new IllegalArgumentException("剪贴板内容格式错误（需包含分隔符 '::'）");
        }

        String[] parts = clipboardText.split(AppConfig.CLIPBOARD_SEPARATOR, 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("剪贴板内容格式错误（分割后不是两部分）");
        }

        try {
            // 解密文件名和内容
            String fileName = new String(CryptoService.decode(parts[0]), StandardCharsets.UTF_8);
            byte[] contentBytes = CryptoService.decode(parts[1]);

            // 写入目标文件
            String fullPath = targetDir + File.separator + fileName;
            FileUtil.writeBytes(contentBytes, fullPath);
            System.out.println("解密成功，已写入文件：" + fullPath);
        } catch (Exception e) {
            log.error("解密文件失败", e);
            System.err.println("解密文件失败：" + e.getMessage());
        }
    }
}