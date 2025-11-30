package com.pruebatecnica.pruebatecnica.service.order.pricing;

import com.pruebatecnica.pruebatecnica.model.OrderLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PriceCalculator {

    public BigDecimal calculateSubtotal(List<OrderLine> orderLines) {
        return orderLines.stream()
                .map(this::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal lineTotal(OrderLine line) {
        return line.getProduct()
                .getPrice()
                .multiply(BigDecimal.valueOf(line.getQuantity()));
    }
}
