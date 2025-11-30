package com.pruebatecnica.pruebatecnica.service.order.discount;

import com.pruebatecnica.pruebatecnica.model.OrderLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class VarietyDiscountPolicy implements DiscountPolicy {

    private static final BigDecimal VARIETY_DISCOUNT_RATE = new BigDecimal("0.10");
    private static final int MINIMUM_DISTINCT_PRODUCTS_FOR_DISCOUNT = 4;

    @Override
    public BigDecimal applyDiscount(BigDecimal subtotal, List<OrderLine> orderLines) {
        long distinctProducts = countDistinctProducts(orderLines);

        if (hasEnoughVariety(distinctProducts)) {
            BigDecimal discount = subtotal.multiply(VARIETY_DISCOUNT_RATE);
            return subtotal.subtract(discount);
        }

        return subtotal;
    }

    private long countDistinctProducts(List<OrderLine> orderLines) {
        return orderLines.stream()
                .map(line -> line.getProduct().getId())
                .distinct()
                .count();
    }

    private boolean hasEnoughVariety(long distinctProducts) {
        return distinctProducts >= MINIMUM_DISTINCT_PRODUCTS_FOR_DISCOUNT;
    }
}

