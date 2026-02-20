package com.kalon.exception;

import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {
    private final int availableStock;
    private final int currentCartQuantity;
    private final int maxCanAdd;
    private final int requestedQuantity;

    public InsufficientStockException(int availableStock, int currentCartQuantity, int maxCanAdd, int requestedQuantity) {
        super(String.format("Only %d items available in stock (you have %d in cart)", availableStock, currentCartQuantity));
        this.availableStock = availableStock;
        this.currentCartQuantity = currentCartQuantity;
        this.maxCanAdd = maxCanAdd;
        this.requestedQuantity = requestedQuantity;
    }
}
