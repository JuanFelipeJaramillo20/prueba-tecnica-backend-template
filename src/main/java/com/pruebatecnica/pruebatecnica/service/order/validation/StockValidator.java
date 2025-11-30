package com.pruebatecnica.pruebatecnica.service.order.validation;

import com.pruebatecnica.pruebatecnica.exception.InsufficientStockException;
import com.pruebatecnica.pruebatecnica.model.OrderLine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StockValidator {

    public void validate(List<OrderLine> orderLines) {
        for (OrderLine line : orderLines) {
            int requested = line.getQuantity();
            int available = line.getProduct().getStock();

            if (available < requested) {
                throw new InsufficientStockException(
                        line.getProduct().getName(),
                        requested,
                        available
                );
            }
        }
    }
}
