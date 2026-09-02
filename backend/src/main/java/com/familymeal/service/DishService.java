package com.familymeal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymeal.entity.Dish;
import com.familymeal.mapper.DishMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class DishService {

    private final DishMapper dishMapper;
    private final Path uploadDir;

    private static final int THUMB_SIZE = 200;

    public DishService(DishMapper dishMapper, @Value("${app.upload.path:./uploads}") String uploadPath) {
        this.dishMapper = dishMapper;
        this.uploadDir = Paths.get(uploadPath);
        try { Files.createDirectories(uploadDir); }
        catch (IOException e) { throw new RuntimeException("无法创建上传目录", e); }
    }


    /** 分页返回 active 菜品（含总条数），用于前端上拉加载更多。
     *  subcategory 非空时按子分类过滤；ownerFilter 非空时按归属过滤（该做饭人的菜 + 公用菜）。 */
    public java.util.Map<String, Object> listActivePage(int page, int size, String subcategory, String ownerFilter) {
        LambdaQueryWrapper<Dish> w = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, "active");
        if (subcategory != null && !subcategory.isEmpty()) {
            w.eq(Dish::getSubcategory, subcategory);
        }
        if (ownerFilter != null && !ownerFilter.isEmpty()) {
            w.in(Dish::getOwner, ownerFilter, "both");
        }
        Long total = dishMapper.selectCount(w);
        int offset = (page - 1) * size;
        List<Dish> items = dishMapper.selectList(
                new LambdaQueryWrapper<Dish>().eq(Dish::getStatus, "active")
                        .eq(subcategory != null && !subcategory.isEmpty(), Dish::getSubcategory, subcategory)
                        .in(ownerFilter != null && !ownerFilter.isEmpty(), Dish::getOwner, ownerFilter, "both")
                        .orderByDesc(Dish::getCreatedAt)
                        .orderByDesc(Dish::getId)
                        .last("LIMIT " + offset + "," + size));
        return java.util.Map.of("items", items, "total", total == null ? 0L : total);
    }


    public List<Dish> listActive() {
        return dishMapper.selectList(
                new LambdaQueryWrapper<Dish>().eq(Dish::getStatus, "active").orderByDesc(Dish::getCreatedAt));
    }

    private String saveImage(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) return "";

        // 读入图片
        BufferedImage src = ImageIO.read(image.getInputStream());
        if (src == null) return "";

        String base = UUID.randomUUID().toString();

        // 存原图（压缩到 1200px 宽）
        BufferedImage full = src.getWidth() > 1200 ? resize(src, 1200) : src;
        ImageIO.write(full, "jpg", uploadDir.resolve(base + ".jpg").toFile());

        // 存缩略图（200x200 正方形裁剪居中）
        BufferedImage thumb = cropSquare(resize(src, Math.max(THUMB_SIZE, Math.min(src.getWidth(), src.getHeight()))));
        ImageIO.write(thumb, "jpg", uploadDir.resolve(base + "_thumb.jpg").toFile());

        return base;
    }

    private BufferedImage resize(BufferedImage src, int maxWidth) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxWidth) return src;
        double ratio = (double) maxWidth / w;
        int nw = maxWidth;
        int nh = (int) (h * ratio);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private BufferedImage cropSquare(BufferedImage src) {
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

    private void deleteImages(String base) {
        if (base == null || base.isEmpty()) return;
        try { Files.deleteIfExists(uploadDir.resolve(base + ".jpg")); } catch (IOException ignored) {}
        try { Files.deleteIfExists(uploadDir.resolve(base + "_thumb.jpg")); } catch (IOException ignored) {}
        try { Files.deleteIfExists(uploadDir.resolve(base)); } catch (IOException ignored) {}
    }

    public Dish add(String name, String description, String spiciness, String category, String subcategory, String owner, MultipartFile image) throws IOException {
        Dish dish = new Dish();
        dish.setName(name);
        dish.setDescription(description != null ? description : "");
        dish.setSpiciness(spiciness != null ? spiciness : "none");
        dish.setCategory(category != null ? category : "cooking");
        dish.setSubcategory(subcategory != null ? subcategory : "");
        dish.setOwner(owner != null && !owner.isEmpty() ? owner : "wife");
        dish.setStatus("active");
        dish.setImagePath(saveImage(image));
        dishMapper.insert(dish);
        return dish;
    }

    public Dish update(Long id, String name, String description, String spiciness, String category, String subcategory, String owner, MultipartFile image) throws IOException {
        Dish dish = dishMapper.selectById(id);
        if (dish == null || "deleted".equals(dish.getStatus())) throw new RuntimeException("菜品不存在");
        dish.setName(name);
        dish.setDescription(description != null ? description : "");
        dish.setSpiciness(spiciness != null ? spiciness : "none");
        dish.setCategory(category != null ? category : "cooking");
        dish.setSubcategory(subcategory != null ? subcategory : "");
        if (owner != null && !owner.isEmpty()) {
            dish.setOwner(owner);
        }

        if (image != null && !image.isEmpty()) {
            deleteImages(dish.getImagePath());
            dish.setImagePath(saveImage(image));
        }
        dishMapper.updateById(dish);
        return dish;
    }

    public void delete(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish != null) { deleteImages(dish.getImagePath()); }
        Dish d = new Dish(); d.setId(id); d.setStatus("deleted");
        dishMapper.updateById(d);
    }

    public Path getImagePath(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null || dish.getImagePath() == null || dish.getImagePath().isEmpty()) return null;
        String base = dish.getImagePath();
        // 新格式: 老数据有扩展名, 新数据只有 base UUID
        Path p = uploadDir.resolve(base + ".jpg");
        if (Files.exists(p)) return p;
        // 老数据兼容: imagePath 包含扩展名
        p = uploadDir.resolve(base);
        return Files.exists(p) ? p : null;
    }

    public Path getThumbPath(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null || dish.getImagePath() == null || dish.getImagePath().isEmpty()) return null;
        String base = dish.getImagePath();
        // 先找缩略图
        Path p = uploadDir.resolve(base + "_thumb.jpg");
        if (Files.exists(p)) return p;
        // 没缩略图就返回原图（老数据）
        return getImagePath(id);
    }
}
