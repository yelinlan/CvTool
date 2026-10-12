package com.yll;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class CvUtil {

    /**
     * 从剪切板获取文字。
     */
    public static String getSysClipboardText() {
        Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (sysClip == null) {
            return "";
        }

        Transferable clipTf = sysClip.getContents(null);
        if (clipTf == null || !clipTf.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return "";
        }

        try {
            Object data = clipTf.getTransferData(DataFlavor.stringFlavor);
            return data == null ? "" : data.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 将字符串复制到剪切板。
     */
    public static void setSysClipboardText(String writeMe) {
        if (writeMe == null) {
            throw new IllegalArgumentException("剪贴板内容不能为空");
        }

        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clip == null) {
            throw new IllegalStateException("当前环境没有可用的系统剪贴板");
        }

        clip.setContents(new StringSelection(writeMe), null);
    }
}
