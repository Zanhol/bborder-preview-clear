package com.familymeal.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * 一次性工具：为 uploads/ 下所有图片生成 200x200 缩略图 (_thumb.jpg)
 * 运行方式: 编译后在 backend 目录下执行
 *   java -cp target/classes com.familymeal.util.ThumbnailGenerator ./uploads
 */
public class ThumbnailGenerator {

    private static final int THUMB_SIZE = 200;

    public static void main(String[] args) throws IOException {
        Path uploadDir = Paths.get(args.length > 0 ? args[0] : "./uploads");
        if (!Files.isDirectory(uploadDir)) {
            System.out.println("目录不存在: " + uploadDir.toAbsolutePath());
            System.exit(1);
        }

        int generated = 0;
        int skipped = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDir)) {
            for (Path file : stream) {
                String name = file.getFileName().toString().toLowerCase();
                // 跳过缩略图文件本身
                if (name.contains("_thumb")) continue;
                // 只处理图片
                if (!name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".png")) continue;

                // 对应的缩略图路径
                String baseName = file.getFileName().toString();
                String thumbName = baseName + "_thumb.jpg";
                Path thumbPath = uploadDir.resolve(thumbName);

                if (Files.exists(thumbPath)) {
                    skipped++;
                    continue;
                }

                try {
                    BufferedImage src = ImageIO.read(file.toFile());
                    if (src == null) {
                        System.out.println("  [跳过] 无法读取: " + baseName);
                        continue;
                    }

                    BufferedImage thumb = cropSquare(resize(src,
                        Math.max(THUMB_SIZE, Math.min(src.getWidth(), src.getHeight()))));
                    ImageIO.write(thumb, "jpg", thumbPath.toFile());
                    generated++;
                    System.out.println("  [OK] " + baseName + " → " + thumbName
                        + " (" + src.getWidth() + "x" + src.getHeight() + " → " + THUMB_SIZE + "x" + THUMB_SIZE + ")");
                } catch (Exception e) {
                    System.out.println("  [失败] " + baseName + ": " + e.getMessage());
                }
            }
        }

        System.out.println();
        System.out.println("===== 完成 =====");
        System.out.println("生成: " + generated + " 张");
        System.out.println("跳过(已有): " + skipped + " 张");
    }

    private static BufferedImage resize(BufferedImage src, int maxWidth) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxWidth) return src;
        double ratio = (double) maxWidth / w;
        int nw = maxWidth, nh = (int) (h * ratio);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private static BufferedImage cropSquare(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int size = Math.min(w, h);
        int x = (w - size) / 2, y = (h - size) / 2;
        BufferedImage out = new BufferedImage(THUMB_SIZE, THUMB_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, THUMB_SIZE, THUMB_SIZE, x, y, x + size, y + size, null);
        g.dispose();
        return out;
    }
}
