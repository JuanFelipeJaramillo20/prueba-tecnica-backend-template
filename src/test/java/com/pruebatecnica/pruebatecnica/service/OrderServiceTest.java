package com.pruebatecnica.pruebatecnica.service;

import com.pruebatecnica.pruebatecnica.dto.CreateOrderRequest;
import com.pruebatecnica.pruebatecnica.dto.OrderItemRequest;
import com.pruebatecnica.pruebatecnica.model.*;
import com.pruebatecnica.pruebatecnica.repository.OrderRepository;
import com.pruebatecnica.pruebatecnica.repository.ProductRepository;
import com.pruebatecnica.pruebatecnica.service.order.InventoryUpdater;
import com.pruebatecnica.pruebatecnica.service.order.OrderLinesFactory;
import com.pruebatecnica.pruebatecnica.service.order.OrderService;
import com.pruebatecnica.pruebatecnica.service.order.discount.DiscountPolicy;
import com.pruebatecnica.pruebatecnica.service.order.discount.VarietyDiscountPolicy;
import com.pruebatecnica.pruebatecnica.service.order.pricing.PriceCalculator;
import com.pruebatecnica.pruebatecnica.service.order.validation.OrderRequestValidator;
import com.pruebatecnica.pruebatecnica.service.order.validation.StockValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRequestValidator orderRequestValidator;

    @Mock
    private OrderLinesFactory orderLinesFactory;

    @Mock
    private StockValidator stockValidator;

    @Mock
    private PriceCalculator priceCalculator;

    @Mock
    private InventoryUpdater inventoryUpdater;

    private final DiscountPolicy discountPolicy = new VarietyDiscountPolicy();

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "discountPolicy", discountPolicy);
    }

    @Test
    void testCreateOrderWithoutDiscount_ShouldNotApplyVarietyDiscount() {
        Product p1 = new Product("Product 1", BigDecimal.valueOf(10.00), 10); p1.setId(1L);
        Product p2 = new Product("Product 2", BigDecimal.valueOf(20.00), 10); p2.setId(2L);
        Product p3 = new Product("Product 3", BigDecimal.valueOf(30.00), 10); p3.setId(3L);

        List<OrderLine> orderLines = List.of(
                new OrderLine(p1, 1),
                new OrderLine(p2, 1),
                new OrderLine(p3, 1)
        );

        OrderItemRequest item1 = new OrderItemRequest(1L, 1);
        OrderItemRequest item2 = new OrderItemRequest(2L, 1);
        OrderItemRequest item3 = new OrderItemRequest(3L, 1);

        CreateOrderRequest request = new CreateOrderRequest(
                "John Doe",
                "john@test.com",
                List.of(item1, item2, item3)
        );

        when(orderLinesFactory.fromRequest(request)).thenReturn(orderLines);
        when(priceCalculator.calculateSubtotal(orderLines)).thenReturn(new BigDecimal("60.00"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal expectedTotal = new BigDecimal("60.00");

        Order result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(expectedTotal, result.getTotalAmount());
    }

    @Test
    void testCreateOrderWithDiscount_ShouldApplyVarietyDiscount() {
        Product p1 = new Product("Product 1", BigDecimal.valueOf(10.00), 10); p1.setId(1L);
        Product p2 = new Product("Product 2", BigDecimal.valueOf(10.00), 10); p2.setId(2L);
        Product p3 = new Product("Product 3", BigDecimal.valueOf(10.00), 10); p3.setId(3L);
        Product p4 = new Product("Product 4", BigDecimal.valueOf(10.00), 10); p4.setId(4L);

        List<OrderLine> orderLines = List.of(
                new OrderLine(p1, 1),
                new OrderLine(p2, 1),
                new OrderLine(p3, 1),
                new OrderLine(p4, 1)
        );

        OrderItemRequest item1 = new OrderItemRequest(1L, 1);
        OrderItemRequest item2 = new OrderItemRequest(2L, 1);
        OrderItemRequest item3 = new OrderItemRequest(3L, 1);
        OrderItemRequest item4 = new OrderItemRequest(4L, 1);

        CreateOrderRequest request = new CreateOrderRequest(
                "John Doe",
                "john@test.com",
                List.of(item1, item2, item3, item4)
        );

        when(orderLinesFactory.fromRequest(request)).thenReturn(orderLines);
        when(priceCalculator.calculateSubtotal(orderLines)).thenReturn(new BigDecimal("40.00"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal expectedTotal = new BigDecimal("36.00");

        Order result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(0, expectedTotal.compareTo(result.getTotalAmount()));
    }

    @Test
    void testCreateOrderWithSameProductMultipleTimes_ShouldNotApplyDiscount() {
        Product p1 = new Product("Apples", BigDecimal.valueOf(10.00), 100); p1.setId(1L);

        List<OrderLine> orderLines = List.of(
                new OrderLine(p1, 10)
        );

        OrderItemRequest item = new OrderItemRequest(1L, 10);

        CreateOrderRequest request = new CreateOrderRequest(
                "John Doe",
                "john@test.com",
                List.of(item)
        );

        when(orderLinesFactory.fromRequest(request)).thenReturn(orderLines);
        when(priceCalculator.calculateSubtotal(orderLines)).thenReturn(new BigDecimal("100.00"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal expectedTotal = new BigDecimal("100.00");

        Order result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(expectedTotal, result.getTotalAmount());
    }

    @Test
    void testCreateBasicOrder() {
        Product p1 = new Product("Test Product", BigDecimal.valueOf(10.00), 5); p1.setId(1L);

        List<OrderLine> orderLines = List.of(
                new OrderLine(p1, 2)
        );

        OrderItemRequest item = new OrderItemRequest(1L, 2);
        CreateOrderRequest request = new CreateOrderRequest(
                "John Doe",
                "john@test.com",
                List.of(item)
        );

        when(orderLinesFactory.fromRequest(request)).thenReturn(orderLines);
        when(priceCalculator.calculateSubtotal(orderLines)).thenReturn(new BigDecimal("20.00"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> {
            Order result = orderService.createOrder(request);
            assertNotNull(result);
            assertEquals("John Doe", result.getCustomerName());
            assertEquals(new BigDecimal("20.00"), result.getTotalAmount());
        });
    }
}
