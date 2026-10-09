package com.fieldnote.store.repository;

import com.fieldnote.store.model.Order;
import com.fieldnote.store.model.Order.OrderLine;
import com.fieldnote.store.model.Product;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MySqlStoreRepository implements StoreRepository {
    private final String url;
    private final String username;
    private final String password;

    public MySqlStoreRepository(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initializeDatabase();
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("MySQL JDBC driver is missing from the application classpath.", exception);
        } catch (SQLException exception) {
            throw databaseFailure("Could not initialize the database", exception);
        }
    }

    @Override
    public List<Product> findAllProducts() {
        String sql = "SELECT id, name, description, category, price_cents, stock, image_url FROM products ORDER BY id";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<Product> products = new ArrayList<>();
            while (results.next()) products.add(readProduct(results));
            return List.copyOf(products);
        } catch (SQLException exception) {
            throw databaseFailure("Could not load products", exception);
        }
    }

    @Override
    public Product findProduct(int id) {
        String sql = "SELECT id, name, description, category, price_cents, stock, image_url FROM products WHERE id = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? readProduct(results) : null;
            }
        } catch (SQLException exception) {
            throw databaseFailure("Could not load product", exception);
        }
    }

    @Override
    public Order placeOrder(String customerName, String email, Map<Integer, Integer> quantities) {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                Order order = placeOrder(connection, customerName, email, quantities);
                connection.commit();
                return order;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                if (exception instanceof IllegalArgumentException invalidOrder) throw invalidOrder;
                throw databaseFailure("Could not place order", exception);
            }
        } catch (SQLException exception) {
            throw databaseFailure("Could not connect to the database", exception);
        }
    }

    private Order placeOrder(Connection connection, String customerName, String email,
                             Map<Integer, Integer> quantities) throws SQLException {
        List<OrderLine> lines = new ArrayList<>();
        int total = 0;
        String selectSql = "SELECT name, price_cents, stock FROM products WHERE id = ? FOR UPDATE";
        try (PreparedStatement select = connection.prepareStatement(selectSql)) {
            for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
                select.setInt(1, entry.getKey());
                try (ResultSet result = select.executeQuery()) {
                    int quantity = entry.getValue();
                    if (!result.next() || quantity < 1 || result.getInt("stock") < quantity) {
                        throw new IllegalArgumentException("One or more items are no longer available in that quantity.");
                    }
                    int price = result.getInt("price_cents");
                    lines.add(new OrderLine(result.getString("name"), quantity, price));
                    total += price * quantity;
                }
            }
        }

        int orderId;
        String insertOrder = "INSERT INTO orders (customer_name, email, total_cents) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, customerName);
            statement.setString(2, email);
            statement.setInt(3, total);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("The database did not return an order ID.");
                orderId = keys.getInt(1);
            }
        }

        String updateStock = "UPDATE products SET stock = stock - ? WHERE id = ?";
        String insertItem = "INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price_cents) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement update = connection.prepareStatement(updateStock);
             PreparedStatement insert = connection.prepareStatement(insertItem)) {
            int lineIndex = 0;
            for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
                OrderLine line = lines.get(lineIndex++);
                update.setInt(1, entry.getValue());
                update.setInt(2, entry.getKey());
                update.executeUpdate();

                insert.setLong(1, orderId);
                insert.setInt(2, entry.getKey());
                insert.setString(3, line.productName());
                insert.setInt(4, line.quantity());
                insert.setInt(5, line.unitPriceCents());
                insert.executeUpdate();
            }
        }
        return new Order(orderId, customerName, email, List.copyOf(lines), total);
    }

    private void initializeDatabase() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS products ("
                    + "id INT PRIMARY KEY, name VARCHAR(160) NOT NULL, description TEXT NOT NULL, "
                    + "category VARCHAR(80) NOT NULL, price_cents INT NOT NULL, stock INT NOT NULL, image_url VARCHAR(700) NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS orders ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT, customer_name VARCHAR(80) NOT NULL, "
                    + "email VARCHAR(254) NOT NULL, total_cents INT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS order_items ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, product_id INT NOT NULL, "
                    + "product_name VARCHAR(160) NOT NULL, quantity INT NOT NULL, unit_price_cents INT NOT NULL, "
                    + "CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE)");

            try (ResultSet count = statement.executeQuery("SELECT COUNT(*) FROM products")) {
                if (count.next() && count.getInt(1) == 0) seedProducts(connection);
            }
        }
    }

    private void seedProducts(Connection connection) throws SQLException {
        String sql = "INSERT INTO products (id, name, description, category, price_cents, stock, image_url) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Product product : InMemoryStoreRepository.seededProducts()) {
                statement.setInt(1, product.id());
                statement.setString(2, product.name());
                statement.setString(3, product.description());
                statement.setString(4, product.category());
                statement.setInt(5, product.priceCents());
                statement.setInt(6, product.stock());
                statement.setString(7, product.imageUrl());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private static Product readProduct(ResultSet result) throws SQLException {
        return new Product(result.getInt("id"), result.getString("name"), result.getString("description"),
                result.getString("category"), result.getInt("price_cents"), result.getInt("stock"),
                result.getString("image_url"));
    }

    private static IllegalStateException databaseFailure(String message, Exception cause) {
        return new IllegalStateException(message + ". Check the database service and credentials.", cause);
    }
}