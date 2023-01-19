package com.steadybit.demo.shopping.checkout;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Version;
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