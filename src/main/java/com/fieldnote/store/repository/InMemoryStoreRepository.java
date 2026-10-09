package com.fieldnote.store.repository;

import com.fieldnote.store.model.Order;
import com.fieldnote.store.model.Order.OrderLine;
import com.fieldnote.store.model.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class InMemoryStoreRepository implements StoreRepository {
    private final Map<Integer, Product> products = new LinkedHashMap<>();
    private final AtomicInteger nextOrderId = new AtomicInteger(1001);

    public InMemoryStoreRepository() {
        for (Product product : seededProducts()) products.put(product.id(), product);
    }

    static List<Product> seededProducts() {
        return List.of(
                new Product(1, "Arc desk lamp", "A quiet pool of light for late ideas.", "Workspace", 6800, 12,
                        "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=85"),
                new Product(2, "Everyday tote", "Room for the market, the library, and everything between.", "Carry", 3200, 18,
                        "https://images.unsplash.com/photo-1590874103328-eac38a683ce7?auto=format&fit=crop&w=900&q=85"),
                new Product(3, "Ridge ceramic cup", "Hand-finished stoneware, made for a slower morning.", "Kitchen", 2400, 24,
                        "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=900&q=85"),
                new Product(4, "Field notes set", "Three pocket-sized notebooks with paper worth keeping.", "Workspace", 1800, 30,
                        "https://images.unsplash.com/photo-1531346878377-a5be20888e57?auto=format&fit=crop&w=900&q=85"),
                new Product(5, "Sunday glass carafe", "A sculptural pitcher for the everyday table.", "Kitchen", 4600, 9,
                        "https://images.unsplash.com/photo-1513558161293-cdaf765edfd7?auto=format&fit=crop&w=900&q=85"),
                new Product(6, "Canvas market bag", "A sturdy, easy-carry companion in washed cotton.", "Carry", 2800, 15,
                        "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=900&q=85"),
                new Product(7, "Pebble catchall", "A small landing place for keys, rings, and pocket finds.", "Living", 2200, 20,
                        "https://images.unsplash.com/photo-1603006905003-be475563bc59?auto=format&fit=crop&w=900&q=85"),
                new Product(8, "Soft line throw", "A breathable cotton layer for couch-side afternoons.", "Living", 7400, 7,
                        "https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=900&q=85"));
    }

    @Override
    public synchronized List<Product> findAllProducts() {
        return List.copyOf(products.values());
    }

    @Override
    public synchronized Product findProduct(int id) {
        return products.get(id);
    }

    @Override
    public synchronized Order placeOrder(String customerName, String email, Map<Integer, Integer> quantities) {
        List<OrderLine> lines = new ArrayList<>();
        int total = 0;
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            Product product = products.get(entry.getKey());
            int quantity = entry.getValue();
            if (product == null || quantity < 1 || product.stock() < quantity) {
                throw new IllegalArgumentException("One or more items are no longer available in that quantity.");
            }
            lines.add(new OrderLine(product.name(), quantity, product.priceCents()));
            total += product.priceCents() * quantity;
        }
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            Product product = products.get(entry.getKey());
            products.put(product.id(), new Product(product.id(), product.name(), product.description(), product.category(),
                    product.priceCents(), product.stock() - entry.getValue(), product.imageUrl()));
        }
        return new Order(nextOrderId.getAndIncrement(), customerName, email, List.copyOf(lines), total);
    }
}