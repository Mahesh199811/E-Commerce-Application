package com.fieldnote.store.repository;

import com.fieldnote.store.model.Order;
import com.fieldnote.store.model.Product;

import java.util.List;
import java.util.Map;

public interface StoreRepository {
    List<Product> findAllProducts();

    Product findProduct(int id);

    Order placeOrder(String customerName, String email, Map<Integer, Integer> quantities);
}