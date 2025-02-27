/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package controller

import (
	"checkout/cart"
	"checkout/stomp_wrapper"
	"encoding/json"
	"github.com/rs/zerolog/log"
	"net/http"
	"time"
)

type CheckoutRestController struct {
	stompWrapper *stomp_wrapper.ConnWrapper
	repository   *cart.CartRepository
}

func NewCheckoutRestController(stompWrapper *stomp_wrapper.ConnWrapper, repository *cart.CartRepository) *CheckoutRestController {
	return &CheckoutRestController{
		stompWrapper: stompWrapper,
		repository:   repository,
	}
}

func (c *CheckoutRestController) CheckoutDirect(w http.ResponseWriter, r *http.Request) {
	var theCart cart.ShoppingCart
	if err := json.NewDecoder(r.Body).Decode(&theCart); err != nil {
		log.Error().Err(err).Msg("Invalid request")
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}

	destination := "/queue/order_created"
	body := toOrder(toCart(theCart))
	if err := c.stompWrapper.Send(destination, "application/json", body, theCart.Id); err != nil {
		http.Error(w, "Failed to publish order", http.StatusInternalServerError)
		log.Error().Err(err).Msgf("Failed to publish direct order %s", theCart.Id)
		return
	}
	log.Info().Msgf("Published direct order %s", theCart.Id)
}

func (c *CheckoutRestController) CheckoutAsync(w http.ResponseWriter, r *http.Request) {
	var theCart cart.ShoppingCart
	if err := json.NewDecoder(r.Body).Decode(&theCart); err != nil {
		log.Error().Err(err).Msg("Invalid request")
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}

	c.repository.Save(toCart(theCart))
	log.Info().Msgf("Buffered order %s", theCart.Id)
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
		publishPending, _ := c.repository.FindPublishPending()
		if len(publishPending) > 0 {
			log.Info().Msgf("publishPending: %v", publishPending)
		}
		var published []string
		for _, theCart := range publishPending {
			order := toOrder(*theCart)
			if err := c.stompWrapper.Send(destination, "application/json", order, theCart.ID); err != nil {
				log.Error().Err(err).Msgf("Failed to publish buffered order %s", theCart.ID)
				continue
			}
			published = append(published, theCart.ID)
			log.Info().Msgf("Published buffered order %s", theCart.ID)
		}
		err := c.repository.MarkAsPublished(published, time.Now())
		if err != nil {
			log.Error().Err(err).Msg("Failed to mark orders as published")
			return
		}
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
