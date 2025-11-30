package com.pruebatecnica.pruebatecnica.service.order;

import com.pruebatecnica.pruebatecnica.dto.CreateOrderRequest;
import com.pruebatecnica.pruebatecnica.model.*;
import com.pruebatecnica.pruebatecnica.repository.OrderRepository;
import com.pruebatecnica.pruebatecnica.service.order.discount.DiscountPolicy;
import com.pruebatecnica.pruebatecnica.service.order.pricing.PriceCalculator;
import com.pruebatecnica.pruebatecnica.service.order.validation.OrderRequestValidator;
import com.pruebatecnica.pruebatecnica.service.order.validation.StockValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderRequestValidator orderRequestValidator;

    @Autowired
    private OrderLinesFactory orderLinesFactory;

    @Autowired
    private StockValidator stockValidator;

    @Autowired
    private PriceCalculator priceCalculator;

    @Autowired
    private DiscountPolicy discountPolicy;

    @Autowired
    private InventoryUpdater inventoryUpdater;


    /**
     * Flujo de creación de orden:
     * 1. Validar request
     * 2. Crear orden
     * 3. Mapear request -> líneas (producto + cantidad)
     * 4. Validar stock
     * 5. Calcular precios
     * 6. Aplicar descuentos
     * 7. Actualizar inventario
     * 8. Guardar orden
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 1. Validación de datos de entrada
        orderRequestValidator.validate(request);

        // 2. Crear la orden vacía
        Order order = new Order(request.getCustomerName(), request.getCustomerEmail());

        // 3. Transformar la request en líneas de pedido (producto + cantidad)
        List<OrderLine> orderLines = orderLinesFactory.fromRequest(request);

        // 4. Validar stock para cada línea
        stockValidator.validate(orderLines);

        // 5. Calcular subtotal
        BigDecimal subtotal = priceCalculator.calculateSubtotal(orderLines);

        // 6. Aplicar descuentos sobre el subtotal
        BigDecimal total = discountPolicy.applyDiscount(subtotal, orderLines);

        // 7. Crear los OrderItem asociados a la orden
        List<OrderItem> orderItems = buildOrderItems(order, orderLines);

        // 8. Actualizar inventario en base de datos
        inventoryUpdater.updateStock(orderLines);

        // 9. Guardar la orden
        order.setItems(orderItems);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.CONFIRMED);

        return orderRepository.save(order);
    }

    private List<OrderItem> buildOrderItems(Order order, List<OrderLine> orderLines) {
        return orderLines.stream()
                .map(line -> {
                    OrderItem item = new OrderItem(line.getProduct(), line.getQuantity());
                    item.setOrder(order);
                    return item;
                })
                .toList();
    }
    
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
    
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}