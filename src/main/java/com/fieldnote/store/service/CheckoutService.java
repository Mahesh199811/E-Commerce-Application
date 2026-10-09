package com.fieldnote.store.service;

import com.fieldnote.store.model.Order;
import com.fieldnote.store.repository.StoreRepository;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CheckoutService {
    private final StoreRepository repository;

    public CheckoutService(StoreRepository repository) {
        this.repository = repository;
    }

    public Order checkout(String customerName, String email, String items) {
        String name = customerName == null ? "" : customerName.trim();
        String address = email == null ? "" : email.trim();
        if (name.length() < 2 || name.length() > 80) {
            throw new IllegalArgumentException("Enter your name (at least 2 characters).");
        }
        if (!address.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (items == null || items.isBlank() || items.length() > 500) {
            throw new IllegalArgumentException("Your cart is empty.");
        }

        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        try {
            for (String item : items.split(",")) {
                String[] parts = item.split(":", -1);
                if (parts.length != 2) {
                    throw new NumberFormatException();
                }
                int id = Integer.parseInt(parts[0]);
                int quantity = Integer.parseInt(parts[1]);
                if (id < 1 || quantity < 1 || quantity > 20 || quantities.putIfAbsent(id, quantity) != null) {
                    throw new NumberFormatException();
                }
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Your cart contains an invalid item.");
        }
        return repository.placeOrder(name, address, quantities);
    }
}