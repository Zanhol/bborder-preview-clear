package com.familymeal.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 全局 DIY 背景：后台管理员上传图片（9:16），存为 uploads/global-background.jpg，
 * 小程序端启动时通过 GET /api/background 读取。图片持久化在磁盘，重启服务后仍生效。
 */
@RestController
@RequestMapping("/api/background")
public class BackgroundController {

    private final Path uploadDir;

    public BackgroundController(@Value("${app.upload.path:./uploads}") String uploadPath) {
        this.uploadDir = Paths.get(uploadPath);
        try { Files.createDirectories(uploadDir); } catch (IOException e) { }
    }

    private Path file() {
        return uploadDir.resolve("global-background.jpg");
    }

    /** 公开读取全局背景图（未登录可访问） */
    @GetMapping
    public ResponseEntity<?> get() {
        try {
            Path p = file();
            if (!Files.exists(p)) return ResponseEntity.notFound().build();
            org.springframework.core.io.FileSystemResource res = new org.springframework.core.io.FileSystemResource(p);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .contentLength(res.contentLength())
                    .header("Cache-Control", "no-cache")
                    .body(res);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** 后台管理员上传/更新全局背景（multipart image；建议前端先裁剪为 9:16） */
    @PostMapping("/admin/upload")
    public ResponseEntity<?> upload(@RequestParam(value = "image", required = false) MultipartFile image,
                                    HttpServletRequest request) {
        if (!"admin".equals(request.getAttribute("role"))) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限"));
        }
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请上传图片"));
        }
        try {
            BufferedImage src = ImageIO.read(image.getInputStream());
            int w = src.getWidth(), h = src.getHeight();
            // 无客户端裁剪时兜底：按 9:16 居中裁剪并压缩到 1080x1920 以内
            BufferedImage out = fit916(src);
            ImageIO.write(out, "jpg", file().toFile());
            // 直接用 no-cache + 时间戳参数提示前端缓存刷新
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", "/api/background?v=" + System.currentTimeMillis(),
                    "width", out.getWidth(), "height", out.getHeight()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 按 9:16 居中裁剪并缩放到宽 1080 */
    private BufferedImage fit916(BufferedImage src) {
        double targetRatio = 9.0 / 16.0;
        int sw = src.getWidth(), sh = src.getHeight();
        double srcRatio = (double) sw / sh;
        int cropW, cropH;
        if (srcRatio > targetRatio) { cropH = sh; cropW = (int) (sh * targetRatio); }
        else { cropW = sw; cropH = (int) (sw / targetRatio); }
        int x = (sw - cropW) / 2, y = (sh - cropH) / 2;
        BufferedImage cropped = src.getSubimage(x, y, cropW, cropH);
        int outW = Math.min(1080, cropW);
        int outH = (int) (outW / targetRatio);
        BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, outW, outH, null);
        g.dispose();
        return out;
    }
}
