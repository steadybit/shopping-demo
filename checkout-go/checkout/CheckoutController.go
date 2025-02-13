/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package checkout

import (
	"checkout/cart"
	"encoding/json"
	"log"
	"net/http"
	"time"

	"github.com/go-stomp/stomp/v3"
)

type CheckoutRestController struct {
	stompConn  *stomp.Conn
	repository *cart.CartRepository
}

func NewCheckoutRestController(stompConn *stomp.Conn, repository *cart.CartRepository) *CheckoutRestController {
	return &CheckoutRestController{stompConn: stompConn, repository: repository}
}

func (c *CheckoutRestController) CheckoutDirect(w http.ResponseWriter, r *http.Request) {
	var cart cart.ShoppingCart
	if err := json.NewDecoder(r.Body).Decode(&cart); err != nil {
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}

	destination := "/queue/order_created"
	body := toOrder(toCart(cart))
	if err := c.stompConn.Send(destination, "application/json", body); err != nil {
		http.Error(w, "Failed to publish order", http.StatusInternalServerError)
		log.Printf("Failed to send order %s: %v", cart.Id, err)
		return
	}
	log.Printf("Published direct order %s", cart.Id)
}

func (c *CheckoutRestController) CheckoutAsync(w http.ResponseWriter, r *http.Request) {
	var cart cart.ShoppingCart
	if err := json.NewDecoder(r.Body).Decode(&cart); err != nil {
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}

	c.repository.Save(toCart(cart))
	log.Printf("Buffered order: %s", cart.Id)
}

func toCart(shoppingCart cart.ShoppingCart) cart.Cart {
	return cart.Cart{
		ID:        shoppingCart.Id,
		Submitted: time.Now(),
		Items:     toItems(shoppingCart.Items),
	}
}

func toItems(items []cart.OrderItem) []cart.Item {
	var result []cart.Item
	for _, item := range items {
		result = append(result, cart.Item{
			ID:       item.ProductID,
			Quantity: item.Quantity,
			Price:    item.Price,
		})
	}
	return result
}

func (c *CheckoutRestController) PublishPendingOrders() {
	destination := "/queue/order_created"
	for {
		publishPending, _ := c.repository.FindPublishPending(5, 5)
		var published []string
		for _, cart := range publishPending {
			if err := c.stompConn.Send(destination, "application/json", toOrder(cart)); err != nil {
				log.Printf("Failed to publish buffered order %s: %v", cart.ID, err)
				continue
			}
			published = append(published, cart.ID)
			log.Printf("Published buffered order %s", cart.ID)
		}
		c.repository.MarkAsPublished(published, time.Now())
		if len(publishPending) < 5 {
			return
		}
	}
}

func toOrder(c cart.Cart) []byte {
	order := cart.Order{
		ID:        c.ID,
		Submitted: c.Submitted,
		Items:     toOrderItems(c.Items),
	}
	orderJson, _ := json.Marshal(order)
	return orderJson
}

func toOrderItems(items []cart.Item) []cart.OrderItem {
	var result []cart.OrderItem
	for _, item := range items {
		result = append(result, cart.OrderItem{
			ProductID: item.ID,
			Quantity:  item.Quantity,
			Price:     item.Price,
		})
	}
	return result
}
