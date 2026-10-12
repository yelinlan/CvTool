package com.yll;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

public class SpecialTool {

    private static final long MAX_FILE_SIZE = 1024L * 1024 * 20;
    private static final String KAGOME = "KAGOME";
    private static final String FORMAT = "yyyyMMddHH";
    private static final String CLIPBOARD_SEPARATOR = "@";

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                chooseSideAndGenerateReg();
            } else if (args.length == 2) {
                executeByFlag(args[0], args[1]);
            } else {
                printUsage();
            }
        } catch (Exception e) {
            System.err.println("程序执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.err.println("参数错误！用法：");
        System.err.println("1. 无参数：选择C部/P部（生成reg文件）");
        System.err.println("2. 带参数：java -jar xxx.jar <路径> <0(加密C)/1(解密写入)>");
    }

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
    }

    private static void generateCopySideRegFiles() {
        String addRegContent = buildRegistryContent("toBase64Text", "toText", "0");
        String deleteRegContent = "Windows Registry Editor Version 5.00\n\n[-HKEY_CLASSES_ROOT\\*\\shell\\toBase64Text]\n";
        writeRegFile("C部-添加右键菜单.reg", addRegContent);
        writeRegFile("C部-删除右键菜单.reg", deleteRegContent);
    }

    private static void generatePasteSideRegFiles() {
        String addRegContent = buildRegistryContent("toRealFile", "toFile", "1");
        String deleteRegContent = "Windows Registry Editor Version 5.00\n\n[-HKEY_CLASSES_ROOT\\*\\shell\\toRealFile]\n";
        writeRegFile("P部-添加右键菜单.reg", addRegContent);
        writeRegFile("P部-删除右键菜单.reg", deleteRegContent);
    }

    private static String buildRegistryContent(String shellName, String menuName, String flag) {
        String javaExePath = escapeWindowsPath(getJavaExePath());
        String jarPath = escapeWindowsPath(getJarPath());

        return "Windows Registry Editor Version 5.00\n\n" +
                "[HKEY_CLASSES_ROOT\\*\\shell\\" + shellName + "]\n" +
                "@=\"" + menuName + "\"\n" +
                "\"Icon\"=\"java.exe\"\n\n" +
                "[HKEY_CLASSES_ROOT\\*\\shell\\" + shellName + "\\command]\n" +
                "@=\"\\\"" + javaExePath + "\\\" -jar \\\"" + jarPath + "\\\" \\\"%1\\\" " + flag + "\"\n";
    }

    private static String escapeWindowsPath(String path) {
        return path == null ? "" : path.replace("\\", "\\\\");
    }

    private static void writeRegFile(String fileName, String content) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8)) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("生成" + fileName + "失败：" + e.getMessage());
        }
    }

    private static String getJarPath() {
        try {
            return Paths.get(SpecialTool.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException("获取当前jar包路径失败：" + e.getMessage(), e);
        }
    }

    private static void executeByFlag(String path, String flag) throws IOException {
        if ("0".equals(flag)) {
            encryptAndCopyToClipboard(path);
        } else if ("1".equals(flag)) {
            decryptFromClipboardAndWrite(path);
        } else {
            throw new IllegalArgumentException("参数错误！flag必须为0（加密C）或1（解密写入）");
        }
    }

    private static void encryptAndCopyToClipboard(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.isFile()) {
            throw new IllegalArgumentException("路径不是有效文件：" + filePath);
        }

        if (file.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件过大（>20MB）：" + filePath);
        }

        String base64Name = encode(file.getName().getBytes(StandardCharsets.UTF_8));
        String base64Content = encode(Files.readAllBytes(file.toPath()));
        CvUtil.setSysClipboardText(base64Name + CLIPBOARD_SEPARATOR + base64Content);
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

    private static void decryptFromClipboardAndWrite(String targetPath) throws IOException {
        File target = new File(targetPath);
        File targetDir = target.isDirectory() ? target : target.getParentFile() == null ? new File(".") : target.getParentFile();
        ensureDirectoryExists(targetDir);

        String clipboardText = CvUtil.getSysClipboardText();
        if (clipboardText == null || clipboardText.trim().isEmpty() || !clipboardText.contains(CLIPBOARD_SEPARATOR)) {
            throw new IllegalArgumentException("剪贴板内容格式错误（需包含@分隔符）");
        }

        String[] parts = clipboardText.split(CLIPBOARD_SEPARATOR, 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new IllegalArgumentException("剪贴板内容格式错误（分割后不是两部分）");
        }

        String fileName = new String(decode(parts[0]), StandardCharsets.UTF_8);
        byte[] contentBytes = decode(parts[1]);

        String fullPath = new File(targetDir, fileName).getAbsolutePath();
        FileUtil.writeBytes(contentBytes, fullPath);
        System.out.println("解密成功，已写入文件：" + fullPath);
    }

    private static void ensureDirectoryExists(File directory) {
        if (directory == null) {
            throw new IllegalArgumentException("目标目录为空");
        }

        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("无法创建目录：" + directory.getAbsolutePath());
        }
    }

    public static byte[] generateKey() {
        String hour = DateUtil.format(new Date(), FORMAT);
        return SecureUtil.md5(hour + KAGOME).getBytes(StandardCharsets.UTF_8);
    }

    public static String getJavaExePath() {
        String javaHome = System.getProperty("java.home");
        String executableName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        return Paths.get(javaHome, "bin", executableName).toString();
    }
}
