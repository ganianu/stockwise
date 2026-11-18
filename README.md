# StockWise - Inventory Management System

## 1. Project Overview

StockWise is a RESTful inventory management backend built using Java and Spring Boot. It provides APIs to manage products, categories, suppliers and stock transactions.

The application supports inventory tracking, low-stock identification, transaction history, soft deletion, data validation, audit tracking and Redis-based product caching.

## 2. Technology Stack

- **Language:** Java 17

- **Framework:** Spring Boot 4

- **Build Tool:** Maven

- **Database:** MySQL 8

- **ORM:** Spring Data JPA / Hibernate

- **Database Migrations:** Flyway

- **Caching:** Redis

- **Security:** Spring Security with HTTP Basic Authentication

- **Password Encoding:** BCrypt

- **Validation:** Jakarta Bean Validation

- **Testing:** JUnit and Spring Boot Test

## 3. Key Features

### Product Management

- Create, retrieve, update and soft-delete products.

- Assign products to categories.

- Paginated product listing with configurable sorting.

- Identify products that have reached or fallen below their reorder level.

- Prevent direct modification of product quantity through product update requests.

### Category Management

- Create, retrieve, update and soft-delete categories.

- Retrieve products belonging to a category.

### Supplier Management

- Create, retrieve, update and soft-delete suppliers.

- Validate supplier details.

- Return active suppliers only.

### Stock Management

- Record stock-in transactions.

- Record stock-out transactions.

- Record positive or negative stock adjustments.

- Prevent stock-out operations that exceed available inventory.

- Prevent adjustments that would result in negative inventory.

- Retrieve paginated transaction history for individual products.

### Additional Features

- Redis caching for product retrieval.

- Cache eviction after successful stock transactions.

- Soft deletion using a deleted flag.

- Automatic creation and modification audit fields.

- Centralised exception handling and validation responses.

- Database schema management through Flyway migrations.

- HTTP Basic Authentication for API endpoints.

## 4. Project Structure

```text

stockwise/

├── src/

│   ├── main/

│   │   ├── java/com/stockwise/

│   │   │   ├── advice/

│   │   │   ├── cache/

│   │   │   ├── configs/

│   │   │   ├── controller/

│   │   │   ├── dto/

│   │   │   ├── exceptions/

│   │   │   ├── models/

│   │   │   ├── repository/

│   │   │   └── service/

│   │   └── resources/

│   │       ├── db/migration/

│   │       └── application.properties

│   └── test/

├── .mvn/

├── mvnw

├── mvnw.cmd

└── pom.xml

```

## 5. Prerequisites

Install and configure the following:

- Java JDK 17

- MySQL Server 8 or compatible version

- Redis server

- Git (optional)

- Postman or another REST API client

The application uses the following local service defaults:

| Service | Default address |

|---|---|

| Spring Boot | `http://localhost:8080` |

| MySQL | `localhost:3306` |

| Redis | `localhost:6379` |

## 6. Database Setup

Create the StockWise database in MySQL:

```sql

CREATE DATABASE stockwise_db;

```

Create or configure a MySQL user with access to this database.

The application uses the following database settings:

- Database: `stockwise_db`

- Username: `stockwise_user`

- Password: supplied through an environment variable

Flyway automatically manages database migrations when the application starts.

Migration scripts are located in:

```text

src/main/resources/db/migration/

```

The initial schema is created by:

```text

V1__create_stockwise_tables.sql

```

Hibernate is configured to validate the database schema rather than automatically modify it.

## 7. Redis Setup

Start a Redis server locally and make sure it is accessible at:

```text

localhost:6379

```

StockWise uses Redis for product caching.

Product cache entries use the `PRODUCT_V3:` key prefix and are stored using a DTO-based JSON representation.

The configured cache expiry is 30 minutes.

Successful stock transactions evict the corresponding product cache entry after the database transaction commits.

## 8. Environment Configuration

The application uses environment variables for database and API credentials.

Configure the following variables before starting the application.

### Windows PowerShell

```powershell

$env:STOCKWISE_DB_PASSWORD = "your_mysql_password"

$env:STOCKWISE_API_USERNAME = "your_api_username"

$env:STOCKWISE_API_PASSWORD = "your_api_password"

```

Do not commit real passwords or credentials to source control.

## 9. Running the Application

Clone the repository:

```bash

git clone <repository-url>

cd stockwise

```

Set the required environment variables, then run the application using the Maven wrapper.

### Windows

```powershell

.\mvnw.cmd spring-boot:run

```

### Linux / macOS

```bash

./mvnw spring-boot:run

```

The application starts on:

```text

http://localhost:8080

```

## 10. Authentication

All endpoints under `/api/**` require HTTP Basic Authentication.

Configure the API username and password through:

- `STOCKWISE_API_USERNAME`

- `STOCKWISE_API_PASSWORD`

In Postman:

1. Open the Authorization tab.

2. Select Basic Auth.

3. Enter the configured username and password.

4. Send the API request.

Passwords are encoded using BCrypt.

## 11. REST API Reference

Base URL:

```text

http://localhost:8080

```

All API endpoints require authentication.

### 11.1 Category APIs

| Method | Endpoint | Description |

|---|---|---|

| POST | `/api/categories` | Create a category |

