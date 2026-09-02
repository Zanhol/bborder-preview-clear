package com.familymeal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.familymeal.entity.Category;
import com.familymeal.entity.Subcategory;
import com.familymeal.mapper.CategoryMapper;
import com.familymeal.mapper.SubcategoryMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类/子分类：后端驱动的可配置数据源（小程序菜单、后台编辑都用它）。
 * 读取无需登录；增删改需 role=admin（后台）。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryMapper categoryMapper;
    private final SubcategoryMapper subcategoryMapper;

    public CategoryController(CategoryMapper categoryMapper, SubcategoryMapper subcategoryMapper) {
        this.categoryMapper = categoryMapper;
        this.subcategoryMapper = subcategoryMapper;
    }

    /** 层级结构：cat -> 子分类列表（含"全部"），供小程序菜单/菜品表单使用 */
    @GetMapping
    public ResponseEntity<?> tree() {
        List<Category> cats = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Category c : cats) {
            List<Subcategory> subs = subcategoryMapper.selectList(
                    new LambdaQueryWrapper<Subcategory>()
                            .eq(Subcategory::getCatKey, c.getCatKey())
                            .orderByAsc(Subcategory::getSort));
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("catKey", c.getCatKey());
            node.put("label", c.getLabel());
            node.put("subcategories", subs.stream().map(s -> {
                String v = (s.getSubKey() != null && !s.getSubKey().isEmpty()) ? s.getSubKey() : s.getLabel();
                return Map.of("label", s.getLabel(), "value", v);
            }).toList());
            tree.add(node);
        }
        return ResponseEntity.ok(tree);
    }

    /** 新增分类（admin） */
    @PostMapping
    public ResponseEntity<?> addCategory(@RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!"admin".equals(request.getAttribute("role")))
            return ResponseEntity.status(403).body(Map.of("error", "无权限"));
        Category c = new Category();
        c.setCatKey(body.get("catKey"));
        c.setLabel(body.get("label"));
        c.setSort(body.get("sort") == null ? 0 : Integer.parseInt(body.get("sort")));
        c.setCatKey(c.getCatKey() == null || c.getCatKey().trim().isEmpty() ? c.getLabel() : c.getCatKey().trim());
        categoryMapper.insert(c);
        return ResponseEntity.ok(Map.of("success", true, "id", c.getId()));
    }

    /** 改分类名（admin） */
    @PutMapping("/{key}/label")
    public ResponseEntity<?> updateLabel(@PathVariable String key,
                                         @RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!"admin".equals(request.getAttribute("role")))
            return ResponseEntity.status(403).body(Map.of("error", "无权限"));
        String label = body.get("label");
        int n = categoryMapper.update(null, new LambdaUpdateWrapper<Category>()
                .eq(Category::getCatKey, key).set(Category::getLabel, label));
        return ResponseEntity.ok(Map.of("success", n > 0));
    }

    /** 改子分类名（admin） */
    @PutMapping("/{key}/sub/{subId}")
    public ResponseEntity<?> updateSubLabel(@PathVariable String key, @PathVariable Long subId,
                                            @RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!"admin".equals(request.getAttribute("role")))
            return ResponseEntity.status(403).body(Map.of("error", "无权限"));
        String label = body.get("label");
        int n = subcategoryMapper.update(null, new LambdaUpdateWrapper<Subcategory>()
                .eq(Subcategory::getId, subId).set(Subcategory::getLabel, label));
        return ResponseEntity.ok(Map.of("success", n > 0));
    }

    /** 新增子分类（admin） */
    @PostMapping("/{key}/sub")
    public ResponseEntity<?> addSub(@PathVariable String key,
                                    @RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!"admin".equals(request.getAttribute("role")))
            return ResponseEntity.status(403).body(Map.of("error", "无权限"));
        Subcategory s = new Subcategory();
        s.setCatKey(key);
        s.setLabel(body.get("label"));
        s.setSort(body.get("sort") == null ? 0 : Integer.parseInt(body.get("sort")));
        subcategoryMapper.insert(s);
        return ResponseEntity.ok(Map.of("success", true, "id", s.getId()));
    }
}
