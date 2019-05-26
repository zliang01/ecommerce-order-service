package com.ecommerce.order.order.model;

import com.ecommerce.order.common.event.DomainEventAwareAggregate;
import com.ecommerce.order.common.model.Address;
import com.ecommerce.order.order.exception.OrderCannotBeModifiedException;
import com.ecommerce.order.order.exception.PaidPriceNotSameWithOrderPriceException;
import com.ecommerce.order.order.exception.ProductNotInOrderException;
import com.ecommerce.order.order.model.event.OrderAddressChangedEvent;
import com.ecommerce.order.order.model.event.OrderCreatedEvent;
import com.ecommerce.order.order.model.event.OrderPaidEvent;
import com.ecommerce.order.order.model.event.OrderProductChangedEvent;
import com.ecommerce.order.product.ProductId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.ecommerce.order.order.model.OrderStatus.CREATED;
import static com.ecommerce.order.order.model.OrderStatus.PAID;
import static com.google.common.collect.Lists.newArrayList;
import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;

public class Order extends DomainEventAwareAggregate {
    private OrderId id;
    private List<OrderItem> items = newArrayList();
    private BigDecimal totalPrice;
    private OrderStatus status;
    private Address address;
    private Instant createdAt;

    private Order() {
    }

    private Order(OrderId id, List<OrderItem> items, Address address) {
        this.id = id;
        this.items.addAll(items);
        this.totalPrice = calculateTotalPrice();
        this.status = CREATED;
        this.address = address;
        this.createdAt = now();
        raiseEvent(new OrderCreatedEvent(this));
    }

    public static Order create(OrderId id, List<OrderItem> items, Address address) {
        return new Order(id, items, address);
    }


    public void changeProductCount(ProductId productId, int count) {
        if (this.status == PAID) {
            throw new OrderCannotBeModifiedException(this.id);
        }

        OrderItem orderItem = retrieveItem(productId);
        orderItem.updateCount(count);
        this.totalPrice = calculateTotalPrice();
        raiseEvent(new OrderProductChangedEvent(this));
    }

    private BigDecimal calculateTotalPrice() {
        return items.stream()
                .map(OrderItem::totalPrice)
                .reduce(ZERO, BigDecimal::add);
    }


    private OrderItem retrieveItem(ProductId productId) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ProductNotInOrderException(productId, id));
    }

    public void pay(BigDecimal paidPrice) {
        if (!this.totalPrice.equals(paidPrice)) {
            throw new PaidPriceNotSameWithOrderPriceException(id);
        }
        this.status = PAID;
        raiseEvent(new OrderPaidEvent(this.getId()));
    }

    public void changeAddressDetail(String detail) {
        if (this.status == PAID) {
            throw new OrderCannotBeModifiedException(this.id);
        }

        this.address = this.address.changeDetailTo(detail);
        raiseEvent(new OrderAddressChangedEvent(getId(), detail, address.getDetail()));
    }

    public OrderId getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Address getAddress() {
        return address;
    }
}
