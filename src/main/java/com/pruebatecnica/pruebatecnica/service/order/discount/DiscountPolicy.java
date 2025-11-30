package com.pruebatecnica.pruebatecnica.service.order.discount;

import com.pruebatecnica.pruebatecnica.model.OrderLine;
import java.math.BigDecimal;
import java.util.List;

public interface DiscountPolicy {
    BigDecimal applyDiscount(BigDecimal subtotal, List<OrderLine> orderLines);
}

