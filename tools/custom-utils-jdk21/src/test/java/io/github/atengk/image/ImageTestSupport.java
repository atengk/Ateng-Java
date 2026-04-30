package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

final class ImageTestSupport {

    private ImageTestSupport() {
    }

    static BufferedImage image(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    static BufferedImage argbImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    static BufferedImage mixedImage() {
        BufferedImage image = image(8, 8, Color.WHITE);
        image.setRGB(0, 0, Color.BLACK.getRGB());
        image.setRGB(7, 7, Color.RED.getRGB());
        return image;
    }

    static byte[] pngBytes(BufferedImage image) {
        return ImageUtil.toBytes(image, "png");
    }

    static Path write(Path directory, String filename, BufferedImage image, String format) {
        Path path = directory.resolve(filename);
        ImageUtil.write(image, format, path);
        return path;
    }
}
