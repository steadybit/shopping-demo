/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package main

import (
	"encoding/json"
	"net/http"
	"os"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	amqp "github.com/rabbitmq/amqp091-go"
	"github.com/rs/zerolog/log"
	"github.com/steadybit/extension-kit/extlogging"
)

var notificationsSent = prometheus.NewCounter(prometheus.CounterOpts{
	Name: "notifications_sent_total",
	Help: "Total number of notifications sent",
})

func init() {
	prometheus.MustRegister(notificationsSent)
}

type OrderItem struct {
	ProductID string  `json:"productId"`
	Quantity  int     `json:"quantity"`
	Price     float64 `json:"price"`
}

type Order struct {
	ID        string      `json:"id"`
	Submitted string      `json:"submitted"`
	Items     []OrderItem `json:"items"`
}

func healthHandler(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("OK"))
}

func readinessHandler(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("READY"))
}

func main() {
	log.Info().Msg("Starting Notification Application...")
	extlogging.InitZeroLog()

	rabbitmqURL, ok := os.LookupEnv("RABBITMQ_URL")
	if !ok {
		log.Fatal().Msg("RABBITMQ_URL environment variable is required")
	}

	var conn *amqp.Connection
	var err error
	for i := 0; i < 30; i++ {
		conn, err = amqp.Dial(rabbitmqURL)
		if err == nil {
			break
		}
		log.Warn().Err(err).Int("attempt", i+1).Msg("Failed to connect to RabbitMQ, retrying...")
		time.Sleep(2 * time.Second)
	}
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to connect to RabbitMQ after retries")
	}
	defer conn.Close()

	ch, err := conn.Channel()
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to open channel")
	}
	defer ch.Close()

	err = ch.ExchangeDeclare(
		"order.events",
		"fanout",
		true,  // durable
		false, // auto-deleted
		false, // internal
		false, // no-wait
		nil,
	)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to declare exchange")
	}

	q, err := ch.QueueDeclare(
		"notification.orders", // named queue
		true,                  // durable
		false,                 // delete when unused
		false,                 // exclusive
		false,                 // no-wait
		nil,
	)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to declare queue")
	}

	err = ch.QueueBind(q.Name, "", "order.events", false, nil)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to bind queue")
	}

	msgs, err := ch.Consume(q.Name, "", true, false, false, false, nil)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to start consuming")
	}

	go func() {
		for msg := range msgs {
			var order Order
			if err := json.Unmarshal(msg.Body, &order); err != nil {
				log.Error().Err(err).Msg("Failed to unmarshal order")
				continue
			}
			log.Info().Str("orderID", order.ID).Msg("Notification sent for order")
			notificationsSent.Inc()
		}
	}()

	log.Info().Msg("Waiting for order events...")

	http.Handle("/metrics", promhttp.Handler())
	http.HandleFunc("/actuator/health/liveness", healthHandler)
	http.HandleFunc("/actuator/health/readiness", readinessHandler)

	port := "8087"
	log.Info().Str("port", port).Msg("Server running")
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		log.Fatal().Err(err).Msg("Server failed")
	}
}
