/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package checkout

import (
	"encoding/json"
	"github.com/gorilla/mux"
	"github.com/streadway/amqp"
	"log"
	"net/http"
	"time"
)

type OrderItem struct {
	ProductID string  `json:"productId"`
	Quantity  int     `json:"quantity"`
	Price     float64 `json:"price"`
}

type ShoppingCart struct {
	Id    string      `json:"id"`
	Items []OrderItem `json:"items"`
}

type Cart struct {
	Id        string      `json:"id"`
	Submitted string   `json:"submitted"`
	Items     []OrderItem `json:"items"`
}

type Order struct {
	ID        string      `json:"id"`
	Submitted string      `json:"submitted"`
	Items     []OrderItem `json:"items"`
}

type CartRepository struct {
	// Implement repository methods
}

func (r *CartRepository) Save(cart Cart) {
	// Save cart to database
}

func (r *CartRepository) FindPublishPending(limit int) []Cart {
	// Find carts with orderPublished is null
	return []Cart{}
}

func (r *CartRepository) MarkAsPublished(ids []string, now time.Time) {
	// Update carts to mark as published
}

type CheckoutRestController struct {
	jms        *amqp.Channel
	repository *CartRepository
}

func NewCheckoutRestController(jms *amqp.Channel, repository *CartRepository) *CheckoutRestController {
	return &CheckoutRestController{jms: jms, repository: repository}
}

func (c *CheckoutRestController) CheckoutDirect(w http.ResponseWriter, r *http.Request) {
	var cart ShoppingCart
	json.NewDecoder(r.Body).Decode(&cart)
	c.jms.Publish("", "order_created", false, false, amqp.Publishing{
		ContentType: "application/json",
		Body:        toOrder(toCart(cart)),
	})
	log.Printf("Published direct order %s", cart.Id)
}

func (c *CheckoutRestController) CheckoutAsync(w http.ResponseWriter, r *http.Request) {
	var cart ShoppingCart
	json.NewDecoder(r.Body).Decode(&cart)
	c.repository.Save(toCart(cart))
	log.Printf("Buffered order: %s", cart.Id)
}

func (c *CheckoutRestController) PublishPendingOrders() {
	for {
		publishPending := c.repository.FindPublishPending(5)
		var published []string
		for _, cart := range publishPending {
			c.jms.Publish("", "order_created", false, false, amqp.Publishing{
				ContentType: "application/json",
				Body:        toOrder(cart),
			})
			published = append(published, cart.Id)
			log.Printf("Published buffered order %s", cart.Id)
		}
		c.repository.MarkAsPublished(published, time.Now())
		if len(publishPending) < 5 {
			return
		}
	}
}

func toCart(cart ShoppingCart) Cart {
	return Cart{
		Id:        cart.Id,
		Submitted: time.Now().Format(time.RFC3339),
		Items:     cart.Items,
	}
}

func toOrder(cart Cart) []byte {
	order := Order{
		ID:        cart.Id,
		Submitted: cart.Submitted,
		Items:     cart.Items,
	}
	orderJson, _ := json.Marshal(order)
	return orderJson
}

func main() {
	conn, _ := amqp.Dial("amqp://guest:guest@localhost:5672/")
	defer conn.Close()
	ch, _ := conn.Channel()
	defer ch.Close()

	repository := &CartRepository{}
	controller := NewCheckoutRestController(ch, repository)

	r := mux.NewRouter()
	r.HandleFunc("/checkout/direct", controller.CheckoutDirect).Methods("POST")
	r.HandleFunc("/checkout/buffered", controller.CheckoutAsync).Methods("POST")

	go func() {
		for {
			controller.PublishPendingOrders()
			time.Sleep(1 * time.Second)
		}
	}()

	log.Fatal(http.ListenAndServe(":8080", r))
}