| GET | `/api/categories` | Retrieve all active categories |

| GET | `/api/categories/{id}` | Retrieve a category |

| PUT | `/api/categories/{id}` | Update a category |

| DELETE | `/api/categories/{id}` | Soft-delete a category |

| GET | `/api/categories/{categoryId}/products` | Retrieve products in a category |

Example request:

```json

{

  "name": "Electronics"

}

```

### 11.2 Product APIs

| Method | Endpoint | Description |

|---|---|---|

| POST | `/api/products` | Create a product |

| GET | `/api/products` | Retrieve paginated products |

| GET | `/api/products/{id}` | Retrieve a product |

| PUT | `/api/products/{id}` | Update a product |

| DELETE | `/api/products/{id}` | Soft-delete a product |

| GET | `/api/products/low-stock` | Retrieve low-stock products |

Example create request:

```json

{

  "name": "Wireless Keyboard",

  "sku": "KB-1001",

  "description": "Wireless office keyboard",

  "unitPrice": 49.99,

  "quantity": 50,

  "reorderLevel": 10,

  "categoryId": 1

}

```

Product listing supports pagination and sorting.

Example:

```http

GET /api/products?pageNumber=0&pageSize=5&sortBy=name&sortDirection=asc

```

Default values:

| Parameter | Default |

|---|---|

| pageNumber | 0 |

| pageSize | 10 |

| sortBy | name |

| sortDirection | asc |

**Important:** Product quantity cannot be updated directly through the product update endpoint. Use the stock transaction APIs instead.

### 11.3 Supplier APIs

| Method | Endpoint | Description |

|---|---|---|

| POST | `/api/suppliers` | Create a supplier |

| GET | `/api/suppliers` | Retrieve active suppliers |

| GET | `/api/suppliers/{id}` | Retrieve a supplier |

| PUT | `/api/suppliers/{id}` | Update a supplier |

| DELETE | `/api/suppliers/{id}` | Soft-delete a supplier |

Example request:

```json

{

  "name": "ABC Electronics",

  "contactPerson": "John Smith",

  "email": "[john@example.com](mailto:john@example.com)",

  "phone": "123456789",

  "address": "Dublin, Ireland"

}

```

### 11.4 Stock Transaction APIs

| Method | Endpoint | Description |

|---|---|---|

| POST | `/api/stock-transactions/stock-in` | Record stock received |

| POST | `/api/stock-transactions/stock-out` | Record stock issued |

| POST | `/api/stock-transactions/adjustments` | Adjust stock quantity |

| GET | `/api/stock-transactions/products/{productId}` | Retrieve product transaction history |

**Stock-in example:**

```json

{

  "productId": 1,

  "quantity": 20,

  "supplierId": 1,

  "notes": "New stock received"

}

```

**Stock-out example:**

```json

{

  "productId": 1,

  "quantity": 5,

  "notes": "Stock issued"

}

```

**Stock adjustment example:**

```json

{

  "productId": 1,

  "quantityChange": -2,

  "notes": "Inventory correction"

}

```

A positive adjustment increases inventory. A negative adjustment decreases inventory.

Transaction history supports pagination:

```http

GET /api/stock-transactions/products/1?pageNumber=0&pageSize=10

```

## 12. Validation and Error Handling

StockWise uses Jakarta Bean Validation to validate incoming requests.

Examples of validation rules include:

- Required product names and SKUs.

- Positive unit prices.

- Non-negative initial product quantities.

- Positive stock-in and stock-out quantities.

- Valid supplier email addresses when provided.

- Maximum field lengths.

- Prevention of direct product quantity updates.

The application provides centralised exception handling.

Error responses follow this structure:

```json

{

  "message": "Error description",

  "status": 400,

  "timestamp": "2026-11-18T10:30:00"

}

```

The timestamp is illustrative.

## 13. Database and Inventory Integrity

- Products, categories and suppliers use soft deletion.

- Stock transactions are recorded as separate inventory events.

- Stock-in, stock-out and adjustment operations are transactional.

- Product records are locked during stock-changing operations to prevent concurrent inventory updates from causing incorrect quantities.

- Stock-out operations cannot reduce inventory below zero.

- Adjustments cannot result in negative inventory.

- Product cache entries are evicted after successful stock transactions.

## 14. Audit Tracking

The application uses Spring Data JPA auditing.

Audit information includes:

- `createdAt`

- `updatedAt`

- `createdBy`

- `lastModifiedBy`

Authenticated API users are recorded as the auditor. System operations use `system` as the fallback auditor.

## 15. Running Tests

Run the complete automated test suite using the Maven wrapper.

### Windows

```powershell

.\mvnw.cmd test

```

### Linux / macOS

```bash

./mvnw test

```

The test suite covers controller and service-layer functionality, including product, category, supplier and stock transaction operations.

## 16. Future Enhancements

Potential future improvements include:

- Role-based access control.

- Multiple warehouse and location management.

- Supplier performance reporting.

- Inventory analytics and dashboards.

- Automated low-stock notifications.

- API documentation using OpenAPI / Swagger.

- Deployment using Docker and cloud infrastructure.

## 17. Author

StockWise was developed as a backend development capstone project using Java, Spring Boot, MySQL and Redis.