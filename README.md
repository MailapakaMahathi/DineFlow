# 🍽️ DineFlow – Smart Restaurant Management System

**DineFlow** is a Java-based desktop application designed to simplify and automate restaurant operations. It integrates food ordering, menu management, billing, inventory tracking, vendor SMS notifications and sales reporting into a single platform.

The application provides an intuitive interface for restaurant administrators to manage daily operations, process customer orders, monitor stock levels and analyse sales performance efficiently.

---

## 📌 Project Overview

Traditional restaurant management involves handling multiple activities such as food orders, billing, inventory tracking and vendor communication separately.

DineFlow addresses these challenges by providing a centralized restaurant management solution with automated order processing, database-driven inventory updates, invoice generation and sales analytics.

## ✨ Key Features

### 1. 📊 Dashboard
- Displays total revenue and order statistics.
- Provides an overview of available menu items.
- Monitors low-stock and out-of-stock items.
- Visualizes restaurant sales performance.
- Displays inventory status.

### 2. 🍛 Food Ordering
- Displays food items with images, categories and prices.
- Allows users to add items to the cart.
- Supports increasing, decreasing and removing quantities.
- Automatically calculates subtotal, GST and discounts.
- Collects customer details and payment method.
- Processes orders and updates inventory.

### 3. 📝 Menu Management
- Add new food items.
- Edit existing menu details.
- Update food prices and stock quantities.
- Manage food categories.
- Maintain vendor information.
- Activate or deactivate menu items.

### 4. 📦 Inventory Management
- Tracks food item stock quantities.
- Identifies in-stock, low-stock and out-of-stock items.
- Supports stock updates.
- Automatically deducts stock after successful order processing.
- Integrates with vendor notification functionality.

### 5. 🧾 Billing & Invoice Generation
- Maintains customer order records.
- Displays billing and payment information.
- Supports searching and filtering billing records.
- Generates PDF invoices using OpenPDF.
- Includes item details, quantities, taxes, discounts and total amounts.

### 6. 📲 Vendor SMS Notifications
- Monitors low-stock conditions.
- Generates vendor notification records.
- Integrates Twilio API for SMS notifications.
- Stores notification statuses and Twilio message IDs.
- Provides notification history for administrators.

### 7. 📈 Sales Reports
- Displays total revenue and orders.
- Calculates average order value.
- Tracks total items sold.
- Provides revenue trend analysis.
- Identifies top-selling food items.
- Displays payment method analysis.
- Supports predefined and custom date ranges.

---

## 🛠️ Technology Stack

| Category | Technologies |
|---|---|
| Programming Language | Java 17 |
| Desktop Framework | JavaFX |
| Frontend | HTML, CSS, JavaScript |
| Database | MySQL 8 |
| Database Connectivity | JDBC |
| PDF Generation | OpenPDF |
| SMS Integration | Twilio API |
| Build Tool | Maven |
| Data Handling | Gson |
| IDE | Eclipse |

---

## 🏗️ System Architecture

DineFlow follows a frontend-backend architecture within a desktop application.

```text
                DINEFLOW APPLICATION
                         |
              JavaFX Desktop Application
                         |
                  JavaFX WebView
                         |
              HTML + CSS + JavaScript
                         |
                JavaScript Bridge
                         |
                  Java Backend
                         |
          +--------------+--------------+
          |              |              |
      Services       JDBC Layer     Integrations
          |              |              |
          |           MySQL       +------+------+
          |                       |             |
          |                    Twilio        OpenPDF
          |
    +-----+-----------------------------+
    |             |                     |
Dashboard     Order Service       Menu Management
Billing       Inventory Service   Sales Reports
Vendor SMS
```

The frontend communicates with Java backend methods through a JavaScript bridge. Java uses JDBC to interact with MySQL, while external libraries support PDF generation and SMS notifications.

---

## 🗄️ Database Design

The application uses a MySQL database named:

`dineflow_db`

### Database Tables

| Table | Description |
|---|---|
| `menu_items` | Stores food item details, prices, stock and vendor information. |
| `restaurant_orders` | Stores customer orders and billing amounts. |
| `order_items` | Stores individual food items associated with each order. |
| `payments` | Maintains payment methods, amounts and payment statuses. |
| `vendor_sms_notifications` | Stores vendor SMS notification details and statuses. |

### Database Relationships

- One restaurant order can contain multiple order items.
- Each order item references a menu item.
- Each restaurant order can have an associated payment record.
- Menu items can have multiple vendor notification records.

---

## ⚙️ Installation and Setup

### Prerequisites

Install the following before running the project:

- Java Development Kit (JDK) 17
- Eclipse IDE
- MySQL Server 8
- Maven
- Internet connection for Twilio integration

