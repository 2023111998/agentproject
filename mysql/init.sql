-- ============================================================================
-- 校园电商/外卖智能服务平台 — MySQL 数据库初始化
-- 7 表 + 种子数据 (2商家/2骑手/7商品/6订单/3政策)
-- ============================================================================

-- 确保客户端和服务端都使用 UTF-8
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS campus DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id      VARCHAR(32) PRIMARY KEY,
    name    VARCHAR(64) NOT NULL,
    role    VARCHAR(16) NOT NULL CHECK(role IN ('customer','merchant','rider')),
    phone   VARCHAR(20) DEFAULT '',
    address VARCHAR(200) DEFAULT ''
) ENGINE=InnoDB;

-- 商家/店铺表
CREATE TABLE IF NOT EXISTS stores (
    id      VARCHAR(32) PRIMARY KEY,
    name    VARCHAR(64) NOT NULL,
    phone   VARCHAR(20) DEFAULT '',
    address VARCHAR(200) DEFAULT '',
    rating  DOUBLE DEFAULT 5.0
) ENGINE=InnoDB;

-- 商品表
CREATE TABLE IF NOT EXISTS products (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name     VARCHAR(100) NOT NULL,
    price    DOUBLE NOT NULL,
    stock    INT DEFAULT 0,
    rating   DOUBLE DEFAULT 5.0,
    tag      VARCHAR(20) DEFAULT '',
    image    VARCHAR(200) DEFAULT '',
    FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB;

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id             VARCHAR(32) PRIMARY KEY,
    user_id        VARCHAR(32) NOT NULL,
    store_id       VARCHAR(32) NOT NULL,
    rider_id       VARCHAR(32) DEFAULT NULL,
    items          JSON DEFAULT NULL,
    amount         DOUBLE NOT NULL,
    type           VARCHAR(8) NOT NULL CHECK(type IN ('外卖','电商')),
    status         VARCHAR(16) DEFAULT '已下单',
    address        VARCHAR(200) DEFAULT '',
    placed_min_ago INT DEFAULT 0,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB;

-- 物流表
CREATE TABLE IF NOT EXISTS logistics (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    order_id   VARCHAR(32) UNIQUE NOT NULL,
    rider_id   VARCHAR(32) DEFAULT NULL,
    status     VARCHAR(16) DEFAULT '待配送',
    eta        VARCHAR(20) DEFAULT '',
    lat        DOUBLE,
    lng        DOUBLE,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB;

-- 售后表
CREATE TABLE IF NOT EXISTS after_sales (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    order_id    VARCHAR(32) NOT NULL,
    user_id     VARCHAR(32) NOT NULL,
    reason      TEXT,
    reason_type VARCHAR(20) DEFAULT '',
    result      TEXT,
    status      VARCHAR(16) DEFAULT '待处理',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB;

-- 政策知识库表
CREATE TABLE IF NOT EXISTS policies (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    title   VARCHAR(200) NOT NULL,
    content TEXT NOT NULL
) ENGINE=InnoDB;

-- ================================================================
-- 种子数据
-- ================================================================

-- 用户 (2 用户 + 2 商家 + 2 骑手)
INSERT INTO users VALUES
('u001', '张三', 'customer', '', '三号宿舍楼'),
('u002', '李四', 'customer', '', '五号宿舍楼'),
('m001', '学生食堂', 'merchant', '010-11110001', '一食堂二楼'),
('m002', '校园商业街数码店', 'merchant', '010-11110002', '商业街18号'),
('r001', '张师傅', 'rider', '13812345678', ''),
('r002', '王师傅', 'rider', '13900001234', '');

-- 店铺
INSERT INTO stores VALUES
('m001', '学生食堂', '010-11110001', '一食堂二楼', 5.0),
('m002', '校园商业街数码店', '010-11110002', '商业街18号', 5.0);

-- 商品 (m001: 外卖 / m002: 数码)
INSERT INTO products (store_id, name, price, stock, rating, tag) VALUES
('m001', '黄焖鸡米饭', 22.00, 98, 4.8, '外卖'),
('m001', '麻辣烫',     18.00, 95, 4.5, '外卖'),
('m001', '珍珠奶茶',   12.00, 97, 4.7, '外卖'),
('m001', '炸鸡排',     15.00, 90, 4.3, '外卖'),
('m001', '可乐',        5.00, 100,4.6, '外卖'),
('m002', '蓝牙耳机',   199.00, 24, 4.6, '数码'),
('m002', '机械键盘',   329.00, 10, 4.7, '数码');

-- 订单 (6 条种子订单)
INSERT INTO orders (id, user_id, store_id, rider_id, items, amount, type, status, address, placed_min_ago, created_at) VALUES
('20260601001', 'u001', 'm001', 'r001', '["黄焖鸡米饭","可乐"]', 32.50, '外卖', '配送中', '三号宿舍楼', 25, '2026-06-01 10:00:00'),
('20260601002', 'u001', 'm001', NULL,    '["麻辣烫"]',            18.00, '外卖', '已下单', '三号宿舍楼', 5,  '2026-07-01 12:00:00'),
('20260601003', 'u001', 'm002', NULL,    '["蓝牙耳机"]',          199.00,'电商', '已发货', '三号宿舍楼', 120,'2026-06-15 15:30:00'),
('20260601004', 'u001', 'm001', 'r002', '["炸鸡排","珍珠奶茶"]', 28.00, '外卖', '已送达', '三号宿舍楼', 45, '2026-06-20 11:20:00'),
('20260601005', 'u002', 'm002', NULL,    '["机械键盘"]',          329.00,'电商', '已下单', '五号宿舍楼', 10, '2026-07-02 09:00:00'),
('20260601006', 'u002', 'm001', NULL,    '["黄焖鸡米饭"]',        22.00, '外卖', '已下单', '五号宿舍楼', 8,  '2026-07-03 08:30:00');

-- 物流 (为每个订单创建物流记录)
INSERT INTO logistics (order_id, rider_id, status, eta, lat, lng) VALUES
('20260601001', 'r001', '配送中', '12:30', 34.1500, 108.8500),
('20260601002', NULL,    '待配送', '',     NULL,    NULL),
('20260601003', NULL,    '待配送', '',     NULL,    NULL),
('20260601004', 'r002', '已送达', '11:50', 34.1510, 108.8520),
('20260601005', NULL,    '待配送', '',     NULL,    NULL),
('20260601006', NULL,    '待配送', '',     NULL,    NULL);

-- 政策知识库
INSERT INTO policies (title, content) VALUES
('配送时效', '外卖订单承诺30分钟内送达，若超过30分钟可在订单页申请超时红包补偿，补偿金额为订单金额的10%-30%。'),
('食品安全', '外卖食品如出现异味、变质、异物等安全问题，请保留食品原样并拍照，核实后将全额退款并额外赔付订单金额的50%-100%。'),
('签收与退款', '签收时发现商品外包装破损或商品损坏，请拒收并拍照，联系客服安排换货或退款。已签收后发现损坏，需在24小时内提交凭证。本平台支持7天无理由退货；生鲜及外卖食品因特殊性不支持无理由退货，但出现质量问题可申请赔付。退款申请审核通过后，款项将在1-3个工作日内原路退回至支付账户。');
