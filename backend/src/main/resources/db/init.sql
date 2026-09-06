CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) DEFAULT NULL COMMENT '微信openid',
    role VARCHAR(16) NOT NULL COMMENT 'husband/wife/husband_test/wife_test',
    nickname VARCHAR(64) DEFAULT NULL,
    avatar_url VARCHAR(255) DEFAULT NULL,
    background_url VARCHAR(512) DEFAULT '' COMMENT '用户DIY的小程序背景',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

INSERT IGNORE INTO users (id, role, nickname) VALUES (1, 'husband', '老公');
INSERT IGNORE INTO users (id, role, nickname) VALUES (2, 'wife', '老婆');

CREATE TABLE IF NOT EXISTS cook_today (
    id BIGINT PRIMARY KEY COMMENT '固定 1，单行记录',
    cook_who VARCHAR(16) NOT NULL DEFAULT 'wife' COMMENT '今日做饭人: husband/wife',
    cook_status VARCHAR(16) NOT NULL DEFAULT 'cooking' COMMENT '做饭状态: cooking/done',
    switched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    switched_by BIGINT DEFAULT NULL COMMENT '切换者用户ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='今日做饭人（动态角色）';

-- 迁移：旧表补 cook_status 列（幂等）
SET @cs_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='cook_today' AND COLUMN_NAME='cook_status');
SET @cs_ddl := IF(@cs_exists=0,
  'ALTER TABLE cook_today ADD COLUMN cook_status VARCHAR(16) NOT NULL DEFAULT ''cooking'' COMMENT ''做饭状态: cooking/done''',
  'SELECT 1');
PREPARE cs_stmt FROM @cs_ddl; EXECUTE cs_stmt; DEALLOCATE PREPARE cs_stmt;

-- 默认今日做饭人=老婆；切换后 INSERT IGNORE 不会覆盖已存在的行
INSERT IGNORE INTO cook_today (id, cook_who) VALUES (1, 'wife');

CREATE TABLE IF NOT EXISTS dishes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL COMMENT '菜名',
    description VARCHAR(512) DEFAULT '' COMMENT '简单描述',
    image_path VARCHAR(255) DEFAULT '' COMMENT '图片本地路径',
    spiciness VARCHAR(16) DEFAULT 'none' COMMENT '辣度: none/male_baby/female_baby',
    category VARCHAR(16) DEFAULT 'cooking' COMMENT '分类: cooking/delivery',
    subcategory VARCHAR(32) DEFAULT '' COMMENT '子分类(cooking): breakfast/meat/vegetable/seafood/soup/snack/staple/drink',
    owner VARCHAR(16) NOT NULL DEFAULT 'wife' COMMENT '归属: husband/wife/both（决定出现在谁的菜单）',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active 或 deleted',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '下单用户ID',
    order_number INT DEFAULT NULL COMMENT '连续编号',
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending 或 received',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    dish_id BIGINT NOT NULL COMMENT '菜品ID',
    dish_name VARCHAR(128) NOT NULL COMMENT '冗余菜名',
    spiciness VARCHAR(16) DEFAULT 'none' COMMENT '辣度',
    quantity INT NOT NULL DEFAULT 1,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 说明：dishes 的 subcategory 列已随上方 CREATE TABLE 定义。若为既有库（dishes 表已存在，
-- CREATE 不会重建），需手工执行一次：ALTER TABLE dishes ADD COLUMN subcategory VARCHAR(32) DEFAULT '' NULL AFTER category;
-- 注：MySQL 原生不支持 ADD COLUMN IF NOT EXISTS，故此处不写自动迁移。

-- 迁移：既有库的 dishes 表补 owner 列（新库已含该列，此段自动跳过）
SET @owner_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dishes' AND COLUMN_NAME = 'owner');
SET @owner_ddl := IF(@owner_exists = 0,
    'ALTER TABLE dishes ADD COLUMN owner VARCHAR(16) NOT NULL DEFAULT ''wife'' COMMENT ''归属: husband/wife/both'' AFTER subcategory',
    'SELECT 1');
PREPARE owner_stmt FROM @owner_ddl;
EXECUTE owner_stmt;
DEALLOCATE PREPARE owner_stmt;

-- 注意：不再插入演示种子菜，系统从空库启动，菜品由做饭人自行录入。

-- 迁移：既有库的 users 表补 background_url 列（新库已含，此段自动跳过）
SET @bg_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'background_url');
SET @bg_ddl := IF(@bg_exists = 0,
    'ALTER TABLE users ADD COLUMN background_url VARCHAR(512) DEFAULT '''' AFTER avatar_url',
    'SELECT 1');
PREPARE bg_stmt FROM @bg_ddl;
EXECUTE bg_stmt;
DEALLOCATE PREPARE bg_stmt;




-- categories config (backend-driven)
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cat_key VARCHAR(32) NOT NULL UNIQUE,
    label VARCHAR(64) NOT NULL,
    sort INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subcategories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cat_key VARCHAR(32) NOT NULL,
    sub_key VARCHAR(32) NOT NULL DEFAULT '',
    label VARCHAR(64) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_catkey_subkey (cat_key, sub_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO categories (cat_key, label, sort) VALUES
 ('cooking','买菜做饭',1),
 ('delivery','懒人外卖',2)
ON DUPLICATE KEY UPDATE label=VALUES(label), sort=VALUES(sort);

INSERT INTO subcategories (cat_key, sub_key, label, sort) VALUES
 ('cooking','','全部',0),
 ('cooking','breakfast','早餐',1),
 ('cooking','meat','肉类',2),
 ('cooking','vegetable','蔬菜类',3),
 ('cooking','seafood','海鲜类',4),
 ('cooking','soup','汤品',5),
 ('cooking','snack','小吃',6),
 ('cooking','staple','主食',7),
 ('cooking','drink','饮品',8)
ON DUPLICATE KEY UPDATE label=VALUES(label), sort=VALUES(sort);
