package com.example.multitools.tools.image;

public enum ImageOutputFormat {
    WEBP("WebP", "webp", "webp"),
    PNG("PNG", "png", "png"),
    JPG("JPG", "jpg", "jpeg"),
    ICO("ICO", "ico", "ico");

    private final String label;
    private final String extension;
    private final String imageIoFormat;

    ImageOutputFormat(String label, String extension, String imageIoFormat) {
        this.label = label;
        this.extension = extension;
        this.imageIoFormat = imageIoFormat;
    }

    public String getLabel() {
        return label;
    }

    public String getExtension() {
        return extension;
    }

    String getImageIoFormat() {
        return imageIoFormat;
    }

    @Override
    public String toString() {
        return label;
    }
}
