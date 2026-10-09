package com.fieldnote.store.model;

import java.util.List;

public record Order(int id, String customerName, String email, List<OrderLine> items, int totalCents) {
    public record OrderLine(String productName, int quantity, int unitPriceCents) {
    }
}