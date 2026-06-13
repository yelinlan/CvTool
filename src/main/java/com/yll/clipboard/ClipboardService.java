package com.yll.clipboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/**
 * 系统剪贴板交互服务。
 *
 * <p>封装 {@code java.awt.datatransfer}，提供读取/写入字符串的便捷方法。</p>
 */
public final class ClipboardService {

    private static final Logger log = LoggerFactory.getLogger(ClipboardService.class);

    private ClipboardService() {
    }

    /**
     * 从剪贴板读取文本。
     *
     * @return 剪贴板中的文本内容，如果无文本则返回空字符串
     */
    public static String getText() {
        String ret = "";
        Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable clipTf = sysClip.getContents(null);

        if (clipTf != null && clipTf.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                ret = (String) clipTf.getTransferData(DataFlavor.stringFlavor);
                log.debug("读取剪贴板文本，长度={}", ret.length());
            } catch (Exception e) {
                log.error("读取剪贴板异常", e);
            }
        }
        return ret;
    }

    /**
     * 将文本写入剪贴板。
     *
     * @param text 要写入的文本
     */
    public static void setText(String text) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable tText = new StringSelection(text);
        clip.setContents(tText, null);
        log.debug("写入剪贴板文本，长度={}", text.length());
    }
}