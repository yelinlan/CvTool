package com.yll;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;

public class SpecialTool {

    // 文件大小上限（20MB）
    private static final long MAX_FILE_SIZE = 1024L * 1024 * 20;
    public static final String KAGOME = "KAGOME";
    public static final String FORMAT = "yyyyMMddHH";

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                // 无参数：让用户选择C部/P部，生成对应reg文件
                chooseSideAndGenerateReg();
            } else if (args.length == 2) {
                // 有参数：根据0/1执行加密/解密逻辑
                String path = args[0];
                String flag = args[1];
                executeByFlag(path, flag);
            } else {
                System.err.println("参数错误！用法：");
                System.err.println("1. 无参数：选择C部/P部（生成reg文件）");
                System.err.println("2. 带参数：java -jar xxx.jar <路径> <0(加密C)/1(解密写入)>");
            }
        } catch (Exception e) {
            System.err.println("程序执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // 无参数时：让用户选择C部/P部，生成对应reg文件
    private static void chooseSideAndGenerateReg() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请选择操作：");
        System.out.println("1. C部（生成添加/删除右键菜单的reg文件）");
        System.out.println("2. P部（生成添加/删除右键菜单的reg文件）");
        System.out.print("输入选项（1/2）：");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                generateCopySideRegFiles();
                System.out.println("C部reg文件生成完成！");
                break;
            case "2":
                generatePasteSideRegFiles();
                System.out.println("P部reg文件生成完成！");
                break;
            default:
                System.err.println("无效选项，请输入1或2！");
        }
        scanner.close();
    }

    // 生成C部的reg文件（添加/删除右键菜单）
    private static void generateCopySideRegFiles() {
        // 1. 添加右键菜单的reg文件
        String addRegContent = "Windows Registry Editor Version 5.00\n\n" +
                "[HKEY_CLASSES_ROOT\\*\\shell\\toBase64Text]\n" +
                "@=\"toText\"\n" +
                "\"Icon\"=\"java.exe\"\n\n" +
                "[HKEY_CLASSES_ROOT\\*\\shell\\toBase64Text\\command]\n" +
                "@=\"\\\"" + getJavaExePath().replace("\\", "\\\\") + "\\\" -jar \\\"" +
                getJarPath() + "\\\" \\\"%1\\\" 0\"\n"; // 关联参数0（加密C）
        writeRegFile("C部-添加右键菜单.reg", addRegContent);


        // 2. 删除右键菜单的reg文件
        String deleteRegContent = "Windows Registry Editor Version 5.00\n\n" +
                "[-HKEY_CLASSES_ROOT\\*\\shell\\toBase64Text]\n";
        writeRegFile("C部-删除右键菜单.reg", deleteRegContent);
    }

    // 生成P部的reg文件（添加/删除右键菜单）
    private static void generatePasteSideRegFiles() {
        // 1. 添加右键菜单的reg文件
        String addRegContent = "Windows Registry Editor Version 5.00\n\n" +
                "[HKEY_CLASSES_ROOT\\*\\shell\\toRealFile]\n" +
                "@=\"toFile\"\n" +
                "\"Icon\"=\"java.exe\"\n\n" +
                "[HKEY_CLASSES_ROOT\\*\\shell\\toRealFile\\command]\n" +
                "@=\"\\\"" + getJavaExePath().replace("\\", "\\\\") + "\\\" -jar \\\"" +
                getJarPath() + "\\\" \\\"%1\\\" 1\"\n"; // 关联参数1（解密写入）
        writeRegFile("P部-添加右键菜单.reg", addRegContent);

        // 2. 删除右键菜单的reg文件
        String deleteRegContent = "Windows Registry Editor Version 5.00\n\n" +
                "[-HKEY_CLASSES_ROOT\\*\\shell\\toRealFile]\n";
        writeRegFile("P部-删除右键菜单.reg", deleteRegContent);
    }

    // 写入reg内容到文件
    private static void writeRegFile(String fileName, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("生成" + fileName + "失败：" + e.getMessage());
        }
    }

    // 获取当前jar包路径（用于reg文件中的命令关联）
    private static String getJarPath() {
        try {
            String path = SpecialTool.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            // 转义反斜杠
            return new File(path).getAbsolutePath().replace("\\", "\\\\");
        } catch (Exception e) {
            throw new RuntimeException("获取当前jar包路径失败：" + e.getMessage());
        }
    }

    // 根据参数0/1执行加密C或解密写入逻辑
    private static void executeByFlag(String path, String flag) throws IOException {
        if ("0".equals(flag)) {
            // 逻辑0：加密文件并C到剪贴板
            encryptAndCopyToClipboard(path);
        } else if ("1".equals(flag)) {
            // 逻辑1：从剪贴板解密并写入文件
            decryptFromClipboardAndWrite(path);
        } else {
            System.err.println("参数错误！flag必须为0（加密C）或1（解密写入）");
        }
    }

    // 加密文件并C到剪贴板
    private static void encryptAndCopyToClipboard(String filePath) throws IOException {
        File file = new File(filePath);
        if (!FileUtil.isFile(filePath)) {
            throw new IllegalArgumentException("路径不是有效文件：" + filePath);
        }
        if (file.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件过大（>20MB）：" + filePath);
        }

        // 加密文件名
        String base64Name = encode(file.getName().getBytes(StandardCharsets.UTF_8));
        // 加密文件内容
        String base64Content = encode(Files.readAllBytes(file.toPath()));

        // 拼接并写入剪贴板（格式：文件名@内容）
        CvUtil.setSysClipboardText(base64Name + "@" + base64Content);
        System.out.println("加密成功，已C到剪贴板：" + filePath);
    }

    public static String encode(byte[] content) {
        AES aes = SecureUtil.aes(generateKey());
        return Base64.getEncoder().encodeToString(aes.encrypt(content));
    }

    public static byte[] decode(String content) {
        AES aes = SecureUtil.aes(generateKey());
        return aes.decrypt(Base64.getDecoder().decode(content));
    }

    // 从剪贴板解密并写入文件
    private static void decryptFromClipboardAndWrite(String targetPath) throws IOException {
        File target = new File(targetPath);
        String targetDir = target.isDirectory() ? targetPath : target.getParent();
        if (targetDir == null) {
            throw new IllegalArgumentException("目标路径无效：" + targetPath);
        }

        // 读取剪贴板内容
        String clipboardText = CvUtil.getSysClipboardText();
        if (clipboardText == null || !clipboardText.contains("@")) {
            throw new IllegalArgumentException("剪贴板内容格式错误（需包含@分隔符）");
        }

        String[] parts = clipboardText.split("@", 2); // 按@分割为两部分
        if (parts.length != 2) {
            throw new IllegalArgumentException("剪贴板内容格式错误（分割后不是两部分）");
        }

        // 解密文件名和内容
        String fileName = new String(decode(parts[0]), StandardCharsets.UTF_8);
        byte[] contentBytes = decode(parts[1]);

        // 写入目标文件
        String fullPath = targetDir + File.separator + fileName;
        FileUtil.writeBytes(contentBytes, fullPath);
        System.out.println("解密成功，已写入文件：" + fullPath);
    }

    public static byte[] generateKey() {
        String hour = DateUtil.format(new Date(), FORMAT);
        return SecureUtil.md5(hour + KAGOME).getBytes(StandardCharsets.UTF_8);
    }

    public static String getJavaExePath() {
        return System.getProperty("java.home") + "\\bin\\java.exe";
    }

}

