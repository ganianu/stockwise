
-- ============================================
-- StockWise V1: Initial Database Schema
-- ============================================

-- 1. Categories
CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NULL,
    deleted BIT(1) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    name VARCHAR(100) NOT NULL,
    created_by VARCHAR(255) DEFAULT NULL,
    last_modified_by VARCHAR(255) DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY UK_categories_name (name)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;


-- 2. Suppliers
CREATE TABLE suppliers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NULL,
    deleted BIT(1) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    contact_person VARCHAR(100) NOT NULL,
    email VARCHAR(150) DEFAULT NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    created_by VARCHAR(255) DEFAULT NULL,
    last_modified_by VARCHAR(255) DEFAULT NULL,

    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;


-- 3. Products
CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NULL,
    deleted BIT(1) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    name VARCHAR(150) NOT NULL,
    quantity INT NOT NULL,
    reorder_level INT NOT NULL,
    sku VARCHAR(50) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    category_id BIGINT DEFAULT NULL,
    created_by VARCHAR(255) DEFAULT NULL,
    last_modified_by VARCHAR(255) DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY UK_products_sku (sku),
    KEY IDX_products_category_id (category_id),

    CONSTRAINT FK_products_category
        FOREIGN KEY (category_id)
        REFERENCES categories (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;


-- 4. Stock Transactions
CREATE TABLE stock_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) DEFAULT NULL,
    deleted BIT(1) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    notes VARCHAR(500) DEFAULT NULL,
    quantity_change INT NOT NULL,
    transaction_type ENUM(
        'ADJUSTMENT',
        'STOCK_IN',
        'STOCK_OUT'
    ) NOT NULL,
    product_id BIGINT NOT NULL,
    supplier_id BIGINT DEFAULT NULL,
    created_by VARCHAR(255) DEFAULT NULL,
    last_modified_by VARCHAR(255) DEFAULT NULL,

    PRIMARY KEY (id),
    KEY IDX_stock_transactions_product_id (product_id),
    KEY IDX_stock_transactions_supplier_id (supplier_id),

    CONSTRAINT FK_stock_transactions_product
        FOREIGN KEY (product_id)
        REFERENCES products (id),

    CONSTRAINT FK_stock_transactions_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES suppliers (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
