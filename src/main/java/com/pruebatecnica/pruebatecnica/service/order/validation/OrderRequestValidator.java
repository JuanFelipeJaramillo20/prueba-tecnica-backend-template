package com.pruebatecnica.pruebatecnica.service.order.validation;

import com.pruebatecnica.pruebatecnica.dto.CreateOrderRequest;
import com.pruebatecnica.pruebatecnica.dto.OrderItemRequest;
import org.springframework.stereotype.Component;

@Component
public class OrderRequestValidator {

    public void validate(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (isBlank(request.getCustomerName())) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (isBlank(request.getCustomerEmail())) {
            throw new IllegalArgumentException("Customer email is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items are required");
        }

        request.getItems().forEach(this::validateItem);
    }

    private void validateItem(OrderItemRequest itemRequest) {
        if (itemRequest.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
