package com.steadybit.demo.shopping.checkout;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface CartRepository extends JpaRepository<Cart, String> {
    @Modifying
    @Query("Update Cart set order_published = :now where id in (:id)")
    void markAsPublished(Collection<String> id, Instant now);

    @Query("Select c from Cart c where order_published is null")
    Page<Cart> findPublishPending(Pageable page);
}
