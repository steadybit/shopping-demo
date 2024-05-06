package com.steadybit.demo.shopping.checkout;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(indexes = { @Index(columnList = "orderPublished") })
@Getter
@Setter
public class Cart {
    @Id
    private String id;

    @Version
    private Long version;

    private Instant orderPublished;
    private Instant submitted;

    @ElementCollection
    private List<Item> items;

    @Embeddable
    @Getter
    @Setter
    public static class Item {
        private String id;
        private int quantity;
        private BigDecimal price;

        public Item() {
        }

        public Item(String id, int quantity, BigDecimal price) {
            this.id = id;
            this.quantity = quantity;
            this.price = price;
        }
    }
}