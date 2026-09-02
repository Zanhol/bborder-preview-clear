package com.familymeal.controller;

import com.familymeal.entity.Dish;
import com.familymeal.service.CookService;
import com.familymeal.service.DishService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private final DishService dishService;
    private final CookService cookService;

    public DishController(DishService dishService, CookService cookService) {
        this.dishService = dishService;
        this.cookService = cookService;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "subcategory", required = false) String subcategory,
            @RequestParam(value = "ownerFilter", required = false) String ownerFilter) {
        return ResponseEntity.ok(dishService.listActivePage(page, size, subcategory, ownerFilter));
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestParam("name") String name,
                                  @RequestParam(value = "description", defaultValue = "") String description,
                                  @RequestParam(value = "spiciness", defaultValue = "none") String spiciness,
                                  @RequestParam(value = "category", defaultValue = "cooking") String category,
                                  @RequestParam(value = "subcategory", defaultValue = "") String subcategory,
                                  @RequestParam(value = "image", required = false) MultipartFile image,
                                  HttpServletRequest request) {
        if (!cookService.isCook((String) request.getAttribute("role"))) {
            return ResponseEntity.status(403).body(Map.of("error", "仅今日做饭人可管理菜品"));
        }
        try {
            Dish dish = dishService.add(name, description, spiciness, category, subcategory, (String) request.getAttribute("role"), image);
            return ResponseEntity.ok(dish);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @RequestParam("name") String name,
                                     @RequestParam(value = "description", defaultValue = "") String description,
                                     @RequestParam(value = "spiciness", defaultValue = "none") String spiciness,
                                     @RequestParam(value = "category", defaultValue = "cooking") String category,
                                     @RequestParam(value = "subcategory", defaultValue = "") String subcategory,
                                     @RequestParam(value = "owner", required = false) String owner,
                                     @RequestParam(value = "image", required = false) MultipartFile image,
                                     HttpServletRequest request) {
        if (!cookService.isCook((String) request.getAttribute("role"))) {
            return ResponseEntity.status(403).body(Map.of("error", "仅今日做饭人可管理菜品"));
        }
        try {
            Dish dish = dishService.update(id, name, description, spiciness, category, subcategory, owner, image);
            return ResponseEntity.ok(dish);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!cookService.isCook((String) request.getAttribute("role"))) {
            return ResponseEntity.status(403).body(Map.of("error", "仅今日做饭人可管理菜品"));
        }
        dishService.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<?> getImage(@PathVariable Long id) {
        try {
            java.nio.file.Path path = dishService.getImagePath(id);
            if (path == null) return ResponseEntity.notFound().build();
            org.springframework.core.io.FileSystemResource resource =
                new org.springframework.core.io.FileSystemResource(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/thumb")
    public ResponseEntity<?> getThumb(@PathVariable Long id) {
        try {
            java.nio.file.Path path = dishService.getThumbPath(id);
            if (path == null) return getImage(id);
            org.springframework.core.io.FileSystemResource resource =
                new org.springframework.core.io.FileSystemResource(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (Exception e) {
            return getImage(id);
        }
    }
}
