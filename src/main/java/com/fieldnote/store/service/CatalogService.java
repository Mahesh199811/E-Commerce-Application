package com.fieldnote.store.service;

import com.fieldnote.store.model.Product;
import com.fieldnote.store.repository.StoreRepository;

import java.util.List;

public final class CatalogService {
    private final StoreRepository repository;

    public CatalogService(StoreRepository repository) {
        this.repository = repository;
    }

    public List<Product> listProducts() {
        return repository.findAllProducts();
    }
}