### Step 1: Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/DineFlow.git
```

Navigate to the project directory:

```bash
cd DineFlow
```

### Step 2: Create the Database

Open MySQL Workbench and execute the SQL script provided in:

`database.sql`

This creates the database and required tables.

### Step 3: Configure Database Connectivity

Update your database connection settings in `DB.java`.

Configure the MySQL URL, username and password for your local environment.

**Security:** Avoid committing database credentials to GitHub. Use environment variables or a local configuration file excluded through `.gitignore`.

### Step 4: Configure Twilio (Optional)

Set the following environment variables if you want to use vendor SMS notifications:

```text
TWILIO_ACCOUNT_SID
TWILIO_AUTH_TOKEN
TWILIO_PHONE_NUMBER
```

Configure these values using your Twilio account credentials.

Note: Twilio trial accounts have recipient verification and messaging-template restrictions.

### Step 5: Run the Application

Import the project into Eclipse as a Maven project.

Wait for Maven dependencies to download.

Run the application using the Maven goal:

```bash
clean javafx:run
```

Alternatively, run `DineFlowApp.java` from Eclipse with the required JavaFX configuration.

---

## 📂 Project Structure

```text
DineFlow/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── dineflow/
│   │   │           ├── DB.java
│   │   │           ├── DineFlowApp.java
│   │   │           ├── DashboardBridge.java
│   │   │           ├── DashboardService.java
│   │   │           ├── MenuItem.java
│   │   │           ├── CartLine.java
│   │   │           ├── MenuManagementService.java
│   │   │           ├── OrderService.java
│   │   │           ├── BillingService.java
│   │   │           ├── InvoicePdfGenerator.java
│   │   │           ├── SmsNotificationService.java
│   │   │           └── SalesReportService.java
│   │   │
│   │   └── resources/
│   │       └── frontend/
│   │           ├── css/
│   │           │   └── style.css
│   │           ├── js/
│   │           │   └── app.js
│   │           ├── images/
│   │           └── pages/
│   │               └── index.html
│   │
│   └── test/
│
├── database.sql
├── pom.xml
└── README.md
```

---

## 🔄 Order Processing Workflow

1. The administrator opens the Food Ordering module.
2. Available menu items are retrieved from MySQL.
3. Food items are added to the shopping cart.
4. The application calculates subtotal, GST, discount and total amount.
5. Customer details and payment method are entered.
6. The Java backend validates item availability and stock quantities.
7. A database transaction inserts order details and payment records.
8. Inventory quantities are updated.
9. The transaction is committed if all operations succeed; otherwise, it is rolled back.
10. The order becomes available in the Billing and Sales Reports modules.

---

## 🔐 Important Implementation Details

- JDBC is used for database connectivity.
- Prepared statements are used for parameterized SQL operations.
- Database transactions help maintain consistency during order processing.
- Foreign keys maintain relationships between database tables.
- Stock quantities are checked before processing orders.
- Historical order item names and prices are stored in the order records.
- Twilio notification statuses are maintained in the database.

**Payment note:** The current application records the selected payment method and payment status as part of its order workflow. It does not currently integrate a real payment gateway or verify actual online payments.

---

## 🚀 Future Enhancements

- Integration with real payment gateways.
- Role-based authentication and authorization.
- Customer management and order history.
- Ingredient-level inventory management.
- Automated email notifications.
- Advanced sales forecasting and analytics.
- Cloud database integration.
- Automated testing and application monitoring.

---

## 👨‍💻 Project Information

**Project Name:** DineFlow – Smart Restaurant Operations, Billing, Inventory and Vendor SMS Management System

**Project Type:** Java Desktop Application

**Domain:** Restaurant Management / Software Development

**Developed Using:** Java, JavaFX, HTML, CSS, JavaScript and MySQL

---

<img width="1339" height="722" alt="image" src="https://github.com/user-attachments/assets/2ba6be5c-2015-4a58-9558-82e6b7dfc653" />
<img width="1324" height="717" alt="image" src="https://github.com/user-attachments/assets/83fd51b6-83b2-4b21-9f97-2895b29b0078" />
<img width="1334" height="717" alt="image" src="https://github.com/user-attachments/assets/874eb739-adda-41a8-b79e-c84fab4e14b7" />
<img width="1326" height="712" alt="image" src="https://github.com/user-attachments/assets/227a5ff2-f444-4ddd-88f3-b983dd514baa" />
<img width="1335" height="698" alt="image" src="https://github.com/user-attachments/assets/62c13f34-a571-4230-b1d0-4033036d573f" />
<img width="1335" height="706" alt="image" src="https://github.com/user-attachments/assets/f7b159ce-a907-4d9f-8f8e-a37317ece5c0" />
<img width="1318" height="707" alt="image" src="https://github.com/user-attachments/assets/9ccfad96-655c-4015-9360-8014bc91bd05" />

**DineFlow – Simplifying Restaurant Operations, One Order at a Time!** 🍽️
