package com.ehapi.objects;

/**
 * 图片代理输出格式。
 * Output image formats supported by the image proxy service.
 */
public enum EhImageFormat {
    /** 默认 JPEG 格式 / Default JPEG format */
    JPEG("jpeg"),
    /** PNG 无损格式 / PNG lossless format */
    PNG("png"),
    /** LVGL 嵌入式预解码二进制格式 / LVGL pre-decoded binary format */
    LVGL("lvgl");

    private final String value;

    EhImageFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EhImageFormat fromString(String text) {
        if (text == null) return JPEG;
        for (EhImageFormat format : values()) {
            if (format.value.equalsIgnoreCase(text) || format.name().equalsIgnoreCase(text)) {
                return format;
            }
        }
        return JPEG;
    }
}
