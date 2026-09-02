-- 修复分类/子分类：补 sub_key（对应 dishes.subcategory 英文值）并重播正确中文
DELETE FROM subcategories;
DELETE FROM categories;

-- 若 subcategories 缺 sub_key 列则补（幂等通过 information_schema 判断）
SET @sk_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='subcategories' AND COLUMN_NAME='sub_key');
SET @sk_ddl := IF(@sk_exists=0,
  'ALTER TABLE subcategories ADD COLUMN sub_key VARCHAR(32) NOT NULL DEFAULT '''' AFTER cat_key',
  'SELECT 1');
PREPARE sk_stmt FROM @sk_ddl; EXECUTE sk_stmt; DEALLOCATE PREPARE sk_stmt;

INSERT INTO categories (cat_key, label, sort) VALUES
 ('cooking','买菜做饭',1),
 ('delivery','懒人外卖',2);

INSERT INTO subcategories (cat_key, sub_key, label, sort) VALUES
 ('cooking','','全部',0),
 ('cooking','breakfast','早餐',1),
 ('cooking','meat','肉类',2),
 ('cooking','vegetable','蔬菜类',3),
 ('cooking','seafood','海鲜类',4),
 ('cooking','soup','汤品',5),
 ('cooking','snack','小吃',6),
 ('cooking','staple','主食',7),
 ('cooking','drink','饮品',8);
