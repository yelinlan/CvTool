package com.yll;

import com.yll.clipboard.ClipboardService;

/**
 * 向后兼容的剪贴板工具类（已废弃）。
 *
 * @deprecated 请使用 {@link com.yll.clipboard.ClipboardService}
 */
@Deprecated
public class CvUtil {

    private CvUtil() {
    }

    /**
     * @deprecated 使用 {@link ClipboardService#getText()}
     */
    @Deprecated
    public static String getSysClipboardText() {
        return ClipboardService.getText();
    }

    /**
     * @deprecated 使用 {@link ClipboardService#setText(String)}
     */
    @Deprecated
    public static void setSysClipboardText(String writeMe) {
        ClipboardService.setText(writeMe);
    }
}