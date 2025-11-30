package com.pruebatecnica.pruebatecnica.service.discount;

import com.pruebatecnica.pruebatecnica.model.OrderLine;
import com.pruebatecnica.pruebatecnica.model.Product;
import com.pruebatecnica.pruebatecnica.service.order.discount.VarietyDiscountPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VarietyDiscountPolicyTest {

    private final VarietyDiscountPolicy discountPolicy = new VarietyDiscountPolicy();

    @Test
    void given3OrLessProductTypes_shouldNotApplyDiscount() {
        Product p1 = new Product("Manzana", BigDecimal.valueOf(10.00), 10); p1.setId(1L);
        Product p2 = new Product("Pera",    BigDecimal.valueOf(20.00), 10); p2.setId(2L);
        Product p3 = new Product("Uva",     BigDecimal.valueOf(30.00), 10); p3.setId(3L);

        List<OrderLine> lines = List.of(
                new OrderLine(p1, 1),
                new OrderLine(p2, 1),
                new OrderLine(p3, 1)
        );

        BigDecimal subtotal = new BigDecimal("60.00");

        BigDecimal result = discountPolicy.applyDiscount(subtotal, lines);

        assertEquals(new BigDecimal("60.00"), result);
    }

    @Test
    void givenMoreThan3ProductTypes_shouldApplyTenPercentDiscount() {
        Product p1 = new Product("Manzana", BigDecimal.valueOf(10.00), 10); p1.setId(1L);
        Product p2 = new Product("Pera",    BigDecimal.valueOf(10.00), 10); p2.setId(2L);
        Product p3 = new Product("Uva",     BigDecimal.valueOf(10.00), 10); p3.setId(3L);
        Product p4 = new Product("Sandía",  BigDecimal.valueOf(10.00), 10); p4.setId(4L);

        List<OrderLine> lines = List.of(
                new OrderLine(p1, 1),
                new OrderLine(p2, 1),
                new OrderLine(p3, 1),
                new OrderLine(p4, 1)
        );

        BigDecimal subtotal = new BigDecimal("40.00");

        BigDecimal result = discountPolicy.applyDiscount(subtotal, lines);

        assertEquals(0, new BigDecimal("36.00").compareTo(result));
    }

    @Test
    void givenManyUnitsOfSameProduct_shouldCountAsSingleTypeAndNotApplyDiscount() {
        Product p1 = new Product("Manzana", BigDecimal.valueOf(10.00), 100); p1.setId(1L);

        List<OrderLine> lines = List.of(
                new OrderLine(p1, 10)
        );

        BigDecimal subtotal = new BigDecimal("100.00");

        BigDecimal result = discountPolicy.applyDiscount(subtotal, lines);

        assertEquals(new BigDecimal("100.00"), result);
    }
}
