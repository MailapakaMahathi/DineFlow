CREATE DATABASE IF NOT EXISTS dineflow_db;
USE dineflow_db;

CREATE TABLE IF NOT EXISTS menu_items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    vendor_name VARCHAR(100),
    vendor_phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS restaurant_orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    subtotal DECIMAL(10,2) NOT NULL,
    tax_amount DECIMAL(10,2) DEFAULT 0.00,
    discount_amount DECIMAL(10,2) DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL,
    order_status VARCHAR(30) DEFAULT 'COMPLETED'
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    item_id INT NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,

    FOREIGN KEY (order_id)
        REFERENCES restaurant_orders(order_id),

    FOREIGN KEY (item_id)
        REFERENCES menu_items(item_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL UNIQUE,
    payment_method VARCHAR(30) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(30) DEFAULT 'SUCCESS',
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (order_id)
        REFERENCES restaurant_orders(order_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vendor_sms_notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    vendor_phone VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    notification_status VARCHAR(30) DEFAULT 'PENDING',
    attempt_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,

    FOREIGN KEY (item_id)
        REFERENCES menu_items(item_id)
        
) ENGINE=InnoDB;


INSERT INTO menu_items
(item_name, category, price, stock_quantity, vendor_name, vendor_phone)
VALUES
('Chicken Biryani', 'Main Course', 180.00, 20, 'Fresh Foods Supplier', '+919876543210'),
('Veg Biryani', 'Main Course', 130.00, 15, 'Fresh Foods Supplier', '+919876543211'),
('Paneer Butter Masala', 'Main Course', 160.00, 10, 'Dairy Suppliers', '+919876543212'),
('Masala Dosa', 'Breakfast', 70.00, 25, 'Local Vegetable Supplier', '+919876543213'),
('Idly', 'Breakfast', 40.00, 30, 'Local Vegetable Supplier', '+919876543214'),
('Gulab Jamun', 'Dessert', 60.00, 30, 'Swagruha Sweets', '+919000543214'),
('poori', 'Breakfast', 45.00, 30, 'Local Vegetable Supplier', '+919876003214');

USE dineflow_db;
SHOW TABLES;

SELECT
    o.order_id,
    o.customer_name,
    o.total_amount,
    p.payment_method,
    p.amount,
    p.payment_status,
    p.payment_date
FROM restaurant_orders o
JOIN payments p
    ON o.order_id = p.order_id
WHERE o.order_id = 1;

SELECT *
FROM vendor_sms_notifications
ORDER BY created_at DESC;

SELECT
    item_id,
    item_name,
    stock_quantity,
    vendor_name,
    vendor_phone
FROM menu_items;

UPDATE menu_items
SET stock_quantity = 6
WHERE item_id = 3;

SELECT *
FROM vendor_sms_notifications
ORDER BY created_at DESC;

UPDATE menu_items
SET vendor_phone = '+916304378120'
WHERE item_id = 3;

ALTER TABLE vendor_sms_notifications
ADD COLUMN twilio_message_sid VARCHAR(64) NULL;

DESCRIBE vendor_sms_notifications;

SELECT
    notification_id,
    notification_status,
    twilio_message_sid,
    attempt_count,
    last_attempt_at,
    sent_at
FROM vendor_sms_notifications
ORDER BY notification_id DESC;

SELECT
    item_name,
    COUNT(*) AS total_records,
    GROUP_CONCAT(item_id ORDER BY item_id) AS item_ids
FROM menu_items
GROUP BY item_name
HAVING COUNT(*) > 1;

SELECT
    m.item_id,
    m.item_name,
    m.stock_quantity,
    COUNT(oi.order_item_id) AS times_ordered
FROM menu_items m
LEFT JOIN order_items oi
    ON m.item_id = oi.item_id
WHERE m.item_id IN (9, 14, 13, 12, 11, 15, 10)
GROUP BY m.item_id, m.item_name, m.stock_quantity
ORDER BY m.item_id;

SELECT item_id, item_name, stock_quantity, is_active
FROM menu_items
ORDER BY item_id;

SELECT
    item_id,
    item_name,
    is_active
FROM menu_items
ORDER BY item_id;

SELECT
    m.item_id,
    m.item_name,
    m.is_active,
    COUNT(oi.order_item_id) AS order_references
FROM menu_items m
LEFT JOIN order_items oi ON m.item_id = oi.item_id
WHERE m.item_id BETWEEN 9 AND 15
GROUP BY m.item_id, m.item_name, m.is_active
ORDER BY m.item_id;

DELETE FROM menu_items
WHERE item_id BETWEEN 9 AND 15
AND is_active = FALSE;