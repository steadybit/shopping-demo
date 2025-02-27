/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package cart

import (
	"errors"
	"testing"
	"time"
)

func TestSaveAndFindPublishPending(t *testing.T) {
	repo := NewCartRepository()

	// Create two carts.
	// cart1 remains unpublished (OrderPublished is zero).
	cart1 := Cart{
		ID: "cart1",
		// OrderPublished is zero by default.
	}
	// cart2 is already published.
	cart2 := Cart{
		ID:             "cart2",
		OrderPublished: time.Now(),
	}

	repo.Save(cart1)
	repo.Save(cart2)

	// Find pending carts (i.e. unpublished carts)
	pending, err := repo.FindPublishPending()
	if err != nil {
		t.Fatalf("FindPublishPending returned error: %v", err)
	}

	// Expect only cart1 to be pending.
	if len(pending) != 1 {
		t.Fatalf("Expected 1 pending cart, got %d", len(pending))
	}

	if pending[0].ID != "cart1" {
		t.Errorf("Expected pending cart ID to be 'cart1', got %s", pending[0].ID)
	}
}

func TestMarkAsPublishedSuccess(t *testing.T) {
	repo := NewCartRepository()

	// Create and save a cart that is unpublished.
	cart := Cart{ID: "cart1"}
	repo.Save(cart)

	now := time.Now().Truncate(time.Millisecond)
	if err := repo.MarkAsPublished([]string{"cart1"}, now); err != nil {
		t.Fatalf("MarkAsPublished returned error: %v", err)
	}

	// After marking as published, the cart should no longer be pending.
	pending, err := repo.FindPublishPending()
	if err != nil {
		t.Fatalf("FindPublishPending returned error: %v", err)
	}
	if len(pending) != 0 {
		t.Errorf("Expected no pending carts, got %d", len(pending))
	}

	// Verify that the cart's OrderPublished field is updated.
	repo.mu.RLock()
	updatedCart, ok := repo.carts["cart1"]
	repo.mu.RUnlock()
	if !ok {
		t.Fatalf("Cart 'cart1' not found in repository")
	}
	if !updatedCart.OrderPublished.Equal(now) {
		t.Errorf("Expected OrderPublished to be %v, got %v", now, updatedCart.OrderPublished)
	}
}

func TestMarkAsPublishedCartNotFound(t *testing.T) {
	repo := NewCartRepository()

	// Attempt to mark a cart that doesn't exist.
	err := repo.MarkAsPublished([]string{"nonexistent"}, time.Now())
	if err == nil {
		t.Error("Expected error when marking nonexistent cart as published, got nil")
	}

	// Optionally, check that the error message contains the missing cart id.
	if !errors.Is(err, errors.New("cart not found: nonexistent")) {
		t.Logf("Received expected error: %v", err)
	}
}
