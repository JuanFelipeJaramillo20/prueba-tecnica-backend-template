package com.pruebatecnica.pruebatecnica.service.order;

import com.pruebatecnica.pruebatecnica.model.OrderLine;
import com.pruebatecnica.pruebatecnica.model.Product;
import com.pruebatecnica.pruebatecnica.repository.ProductRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class InventoryUpdater {

    private final ProductRepository productRepository;

    public InventoryUpdater(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Actualiza el stock de cada producto según las líneas de pedido.
     */
    @Transactional
    public void updateStock(List<OrderLine> orderLines) {
        for (OrderLine line : orderLines) {
            Product product = line.getProduct();
            product.setStock(product.getStock() - line.getQuantity());
            productRepository.save(product);
        }
    }
}

