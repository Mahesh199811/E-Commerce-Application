package com.fieldnote.store.model;

public record Product(
        int id,
        String name,
        String description,
        String category,
        int priceCents,
        int stock,
        String imageUrl) {
}