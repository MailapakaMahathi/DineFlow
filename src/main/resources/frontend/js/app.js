"use strict";

// ==========================================
// 1. GLOBAL VARIABLES
// ==========================================

let availableMenuItems = [];
let managementMenuItems = [];
let inventoryItems = [];
let shoppingCart = [];

const GST_RATE = 0.05;


// ==========================================
// 2. COMMON UTILITIES
// ==========================================

function escapeHTML(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

function formatCurrency(amount) {
    return Number(amount || 0).toLocaleString("en-IN", {
        style: "currency",
        currency: "INR"
    });
}

function getBridge() {
    if (typeof dineFlow === "undefined") {
        throw new Error("Java backend connection is unavailable.");
    }
    return dineFlow;
}


// ==========================================
// 3. CURRENT DATE
// ==========================================

function displayCurrentDate() {
    const dateElement = document.getElementById("currentDate");

    if (!dateElement) return;

    dateElement.textContent = new Date().toLocaleDateString("en-IN", {
        weekday: "short",
        day: "numeric",
        month: "short",
        year: "numeric"
    });
}


// ==========================================
// 4. PAGE NAVIGATION
// ==========================================

function initializeNavigation() {
    document.addEventListener("click", function(event) {
        const button = event.target.closest("[data-page]");

        if (!button) return;

        event.preventDefault();
        handleNavigation(button.dataset.page);
    });
}

function handleNavigation(page) {
    const pageTitles = {
        dashboard: {
            title: "Dashboard 🏠",
            description: "Welcome back! Here's what's happening today."
        },
        orders: {
            title: "Food Ordering🍽",
            description: "Explore our menu and manage customer orders."
        },
        menu: {
            title: "Menu Management🍜",
            description: "Manage restaurant food items and prices."
        },
        inventory: {
            title: "Inventory📦",
            description: "Monitor stock levels and update quantities."
        },
        billing: {
            title: "Billing 💳",
            description: "Manage customer bills and payments."
        },
        vendors: {
            title: "Vendor Notifications🚚",
            description: "Monitor vendor SMS notifications."
        },
        reports: {
            title: "Sales Reports📊",
            description: "View restaurant sales performance."
        }
    };

    const pageInfo = pageTitles[page];

    if (!pageInfo) return;

    document.querySelector(".topbar h1").textContent = pageInfo.title;
    document.querySelector(".topbar p").textContent = pageInfo.description;

    document.querySelectorAll(".page-content").forEach(section => {
        section.style.display = "none";
    });

    if (page === "dashboard") {
        document.getElementById("dashboardPage").style.display = "block";
        loadDashboardStats();
        loadRecentOrders();

    } else if (page === "orders") {
        document.getElementById("ordersPage").style.display = "block";
        loadMenuItems();

    } else if (page === "menu") {
        document.getElementById("menuPage").style.display = "block";
        loadManagementMenuItems();

    } else if (page === "inventory") {
        document.getElementById("inventoryPage").style.display = "block";
        loadInventoryItems();
		
		} else if (page === "billing") {
		        document.getElementById("billingPage").style.display = "block";
		        loadBillingStats();
		        loadBillingOrders();

		    } else if (page === "vendors") {
		        document.getElementById("vendorsPage").style.display = "block";
		        loadVendorNotifications();

		    } else if (page === "reports") {
		        document.getElementById("reportsPage").style.display = "block";

		        if (typeof loadSalesReports === "function") {
		            loadSalesReports();
		        }

		    } else {
		        console.log(pageInfo.title + " module is not implemented yet.");
		    }
    document.querySelectorAll(".nav-item").forEach(button => {
        button.classList.toggle(
            "active",
            button.dataset.page === page
        );
    });

    console.log("Navigated to:", page);
}


// ==========================================
// 5. DASHBOARD STATISTICS
// ==========================================

function loadDashboardStats() {
    try {
        const stats = JSON.parse(getBridge().getDashboardStatsJson());
        updateDashboardUI(stats);

        // Load Dashboard Sales Overview chart
        loadDashboardSalesOverview();

    } catch (error) {
        console.error("Unable to load dashboard statistics:", error);
    }
}

function updateDashboardUI(stats) {
    document.getElementById("totalRevenue").textContent =
        formatCurrency(stats.revenue);

    document.getElementById("totalOrders").textContent =
        stats.orders;

    document.getElementById("totalMenuItems").textContent =
        stats.menuItems;

    document.getElementById("lowStockItems").textContent =
        stats.lowStock;

    document.getElementById("inStockCount").textContent =
        stats.inStock;

    document.getElementById("lowStockCount").textContent =
        stats.lowStock;

    document.getElementById("outStockCount").textContent =
        stats.outOfStock;
}


// ==========================================
// 6. RECENT ORDERS
// ==========================================

function loadRecentOrders() {
    try {
        const orders = JSON.parse(getBridge().getRecentOrdersJson());
        displayRecentOrders(orders);

    } catch (error) {
        console.error("Unable to load recent orders:", error);
    }
}

function displayRecentOrders(orders) {
    const tableBody = document.getElementById("recentOrdersBody");

    if (!tableBody) return;

    if (!orders || orders.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-message">
                    No orders available yet.
                </td>
            </tr>`;
        return;
    }

    tableBody.innerHTML = orders.map(order => {
        const status = String(order.status || "PENDING").toUpperCase();

        const statusClass =
            status === "COMPLETED" ? "completed" :
            status === "CANCELLED" ? "cancelled" : "pending";

        return `
            <tr>
                <td>#${escapeHTML(order.orderId)}</td>
                <td>${escapeHTML(order.customer)}</td>
                <td>${escapeHTML(order.date)}</td>
                <td>${formatCurrency(order.amount)}</td>
                <td>${escapeHTML(order.payment)}</td>
                <td>
                    <span class="order-status ${statusClass}">
                        ${escapeHTML(status)}
                    </span>
                </td>
            </tr>`;
    }).join("");
}
// ==========================================
// 7. FOOD ORDERING - LOAD MENU
// ==========================================

function loadMenuItems() {
    const container = document.getElementById("menuItemsContainer");

    try {
        const response = getBridge().getMenuItemsJson();

		availableMenuItems = JSON.parse(response).filter(
		    item => item.active === true
		);

        if (!Array.isArray(availableMenuItems)) {
            throw new Error("Invalid menu response.");
        }

        populateCategoryFilter();
        displayMenuItems(availableMenuItems);

        console.log("Food ordering items loaded:", availableMenuItems.length);

    } catch (error) {
        console.error("Error loading food ordering menu:", error);

        if (container) {
            container.innerHTML =
                "<p>Unable to load menu items. Please check the connection.</p>";
        }
    }
}


// ==========================================
// 8. FOOD ORDERING CATEGORY FILTER
// ==========================================

function populateCategoryFilter() {
    const categoryFilter = document.getElementById("categoryFilter");

    if (!categoryFilter) return;

    const previousValue = categoryFilter.value;

    const categories = [
        ...new Set(
            availableMenuItems.map(item => item.category).filter(Boolean)
        )
    ];

    categoryFilter.innerHTML =
        '<option value="All">All Categories</option>';

    categories.forEach(category => {
        const option = document.createElement("option");

        option.value = category;
        option.textContent = category;

        categoryFilter.appendChild(option);
    });

    if (categories.includes(previousValue)) {
        categoryFilter.value = previousValue;
    }
}

function filterMenuItems() {
    const selectedCategory =
        document.getElementById("categoryFilter").value;

    const filteredItems = selectedCategory === "All"
        ? availableMenuItems
        : availableMenuItems.filter(
            item => item.category === selectedCategory
        );

    displayMenuItems(filteredItems);
}


// ==========================================
// 9. DISPLAY FOOD ORDERING CARDS
// ==========================================


function displayMenuItems(items) {

    const container = document.getElementById("menuItemsContainer");

    if (!container) return;

    if (!items || items.length === 0) {
        container.innerHTML = "<p>No menu items available.</p>";
        return;
    }

    // Match each food item with its image
	const foodImages = {
	    "idly": "idly.jpg",
	    "masala dosa": "masaladosa.jpg",
	    "poori": "poori.jpg",
	    "chicken biryani": "chickenbiryani.jpg",
	    "veg biryani": "vegbiryani.jpg",
	    "paneer butter masala": "paneerbuttermasala.jpg",
	    "gulab jamun": "gulabjamun.jpg",
	    "brownie": "brownie.jpg",
		"paneer dosa": "paneerdosa.png",
		"butter naan": "butternaan.png"
	};

    container.innerHTML = items.map(item => {

        const isAvailable = Number(item.stockQuantity) > 0;

        const imageName = foodImages[
            String(item.itemName || "").trim().toLowerCase()
        ];
				console.log("Food:", item.itemName, "Image:", imageName);

        const imagePath = imageName
            ? `../images/${imageName}`
            : "";
			console.log("PANEER DOSA IMAGE PATH:", imagePath);
			console.log("Food item:", item.itemName, "| Image:", imageName);
        return `
            <div class="food-card">

                <img
                    class="food-card-image"
                    src="${imagePath}"
                    alt="${escapeHTML(item.itemName)}"
                    onerror="console.error('IMAGE LOAD FAILED:', this.src); this.alt='Image not found';"
                    style="${imagePath ? '' : 'display:none;'}"
                >

                <div class="food-card-details">

                    <span class="food-category">
                        ${escapeHTML(item.category)}
                    </span>

                    <h3>${escapeHTML(item.itemName)}</h3>

                    <p class="food-price">
                        ${formatCurrency(item.price)}
                    </p>

                    <p class="food-stock">
                        ${isAvailable
                            ? "Available: " + item.stockQuantity
                            : "Out of Stock"}
                    </p>

                    <button
                        class="add-to-cart-btn"
                        data-item-id="${item.itemId}"
                        ${!isAvailable ? "disabled" : ""}>

                        ${isAvailable ? "＋ Add to Cart" : "Unavailable"}

                    </button>

                </div>
            </div>
        `;

    }).join("");
}



// ==========================================
// 10. SHOPPING CART
// ==========================================

function addToCart(itemId) {
    const item = availableMenuItems.find(
        menuItem => Number(menuItem.itemId) === itemId
    );

    if (!item || item.stockQuantity <= 0) return;

    const existingItem = shoppingCart.find(
        cartItem => cartItem.itemId === itemId
    );

    if (existingItem) {

        if (existingItem.quantity >= item.stockQuantity) {
            alert("Maximum available stock reached!");
            return;
        }

        existingItem.quantity++;

    } else {

        shoppingCart.push({
            itemId: Number(item.itemId),
            itemName: item.itemName,
            price: Number(item.price),
            stockQuantity: Number(item.stockQuantity),
            quantity: 1
        });
    }

    updateCartUI();
}

function increaseQuantity(itemId) {
    const cartItem = shoppingCart.find(
        item => item.itemId === itemId
    );

    if (!cartItem) return;

    if (cartItem.quantity < cartItem.stockQuantity) {
        cartItem.quantity++;
    } else {
        alert("Maximum available stock reached!");
    }

    updateCartUI();
}

function decreaseQuantity(itemId) {
    const cartItem = shoppingCart.find(
        item => item.itemId === itemId
    );

    if (!cartItem) return;

    cartItem.quantity--;

    if (cartItem.quantity <= 0) {
        shoppingCart = shoppingCart.filter(
            item => item.itemId !== itemId
        );
    }

    updateCartUI();
}

function removeFromCart(itemId) {
    shoppingCart = shoppingCart.filter(
        item => item.itemId !== itemId
    );

    updateCartUI();
}


// ==========================================
// 11. UPDATE CART AND BILL
// ==========================================

function updateCartUI() {
    const cartContainer = document.getElementById("cartItems");

    if (!cartContainer) return;

    if (shoppingCart.length === 0) {

        cartContainer.innerHTML = "<p>Your cart is empty.</p>";

    } else {

        cartContainer.innerHTML = shoppingCart.map(item => {
            const lineTotal = item.price * item.quantity;

            return `
                <div class="cart-item">

                    <div class="cart-item-info">
                        <strong>${escapeHTML(item.itemName)}</strong>
                        <span>${formatCurrency(item.price)} each</span>
                    </div>

                    <div class="cart-item-controls">

                        <button data-cart-action="decrease"
                                data-item-id="${item.itemId}">−</button>

                        <span>${item.quantity}</span>

                        <button data-cart-action="increase"
                                data-item-id="${item.itemId}">+</button>

                        <button class="remove-cart-item"
                                data-cart-action="remove"
                                data-item-id="${item.itemId}">✕</button>

                    </div>

                    <strong class="cart-line-total">
                        ${formatCurrency(lineTotal)}
                    </strong>

                </div>`;
        }).join("");
    }

    const subtotal = shoppingCart.reduce(
        (sum, item) => sum + item.price * item.quantity,
        0
    );

    const tax = subtotal * GST_RATE;

    const discountInput = document.getElementById("discountAmount");

    const discount = Math.max(
        0,
        Number(discountInput?.value || 0)
    );

    const total = Math.max(0, subtotal + tax - discount);

    document.getElementById("cartSubtotal").textContent =
        formatCurrency(subtotal);

    document.getElementById("cartTax").textContent =
        formatCurrency(tax);

    document.getElementById("cartTotal").textContent =
        formatCurrency(total);

    document.getElementById("proceedToBilling").disabled =
        shoppingCart.length === 0;
}


// ==========================================
// 12. FOOD ORDERING EVENTS
// ==========================================

function initializeMenuEvents() {
    document.getElementById("menuItemsContainer")
        ?.addEventListener("click", function(event) {

            const button = event.target.closest(".add-to-cart-btn");

            if (!button || button.disabled) return;

            addToCart(Number(button.dataset.itemId));
        });

    document.getElementById("categoryFilter")
        ?.addEventListener("change", filterMenuItems);
}

function initializeCartEvents() {
    document.getElementById("cartItems")
        ?.addEventListener("click", function(event) {

            const button = event.target.closest("[data-cart-action]");

            if (!button) return;

            const itemId = Number(button.dataset.itemId);
            const action = button.dataset.cartAction;

            if (action === "increase") {
                increaseQuantity(itemId);

            } else if (action === "decrease") {
                decreaseQuantity(itemId);

            } else if (action === "remove") {
                removeFromCart(itemId);
            }
        });
}


// ==========================================
// 13. CHECKOUT
// ==========================================

function initializeCheckout() {
    document.getElementById("discountAmount")
        ?.addEventListener("input", updateCartUI);

    document.getElementById("proceedToBilling")
        ?.addEventListener("click", function() {

            const confirmButton = this;

            const customerName =
                document.getElementById("customerName").value.trim();

            const paymentMethod =
                document.getElementById("paymentMethod").value;

            const discount =
                Number(document.getElementById("discountAmount").value || 0);

            if (!customerName) {
                alert("Please enter the customer name.");
                return;
            }

            if (!paymentMethod) {
                alert("Please select a payment method.");
                return;
            }

            if (shoppingCart.length === 0) {
                alert("Please add items to your cart.");
                return;
            }

            if (!Number.isFinite(discount) || discount < 0) {
                alert("Please enter a valid discount.");
                return;
            }

            const subtotal = shoppingCart.reduce(
                (sum, item) => sum + item.price * item.quantity,
                0
            );

            const tax = subtotal * GST_RATE;

            if (discount > subtotal + tax) {
                alert("Discount cannot exceed the bill amount.");
                return;
            }

            const cartData = shoppingCart.map(item => ({
                itemId: item.itemId,
                quantity: item.quantity
            }));

            confirmButton.disabled = true;
            confirmButton.textContent = "Processing Order...";

            try {
                const response = getBridge().placeOrder(
                    customerName,
                    JSON.stringify(cartData),
                    paymentMethod,
                    discount
                );

                const result = JSON.parse(response);

                if (result.success) {

                    alert(
                        "Order placed successfully!\n" +
                        "Order ID: " + result.orderId + "\n" +
                        "Total: " + formatCurrency(result.total)
                    );

                    shoppingCart = [];

                    document.getElementById("customerName").value = "";
                    document.getElementById("paymentMethod").value = "";
                    document.getElementById("discountAmount").value = "0";

                    updateCartUI();

                    loadMenuItems();
                    loadDashboardStats();
                    loadRecentOrders();

                } else {

                    alert("Order failed: " + result.message);
                }

            } catch (error) {

                console.error("Order placement error:", error);
                alert("Unable to place order. Check the Eclipse Console.");

            } finally {

                confirmButton.disabled = shoppingCart.length === 0;
                confirmButton.textContent = "Confirm Order →";
            }
        });
}
// ==========================================
// 14. MENU MANAGEMENT - LOAD ITEMS
// ==========================================

function loadManagementMenuItems() {
    const tableBody = document.getElementById("managementMenuBody");

    if (tableBody) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="8">Loading menu items...</td>
            </tr>`;
    }

    try {
        const response =
            getBridge().getAllMenuItemsForManagementJson();

        console.log("Management response:", response);

        managementMenuItems = JSON.parse(response);

        if (!Array.isArray(managementMenuItems)) {
            throw new Error("Invalid menu management response.");
        }

        console.log("Parsed menu items:", managementMenuItems);

        populateManagementCategoryFilter();
        updateManagementStats();
        displayManagementMenuItems();

        console.log(
            "Management menu items loaded:",
            managementMenuItems.length
        );

    } catch (error) {
        console.error("Menu management loading failed:", error);

        if (tableBody) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="8">
                        Unable to load menu items.
                    </td>
                </tr>`;
        }
    }
}


// ==========================================
// 15. MENU MANAGEMENT STATISTICS
// ==========================================

function updateManagementStats() {
    const total = managementMenuItems.length;

    const active = managementMenuItems.filter(
        item => item.active
    ).length;

    const inactive = total - active;

    const lowStock = managementMenuItems.filter(
        item => item.active &&
            Number(item.stockQuantity) > 0 &&
            Number(item.stockQuantity) <= 5
    ).length;

    document.getElementById("managementTotalItems").textContent = total;

    document.getElementById("managementActiveItems").textContent = active;

    document.getElementById("managementInactiveItems").textContent = inactive;

    document.getElementById("managementLowStock").textContent = lowStock;
}


// ==========================================
// 16. MENU MANAGEMENT CATEGORY FILTER
// ==========================================

function populateManagementCategoryFilter() {
    const filter = document.getElementById("managementCategoryFilter");

    if (!filter) return;

    const previousValue = filter.value;

    const categories = [
        ...new Set(
            managementMenuItems.map(item => item.category).filter(Boolean)
        )
    ];

    filter.innerHTML = '<option value="All">All Categories</option>';

    categories.forEach(category => {
        const option = document.createElement("option");

        option.value = category;
        option.textContent = category;

        filter.appendChild(option);
    });

    if (categories.includes(previousValue)) {
        filter.value = previousValue;
    }
}


// ==========================================
// 17. DISPLAY MENU MANAGEMENT TABLE
// ==========================================

function displayManagementMenuItems() {
    const tableBody = document.getElementById("managementMenuBody");

    if (!tableBody) return;

    const searchValue =
        document.getElementById("menuSearchInput").value
            .trim().toLowerCase();

    const categoryValue =
        document.getElementById("managementCategoryFilter").value;

    const statusValue =
        document.getElementById("managementStatusFilter").value;

    const filteredItems = managementMenuItems.filter(item => {

        const matchesSearch =
            String(item.itemName || "").toLowerCase().includes(searchValue);

        const matchesCategory =
            categoryValue === "All" || item.category === categoryValue;

        const matchesStatus =
            statusValue === "All" ||
            (statusValue === "Active" && item.active) ||
            (statusValue === "Inactive" && !item.active);

        return matchesSearch && matchesCategory && matchesStatus;
    });

    if (filteredItems.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="8" class="empty-message">
                    No matching menu items found.
                </td>
            </tr>`;

        return;
    }

    tableBody.innerHTML = filteredItems.map(item => {

        const stock = Number(item.stockQuantity);

        const stockClass =
            stock === 0 ? "out-stock" :
            stock <= 5 ? "low-stock" : "in-stock";

        const statusClass = item.active ? "completed" : "cancelled";

        return `
            <tr>

                <td>#${escapeHTML(item.itemId)}</td>

                <td>
                    <strong>${escapeHTML(item.itemName)}</strong>
                </td>

                <td>${escapeHTML(item.category)}</td>

                <td>${formatCurrency(item.price)}</td>

                <td>
                    <span class="status-indicator ${stockClass}"></span>
                    ${stock}
                </td>

                <td>${escapeHTML(item.vendorName || "N/A")}</td>

                <td>
                    <span class="order-status ${statusClass}">
                        ${item.active ? "ACTIVE" : "INACTIVE"}
                    </span>
                </td>

                <td>
                    <div class="management-actions">

                        <button class="menu-edit-btn"
                                data-action="edit"
                                data-id="${item.itemId}"
                                ${!item.active ? "disabled" : ""}>
                            Edit
                        </button>

                        <button class="menu-delete-btn"
                                data-action="deactivate"
                                data-id="${item.itemId}"
                                ${!item.active ? "disabled" : ""}>
                            Deactivate
                        </button>

                    </div>
                </td>

            </tr>`;
    }).join("");
}


// ==========================================
// 18. MENU MANAGEMENT MODAL
// ==========================================

function openMenuModal(item = null) {
    const modal = document.getElementById("menuItemModal");

    document.getElementById("menuItemForm").reset();

    document.getElementById("menuItemId").value = "";

    if (item) {

        document.getElementById("menuModalTitle").textContent =
            "Edit Menu Item";

        document.getElementById("menuItemId").value = item.itemId;
        document.getElementById("menuItemName").value = item.itemName;
        document.getElementById("menuItemCategory").value = item.category;
        document.getElementById("menuItemPrice").value = item.price;
        document.getElementById("menuItemStock").value = item.stockQuantity;
        document.getElementById("menuVendorName").value = item.vendorName || "";
        document.getElementById("menuVendorPhone").value = item.vendorPhone || "";

        document.getElementById("saveMenuItem").textContent =
            "Update Menu Item";

    } else {

        document.getElementById("menuModalTitle").textContent =
            "Add New Menu Item";

        document.getElementById("saveMenuItem").textContent =
            "Save Menu Item";
    }

    modal.style.display = "flex";
}

function closeMenuModal() {
    document.getElementById("menuItemModal").style.display = "none";
}


// ==========================================
// 19. ADD / EDIT MENU ITEM
// ==========================================

function initializeMenuManagementForm() {
    document.getElementById("menuItemForm")
        ?.addEventListener("submit", function(event) {

            event.preventDefault();

            const itemId =
                Number(document.getElementById("menuItemId").value || 0);

            const itemName =
                document.getElementById("menuItemName").value.trim();

            const category =
                document.getElementById("menuItemCategory").value;

            const price =
                Number(document.getElementById("menuItemPrice").value);

            const stock =
                Number(document.getElementById("menuItemStock").value);

            const vendorName =
                document.getElementById("menuVendorName").value.trim();

            const vendorPhone =
                document.getElementById("menuVendorPhone").value.trim();

            if (!itemName || !category) {
                alert("Please enter the item name and category.");
                return;
            }

            if (!Number.isFinite(price) || price <= 0) {
                alert("Enter a valid price greater than zero.");
                return;
            }

            if (!Number.isInteger(stock) || stock < 0) {
                alert("Enter a valid non-negative whole-number stock quantity.");
                return;
            }

            try {
                const bridge = getBridge();

                let success;

                if (itemId > 0) {

                    success = bridge.updateMenuItem(
                        itemId,
                        itemName,
                        category,
                        price,
                        stock,
                        vendorName,
                        vendorPhone
                    );

                } else {

                    success = bridge.addMenuItem(
                        itemName,
                        category,
                        price,
                        stock,
                        vendorName,
                        vendorPhone
                    );
                }

                if (success) {

                    alert(
                        itemId > 0
                            ? "Menu item updated successfully!"
                            : "Menu item added successfully!"
                    );

                    closeMenuModal();

                    loadManagementMenuItems();
                    loadDashboardStats();

                    // Refresh Food Ordering data for the next visit.
                    loadMenuItems();

                } else {

                    alert("Unable to save menu item. Check the Eclipse Console.");
                }

            } catch (error) {

                console.error("Menu item save failed:", error);
                alert("Error saving menu item: " + error.message);
            }
        });
}


// ==========================================
// 20. MENU MANAGEMENT BUTTON EVENTS
// ==========================================

function initializeMenuManagementEvents() {

    document.getElementById("openAddMenuModal")
        ?.addEventListener("click", function() {
            openMenuModal();
        });

    document.getElementById("closeMenuModal")
        ?.addEventListener("click", closeMenuModal);

    document.getElementById("cancelMenuModal")
        ?.addEventListener("click", closeMenuModal);

    document.getElementById("refreshMenuManagement")
        ?.addEventListener("click", loadManagementMenuItems);

    document.getElementById("menuSearchInput")
        ?.addEventListener("input", displayManagementMenuItems);

    document.getElementById("managementCategoryFilter")
        ?.addEventListener("change", displayManagementMenuItems);

    document.getElementById("managementStatusFilter")
        ?.addEventListener("change", displayManagementMenuItems);

    document.getElementById("managementMenuBody")
        ?.addEventListener("click", function(event) {

            const button = event.target.closest("[data-action]");

            if (!button || button.disabled) return;

            const itemId = Number(button.dataset.id);
            const action = button.dataset.action;

            const item = managementMenuItems.find(
                menuItem => Number(menuItem.itemId) === itemId
            );

            if (!item) return;

            if (action === "edit") {

                openMenuModal(item);

            } else if (action === "deactivate") {

                const confirmed = confirm(
                    "Are you sure you want to deactivate " +
                    item.itemName + "?"
                );

                if (!confirmed) return;

                try {

                    const success =
                        getBridge().deactivateMenuItem(itemId);

                    if (success) {

                        alert("Menu item deactivated successfully!");

                        loadManagementMenuItems();
                        loadDashboardStats();
                        loadMenuItems();

                    } else {

                        alert("Unable to deactivate this item.");
                    }

                } catch (error) {

                    console.error("Deactivation failed:", error);
                    alert("Error deactivating menu item.");
                }
            }
        });

    document.getElementById("menuItemModal")
        ?.addEventListener("click", function(event) {

            if (event.target === this) {
                closeMenuModal();
            }
        });
}
// ==========================================
// 21. INVENTORY - LOAD ITEMS
// ==========================================

function loadInventoryItems() {
    const tableBody = document.getElementById("inventoryTableBody");

    if (!tableBody) return;

    tableBody.innerHTML = `
        <tr>
            <td colspan="7">Loading inventory...</td>
        </tr>`;

    try {
        const response = getBridge().getMenuItemsJson();

        if (typeof response !== "string") {
            throw new Error("Invalid response from Java backend.");
        }

        inventoryItems = JSON.parse(response);

        if (!Array.isArray(inventoryItems)) {
            throw new Error("Invalid inventory data.");
        }

        updateInventoryStats();
        displayInventoryItems();

        console.log("Inventory loaded:", inventoryItems.length);

    } catch (error) {
        console.error("Inventory loading failed:", error);

        tableBody.innerHTML = `
            <tr>
                <td colspan="7">
                    ${escapeHTML(error.message)}
                </td>
            </tr>`;
    }
}


// ==========================================
// 22. INVENTORY STATISTICS
// ==========================================

function updateInventoryStats() {
    const total = inventoryItems.length;

    const inStock = inventoryItems.filter(
        item => Number(item.stockQuantity) > 5
    ).length;

    const lowStock = inventoryItems.filter(item => {
        const stock = Number(item.stockQuantity);
        return stock >= 1 && stock <= 5;
    }).length;

    const outOfStock = inventoryItems.filter(
        item => Number(item.stockQuantity) === 0
    ).length;

    document.getElementById("inventoryTotalItems").textContent = total;

    document.getElementById("inventoryInStock").textContent = inStock;

    document.getElementById("inventoryLowStock").textContent = lowStock;

    document.getElementById("inventoryOutOfStock").textContent = outOfStock;
}


// ==========================================
// 23. INVENTORY SEARCH AND FILTER
// ==========================================

function displayInventoryItems() {
    const tableBody = document.getElementById("inventoryTableBody");

    if (!tableBody) return;

    const searchValue =
        (document.getElementById("inventorySearch")?.value || "")
            .trim().toLowerCase();

    const statusValue =
        document.getElementById("inventoryStatusFilter")?.value || "All";

    const filteredItems = inventoryItems.filter(item => {

        const name = String(item.itemName || "").toLowerCase();
        const category = String(item.category || "").toLowerCase();

        const stock = Number(item.stockQuantity);

        const matchesSearch =
            name.includes(searchValue) ||
            category.includes(searchValue);

        let stockStatus;

        if (stock === 0) {
            stockStatus = "Out of Stock";
        } else if (stock <= 5) {
            stockStatus = "Low Stock";
        } else {
            stockStatus = "In Stock";
        }

        const matchesStatus =
            statusValue === "All" ||
            statusValue === stockStatus;

        return matchesSearch && matchesStatus;
    });

    if (filteredItems.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-message">
                    No matching inventory items found.
                </td>
            </tr>`;

        return;
    }

    tableBody.innerHTML = filteredItems.map(item => {

        const stock = Number(item.stockQuantity);

        const stockClass =
            stock === 0 ? "out-stock" :
            stock <= 5 ? "low-stock" : "in-stock";

        const statusText =
            stock === 0 ? "OUT OF STOCK" :
            stock <= 5 ? "LOW STOCK" : "IN STOCK";

        return `
            <tr>

                <td>#${escapeHTML(item.itemId)}</td>

                <td>
                    <strong>${escapeHTML(item.itemName)}</strong>
                </td>

                <td>${escapeHTML(item.category || "N/A")}</td>

                <td>
                    <span class="status-indicator ${stockClass}"></span>
                    ${stock}
                </td>

                <td>
                    <span class="order-status ${stockClass}">
                        ${statusText}
                    </span>
                </td>

                <td>
                    <input
                        type="number"
                        class="inventory-quantity-input"
                        data-stock-input="${escapeHTML(item.itemId)}"
                        min="0"
                        step="1"
                        value="${stock}"
                        aria-label="New stock quantity">
                </td>

                <td>
                    <button
                        type="button"
                        class="inventory-update-btn"
                        data-stock-update="${escapeHTML(item.itemId)}">
                        Update Stock
                    </button>
                </td>

            </tr>`;
    }).join("");
}


// ==========================================
// 24. UPDATE INVENTORY STOCK
// ==========================================

function updateInventoryStock(itemId, button) {

    const input = document.querySelector(
        `[data-stock-input="${itemId}"]`
    );

    if (!input) {
        alert("Stock quantity input was not found.");
        return;
    }

    const newQuantity = Number(input.value);

    if (!Number.isInteger(newQuantity) || newQuantity < 0) {
        alert("Enter a valid whole-number stock quantity (0 or greater).");
        input.focus();
        return;
    }

    const item = inventoryItems.find(
        menuItem => Number(menuItem.itemId) === Number(itemId)
    );

    if (!item) {
        alert("Inventory item not found. Refresh and try again.");
        return;
    }

    if (newQuantity === Number(item.stockQuantity)) {
        alert("The stock quantity has not changed.");
        return;
    }

    button.disabled = true;
    button.textContent = "Updating...";

    try {

        const success = getBridge().updateStock(
            Number(itemId),
            newQuantity
        );

        if (success) {

            alert("Stock quantity updated successfully!");

            loadInventoryItems();
            loadManagementMenuItems();
            loadDashboardStats();
            loadMenuItems();

        } else {

            alert("Unable to update stock. Please try again.");

            button.disabled = false;
            button.textContent = "Update Stock";
        }

    } catch (error) {

        console.error("Inventory stock update failed:", error);

        alert("Error updating stock: " + error.message);

        button.disabled = false;
        button.textContent = "Update Stock";
    }
}


// ==========================================
// 25. INVENTORY EVENT LISTENERS
// ==========================================

function initializeInventoryEvents() {

    document.getElementById("refreshInventory")
        ?.addEventListener("click", loadInventoryItems);

    document.getElementById("inventorySearch")
        ?.addEventListener("input", displayInventoryItems);

    document.getElementById("inventoryStatusFilter")
        ?.addEventListener("change", displayInventoryItems);

    document.getElementById("inventoryTableBody")
        ?.addEventListener("click", function(event) {

            const button = event.target.closest("[data-stock-update]");

            if (!button || button.disabled) return;

            const itemId = Number(button.dataset.stockUpdate);

            updateInventoryStock(itemId, button);
        });
}


// ==========================================
// 26. APPLICATION INITIALIZATION
// ==========================================

document.addEventListener("DOMContentLoaded", function() {

    displayCurrentDate();

    initializeNavigation();

    initializeMenuEvents();

    initializeCartEvents();

    initializeCheckout();

    initializeMenuManagementForm();

    initializeMenuManagementEvents();

    initializeInventoryEvents();
	
	initializeBillingEvents();
	
	initializeVendorNotificationEvents();
	
	initializeDashboardSalesEvents();
	
	initializeSalesReportEvents();

    updateCartUI();

    console.log("DineFlow JavaScript initialized successfully.");

});

// ==========================================
// 27. BILLING MODULE
// ==========================================

// Load Billing dashboard statistics
function loadBillingStats() {
    try {
        const stats = JSON.parse(getBridge().getBillingStatsJson());

        if (!stats.success) {
            throw new Error(stats.message || "Unable to load billing statistics.");
        }

        document.getElementById("billingTotalOrders").textContent =
            stats.totalOrders;

        document.getElementById("billingTotalRevenue").textContent =
            formatCurrency(stats.totalRevenue);

        document.getElementById("billingSuccessfulPayments").textContent =
            stats.successfulPayments;

        document.getElementById("billingPendingPayments").textContent =
            stats.pendingPayments;

    } catch (error) {
        console.error("Billing statistics error:", error);
    }
}


// Load all billing orders
function loadBillingOrders() {
    const tableBody = document.getElementById("billingOrdersBody");

    if (!tableBody) return;

    try {
        const orders = JSON.parse(getBridge().getBillingOrdersJson());

        if (!Array.isArray(orders)) {
            throw new Error(orders.message || "Invalid billing orders response.");
        }

        displayBillingOrders(orders);

    } catch (error) {
        console.error("Unable to load billing orders:", error);

        tableBody.innerHTML = `
            <tr>
                <td colspan="10" class="empty-message">
                    Unable to load billing records.
                </td>
            </tr>
        `;
    }
}


// Display billing records in the table
function displayBillingOrders(orders) {
    const tableBody = document.getElementById("billingOrdersBody");

    if (!tableBody) return;

    const searchValue =
        document.getElementById("billingSearch")?.value
            .trim().toLowerCase() || "";

    const paymentFilter =
        document.getElementById("billingPaymentFilter")?.value || "All";

    const filteredOrders = orders.filter(order => {
        const matchesSearch =
            String(order.orderId).includes(searchValue) ||
            String(order.customerName || "").toLowerCase().includes(searchValue);

        const matchesPayment =
            paymentFilter === "All" ||
            String(order.paymentMethod || "").toUpperCase() === paymentFilter;

        return matchesSearch && matchesPayment;
    });

    if (filteredOrders.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="10" class="empty-message">
                    No billing records found.
                </td>
            </tr>
        `;
        return;
    }

    tableBody.innerHTML = filteredOrders.map(order => {
        const paymentStatus =
            String(order.paymentStatus || "PENDING").toUpperCase();

        const statusClass =
            ["PAID", "SUCCESS"].includes(paymentStatus)
                ? "completed"
                : paymentStatus === "FAILED"
                    ? "cancelled"
                    : "pending";

        const orderDate = order.orderDate
            ? new Date(order.orderDate).toLocaleString("en-IN")
            : "N/A";

        return `
            <tr>
                <td>#${escapeHTML(order.orderId)}</td>
                <td>${escapeHTML(order.customerName || "Walk-in Customer")}</td>
                <td>${escapeHTML(orderDate)}</td>
                <td>${formatCurrency(order.subtotal)}</td>
                <td>${formatCurrency(order.tax)}</td>
                <td>${formatCurrency(order.discount)}</td>
                <td><strong>${formatCurrency(order.total)}</strong></td>
                <td>${escapeHTML(order.paymentMethod || "N/A")}</td>
                <td>
                    <span class="order-status ${statusClass}">
                        ${escapeHTML(paymentStatus)}
                    </span>
                </td>
                <td>
                    <button
                        class="btn btn-primary"
                        data-generate-invoice="${Number(order.orderId)}">
                        Generate PDF
                    </button>
                </td>
            </tr>
        `;
    }).join("");
}


// Generate invoice for a selected order
function generateBillingInvoice(orderId, button) {
    if (!Number.isInteger(orderId) || orderId <= 0) {
        alert("Invalid order ID.");
        return;
    }

    try {
        button.disabled = true;
        button.textContent = "Generating...";

        const response = JSON.parse(
            getBridge().generateInvoice(orderId)
        );

        if (!response.success) {
            throw new Error(response.message || "Invoice generation failed.");
        }

        alert(
            "Invoice generated successfully!\n\n" +
            "Saved location:\n" + response.filePath
        );

        console.log("Invoice saved:", response.filePath);

    } catch (error) {
        console.error("Invoice generation error:", error);
        alert("Unable to generate invoice: " + error.message);

    } finally {
        button.disabled = false;
        button.textContent = "Generate PDF";
    }
}


// Billing event listeners
function initializeBillingEvents() {

    document.getElementById("refreshBilling")
        ?.addEventListener("click", () => {
            loadBillingStats();
            loadBillingOrders();
        });

    document.getElementById("billingSearch")
        ?.addEventListener("input", loadBillingOrders);

    document.getElementById("billingPaymentFilter")
        ?.addEventListener("change", loadBillingOrders);

    document.getElementById("billingOrdersBody")
        ?.addEventListener("click", event => {

            const button = event.target.closest("[data-generate-invoice]");

            if (!button || button.disabled) return;

            const orderId = Number(button.dataset.generateInvoice);

            generateBillingInvoice(orderId, button);
        });
}
// ==========================================
// VENDOR NOTIFICATIONS
// ==========================================

let vendorNotifications = [];

function loadVendorNotifications() {
    try {
        const bridge = getBridge();

        if (!bridge) {
            console.error("Java bridge not available.");
            return;
        }

        const response = JSON.parse(
            bridge.getVendorNotificationsJson()
        );

        if (!Array.isArray(response)) {
            console.error("Unable to load vendor notifications:", response.message);
            return;
        }

        vendorNotifications = response;

        updateVendorNotificationStats();
        displayVendorNotifications();

    } catch (error) {
        console.error("Error loading vendor notifications:", error);
    }
}

function updateVendorNotificationStats() {
    const total = vendorNotifications.length;

    const sent = vendorNotifications.filter(n =>
        ["SENT", "SUCCESS"].includes(
            String(n.status || "").toUpperCase()
        )
    ).length;

    const pending = vendorNotifications.filter(n =>
        String(n.status || "").toUpperCase() === "PENDING"
    ).length;

    const failed = vendorNotifications.filter(n =>
        String(n.status || "").toUpperCase() === "FAILED"
    ).length;

    document.getElementById("vendorTotalNotifications").textContent = total;
    document.getElementById("vendorSentNotifications").textContent = sent;
    document.getElementById("vendorPendingNotifications").textContent = pending;
    document.getElementById("vendorFailedNotifications").textContent = failed;
}

function displayVendorNotifications() {
    const searchValue = (
        document.getElementById("vendorNotificationSearch").value || ""
    ).toLowerCase();

    const statusFilter = (
        document.getElementById("vendorNotificationStatus").value || "ALL"
    ).toUpperCase();

    const filteredNotifications = vendorNotifications.filter(n => {
        const status = String(n.status || "").toUpperCase();

        const matchesSearch = [
            n.notificationId,
            n.itemName,
            n.vendorName,
            n.vendorPhone,
            n.message
        ].some(value =>
            String(value || "").toLowerCase().includes(searchValue)
        );

        const matchesStatus =
            statusFilter === "ALL" ||
            status === statusFilter ||
            (statusFilter === "SENT" && status === "SUCCESS");

        return matchesSearch && matchesStatus;
    });

    const tbody = document.getElementById("vendorNotificationsBody");

    if (filteredNotifications.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align:center;">
                    No vendor notifications found.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filteredNotifications.map(n => {
        const status = String(n.status || "PENDING").toUpperCase();

        let statusClass = "pending";
        if (["SENT", "SUCCESS"].includes(status)) {
            statusClass = "success";
        } else if (status === "FAILED") {
            statusClass = "danger";
        }

        return `
            <tr>
                <td>${escapeHTML(n.notificationId)}</td>
                <td>${escapeHTML(n.itemName || "-")}</td>
                <td>${escapeHTML(n.vendorName || "-")}</td>
                <td>${escapeHTML(n.vendorPhone || "-")}</td>
                <td>${escapeHTML(n.message || "-")}</td>
                <td>
                    <span class="status-badge ${statusClass}">
                        ${escapeHTML(status)}
                    </span>
                </td>
                <td>${escapeHTML(n.attemptCount ?? 0)}</td>
                <td>${escapeHTML(n.createdAt || "-")}</td>
            </tr>
        `;
    }).join("");
}

function initializeVendorNotificationEvents() {
    document.getElementById("refreshVendorNotifications")
        ?.addEventListener("click", loadVendorNotifications);

    document.getElementById("vendorNotificationSearch")
        ?.addEventListener("input", displayVendorNotifications);

    document.getElementById("vendorNotificationStatus")
        ?.addEventListener("change", displayVendorNotifications);
}

// ==========================================
// 28. SALES REPORTS MODULE
// ==========================================

// Store the latest report response
let salesReportData = null;


// Load sales reports from Java backend
function loadSalesReports() {

    const chart = document.getElementById("reportSalesChart");

    if (chart) {
        chart.innerHTML = "<p>Loading sales reports...</p>";
    }

    try {
        const period = document.getElementById("reportPeriod").value;
        const startDate = document.getElementById("reportStartDate").value;
        const endDate = document.getElementById("reportEndDate").value;

        // Validate custom date range
        if ((startDate && !endDate) || (!startDate && endDate)) {
            throw new Error("Please select both start and end dates.");
        }

        if (startDate && endDate && endDate < startDate) {
            throw new Error("End date cannot be before start date.");
        }

        const response = getBridge().getSalesReportJson(
            period,
            startDate,
            endDate
        );

        const report = JSON.parse(response);

        if (!report.success) {
            throw new Error(report.message || "Unable to load sales reports.");
        }

        salesReportData = report;

        updateSalesSummary(report.summary);
        displayRevenueTrend(report.trend);
        displayTopSellingItems(report.topSellingItems);
        displayPaymentAnalysis(report.paymentAnalysis);
        displaySalesBreakdown(report.breakdown);

        console.log("Sales reports loaded successfully:", report);

    } catch (error) {

        console.error("Sales report loading failed:", error);

        if (chart) {
            chart.innerHTML =
                `<p class="empty-message">${escapeHTML(error.message)}</p>`;
        }

        alert("Unable to load sales reports: " + error.message);
    }
}


// ------------------------------------------
// 1. UPDATE SALES SUMMARY CARDS
// ------------------------------------------

function updateSalesSummary(summary) {

    if (!summary) return;

    document.getElementById("reportTotalRevenue").textContent =
        formatCurrency(summary.totalRevenue);

    document.getElementById("reportTotalOrders").textContent =
        Number(summary.totalOrders || 0);

    document.getElementById("reportAverageOrder").textContent =
        formatCurrency(summary.averageOrderValue);

    document.getElementById("reportItemsSold").textContent =
        Number(summary.itemsSold || 0);
}


// ------------------------------------------
// 2. DISPLAY REVENUE TREND CHART
// ------------------------------------------

function displayRevenueTrend(trend) {

    const container = document.getElementById("reportSalesChart");

    if (!container) return;

    if (!Array.isArray(trend) || trend.length === 0) {
        container.innerHTML =
            `<p class="empty-message">No sales data available for this period.</p>`;
        return;
    }

    const maxRevenue = Math.max(
        ...trend.map(row => Number(row.revenue || 0)),
        1
    );

    container.innerHTML = `
        <div style="
            display:flex;
            align-items:flex-end;
            gap:16px;
            min-height:240px;
            padding:20px 10px;
            overflow-x:auto;
        ">
            ${trend.map(row => {

                const revenue = Number(row.revenue || 0);
                const height = Math.max(
                    (revenue / maxRevenue) * 180,
                    4
                );

                const date = new Date(row.date + "T00:00:00")
                    .toLocaleDateString("en-IN", {
                        day: "numeric",
                        month: "short"
                    });

                return `
                    <div style="
                        min-width:65px;
                        flex:1;
                        display:flex;
                        flex-direction:column;
                        align-items:center;
                        justify-content:flex-end;
                        gap:8px;
                    ">
                        <strong style="font-size:11px;">
                            ${formatCurrency(revenue)}
                        </strong>

                        <div
                            title="${escapeHTML(row.date)}: ${formatCurrency(revenue)}"
                            style="
                                width:70%;
                                height:${height}px;
                                min-height:4px;
                                background:linear-gradient(180deg,#ffad42,#ed8e2f);
                                border-radius:7px 7px 0 0;
                            ">
                        </div>

                        <span style="font-size:12px;white-space:nowrap;">
                            ${escapeHTML(date)}
                        </span>
                    </div>
                `;
            }).join("")}
        </div>
    `;
}


// ------------------------------------------
// 3. DISPLAY TOP-SELLING ITEMS
// ------------------------------------------

function displayTopSellingItems(items) {

    const tbody = document.getElementById("topSellingItemsBody");

    if (!tbody) return;

    if (!Array.isArray(items) || items.length === 0) {

        tbody.innerHTML = `
            <tr>
                <td colspan="3" class="empty-message">
                    No item sales recorded for this period.
                </td>
            </tr>
        `;

        return;
    }

    tbody.innerHTML = items.map((item, index) => `

        <tr>
            <td>
                <strong>${index + 1}. ${escapeHTML(item.itemName)}</strong>
            </td>

            <td>${Number(item.quantitySold || 0)}</td>

            <td>${formatCurrency(item.revenue)}</td>
        </tr>

    `).join("");
}


// ------------------------------------------
// 4. DISPLAY PAYMENT ANALYSIS
// ------------------------------------------

function displayPaymentAnalysis(payments) {

    const container = document.getElementById("paymentAnalysisBody");

    if (!container) return;

    if (!Array.isArray(payments) || payments.length === 0) {

        container.innerHTML = `
            <p class="empty-message">
                No successful payments recorded for this period.
            </p>
        `;

        return;
    }

    const totalRevenue = payments.reduce(
        (sum, payment) => sum + Number(payment.revenue || 0),
        0
    );

    container.innerHTML = payments.map(payment => {

        const revenue = Number(payment.revenue || 0);

        const percentage = totalRevenue > 0
            ? (revenue / totalRevenue) * 100
            : 0;

        return `
            <div style="margin-bottom:22px;">

                <div style="
                    display:flex;
                    justify-content:space-between;
                    margin-bottom:8px;
                ">
                    <strong>${escapeHTML(payment.paymentMethod)}</strong>

                    <strong>${formatCurrency(revenue)}</strong>
                </div>

                <div style="
                    width:100%;
                    height:10px;
                    background:#edf0f5;
                    border-radius:10px;
                    overflow:hidden;
                ">
                    <div style="
                        width:${percentage}%;
                        height:100%;
                        background:#f29d3f;
                        border-radius:10px;
                    "></div>
                </div>

                <p style="font-size:12px;margin-top:6px;color:#8792a8;">
                    ${Number(payment.orders || 0)} orders
                    · ${percentage.toFixed(1)}% of revenue
                </p>

            </div>
        `;

    }).join("");
}


// ------------------------------------------
// 5. DISPLAY DAILY SALES BREAKDOWN
// ------------------------------------------

function displaySalesBreakdown(breakdown) {

    const tbody = document.getElementById("salesBreakdownBody");

    if (!tbody) return;

    if (!Array.isArray(breakdown) || breakdown.length === 0) {

        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="empty-message">
                    No sales breakdown available for this period.
                </td>
            </tr>
        `;

        return;
    }

    tbody.innerHTML = breakdown.map(row => {

        const date = new Date(row.date + "T00:00:00")
            .toLocaleDateString("en-IN", {
                day: "2-digit",
                month: "short",
                year: "numeric"
            });

        return `
            <tr>

                <td>${escapeHTML(date)}</td>

                <td>${Number(row.orders || 0)}</td>

                <td>${Number(row.itemsSold || 0)}</td>

                <td>
                    <strong>${formatCurrency(row.revenue)}</strong>
                </td>

            </tr>
        `;

    }).join("");
}


// ------------------------------------------
// 6. SALES REPORT EVENT LISTENERS
// ------------------------------------------

function initializeSalesReportEvents() {

    // Refresh report
    document.getElementById("refreshReports")
        ?.addEventListener("click", loadSalesReports);

    // Apply date and period filters
    document.getElementById("applyReportFilter")
        ?.addEventListener("click", loadSalesReports);

}

// ==========================================
// 29. DASHBOARD SALES OVERVIEW
// ==========================================

function loadDashboardSalesOverview() {

    const chart = document.getElementById("salesChart");
    const totalElement = document.getElementById("dashboardSalesRevenue");
    const periodElement = document.getElementById("dashboardSalesPeriod");

    if (!chart || !periodElement) return;

    try {
        const period = periodElement.value || "WEEK";

        const response = getBridge().getSalesReportJson(
            period,
            "",
            ""
        );

        const report = JSON.parse(response);

        if (!report.success) {
            throw new Error(report.message || "Unable to load sales overview.");
        }

        const revenue = Number(report.summary?.totalRevenue || 0);

        if (totalElement) {
            totalElement.textContent = formatCurrency(revenue);
        }

        displayDashboardSalesChart(report.trend || []);

    } catch (error) {

        console.error("Dashboard sales overview error:", error);

        chart.innerHTML = `
            <p class="empty-message">
                Unable to load sales analytics.
            </p>
        `;
    }
}


// Render the dashboard revenue chart
function displayDashboardSalesChart(trend) {

    const chart = document.getElementById("salesChart");

    if (!chart) return;

    if (!Array.isArray(trend) || trend.length === 0) {

        chart.innerHTML = `
            <p class="empty-message">
                No sales recorded for this period yet.
            </p>
        `;

        return;
    }

    const maxRevenue = Math.max(
        ...trend.map(item => Number(item.revenue || 0)),
        1
    );

    chart.innerHTML = `
        <div style="
            display:flex;
            align-items:flex-end;
            gap:14px;
            min-height:230px;
            padding:20px 12px;
            overflow-x:auto;
        ">

            ${trend.map(item => {

                const revenue = Number(item.revenue || 0);

                const height = Math.max(
                    (revenue / maxRevenue) * 155,
                    4
                );

                const date = new Date(item.date + "T00:00:00");

                const label = date.toLocaleDateString("en-IN", {
                    day: "2-digit",
                    month: "short"
                });

                return `
                    <div style="
                        flex:1;
                        min-width:55px;
                        display:flex;
                        flex-direction:column;
                        align-items:center;
                        justify-content:flex-end;
                        gap:8px;
                    ">

                        <span style="
                            font-size:11px;
                            color:#718096;
                            white-space:nowrap;
                        ">
                            ${formatCurrency(revenue)}
                        </span>

                        <div
                            title="${escapeHTML(item.date)}: ${formatCurrency(revenue)}"
                            style="
                                width:65%;
                                height:${height}px;
                                min-height:4px;
                                background:linear-gradient(180deg,#ffb34d,#ef9638);
                                border-radius:7px 7px 0 0;
                            ">
                        </div>

                        <span style="
                            font-size:11px;
                            color:#718096;
                            white-space:nowrap;
                        ">
                            ${escapeHTML(label)}
                        </span>

                    </div>
                `;

            }).join("")}

        </div>
    `;
}


// Initialize dashboard chart controls
function initializeDashboardSalesEvents() {

    document.getElementById("dashboardSalesPeriod")
        ?.addEventListener("change", loadDashboardSalesOverview);

}
