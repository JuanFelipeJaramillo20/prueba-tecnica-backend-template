package com.pruebatecnica.pruebatecnica.service.order;

import com.pruebatecnica.pruebatecnica.dto.CreateOrderRequest;
import com.pruebatecnica.pruebatecnica.dto.OrderItemRequest;
import com.pruebatecnica.pruebatecnica.exception.ProductNotFoundException;
import com.pruebatecnica.pruebatecnica.model.OrderLine;
import com.pruebatecnica.pruebatecnica.model.Product;
import com.pruebatecnica.pruebatecnica.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderLinesFactory {

    private final ProductRepository productRepository;

    public OrderLinesFactory(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Convierte la lista de OrderItemRequest en OrderLine (producto + cantidad).
     */
    public List<OrderLine> fromRequest(CreateOrderRequest request) {
        return request.getItems().stream()
                .map(this::toOrderLine)
                .toList();
    }

    private OrderLine toOrderLine(OrderItemRequest itemRequest) {
        Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(itemRequest.getProductId()));
        return new OrderLine(product, itemRequest.getQuantity());
    }
}

