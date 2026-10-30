package com.health.management.storage;

import com.health.management.common.exception.ServiceException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 给打卡照片添加服务端时间水印。
 * 注意：水印证明的是服务端收到照片的时间，不能证明照片的实际拍摄时间。
 */
@Service
public class ImageWatermarkService {

    /** 防止超大图片导致内存占用过高 */
    private static final int MAX_DIMENSION = 8000;
    private static final long MAX_PIXELS = 40_000_000L;

    /**
     * @param original 原始图片字节
     * @param text     水印文本
     * @param format   jpg/jpeg/png
     * @return 加水印后的图片字节
     */
    public byte[] watermark(byte[] original, String text, String format) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException e) {
            throw ServiceException.badRequest("图片读取失败，请更换图片");
        }
        if (image == null) {
            throw ServiceException.badRequest("不支持的图片格式，仅支持 JPG/PNG");
        }
        if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION
                || (long) image.getWidth() * image.getHeight() > MAX_PIXELS) {
            throw ServiceException.badRequest("图片尺寸过大");
        }

        int fontSize = Math.max(16, Math.min(48, image.getWidth() / 35));
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            FontMetrics metrics = graphics.getFontMetrics();
            int textWidth = metrics.stringWidth(text);
            int textHeight = metrics.getHeight();
            int x = Math.max(10, image.getWidth() - textWidth - 20);
            int y = Math.max(10, image.getHeight() - textHeight - 20);

            graphics.setColor(new Color(0, 0, 0, 120));
            graphics.fillRoundRect(x - 10, y - 6, textWidth + 20, textHeight + 12, 10, 10);
            graphics.setColor(Color.WHITE);
            graphics.drawString(text, x, y + metrics.getAscent());
        } finally {
            graphics.dispose();
        }

        String outputFormat = "png".equalsIgnoreCase(format) ? "png" : "jpg";
        if ("jpg".equals(outputFormat) && image.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D rgbGraphics = rgb.createGraphics();
            try {
                rgbGraphics.setColor(Color.WHITE);
                rgbGraphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                rgbGraphics.drawImage(image, 0, 0, null);
            } finally {
                rgbGraphics.dispose();
            }
            image = rgb;
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            boolean written = ImageIO.write(image, outputFormat, output);
            if (!written) {
                throw ServiceException.fail("图片水印处理失败");
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw ServiceException.fail("图片水印处理失败：" + e.getMessage());
        }
    }
}
