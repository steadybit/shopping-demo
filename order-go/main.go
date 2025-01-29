/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package main

import (
	"encoding/json"
	stomp "github.com/go-stomp/stomp/v3"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/rs/zerolog/log"
	"github.com/steadybit/extension-kit/extlogging"
	"math/big"
	"net/http"
	"os"
	"time"
)

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	_, err := w.Write([]byte("OK"))
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

func readinessHandler(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	_, err := w.Write([]byte("READY"))
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

type OrderItem struct {
	ProductID string     `json:"productId"`
	Quantity  int        `json:"quantity"`
	Price     *big.Float `json:"price"`
}
type Order struct {
	ID        string      `json:"id"`
	Submitted time.Time   `json:"submitted"`
	Items     []OrderItem `json:"items"`
}

func main() {
	log.Info().Msg("Starting Order Application...")
	extlogging.InitZeroLog()

	// Retrieve ActiveMQ connection info from environment variables (or use defaults).
	brokerAddr, found := os.LookupEnv("SPRING_ACTIVEMQ_BROKER_URL")
	if !found {
		brokerAddr, found = os.LookupEnv("spring.activemq.broker-url")
	}
	if !found {
		brokerAddr = "localhost:61613"
	}
	user := os.Getenv("ACTIVEMQ_USER")
	pass := os.Getenv("ACTIVEMQ_PASS")

	// 1) Connect to ActiveMQ using STOMP.
	var conn *stomp.Conn
	var err error
	if user == "" {
		conn, err = stomp.Dial("tcp", brokerAddr, stomp.ConnOpt.HeartBeat(5*time.Second, 5*time.Second))
	} else {
		conn, err = stomp.Dial("tcp", brokerAddr, stomp.ConnOpt.Login(user, pass), stomp.ConnOpt.HeartBeat(5*time.Second, 5*time.Second))
	}
	if err != nil {
		log.Error().Err(err).Msg("Failed to connect to ActiveMQ")
	}
	defer func() {
		if conn != nil {
			_ = conn.Disconnect()
		}
	}()

	// 2) Subscribe to the "order_created" queue. (Equivalent of @JmsListener in Java.)
	if conn == nil {
		log.Error().Msg("Connection to ActiveMQ is nil")
		return
	}
	sub, err := conn.Subscribe("order_created", stomp.AckAuto)
	if err != nil {
		log.Error().Err(err).Msg("Failed to subscribe to queue")
	}

	// 3) Consume messages in a separate goroutine.
	//    Unmarshal into an Order object and log it.
	go func() {
		for {
			msg := <-sub.C
			if msg.Err != nil {
				log.Printf("Error while reading message: %v", msg.Err)
				continue
			}

			var order Order
			if err := json.Unmarshal(msg.Body, &order); err != nil {
				log.Printf("Failed to parse message body as Order: %v", err)
				continue
			}

			log.Printf("Order created: %+v", order)
		}
	}()

	http.Handle("/metrics", promhttp.Handler())

	// Setup HTTP server
	http.HandleFunc("/actuator/health/liveness", healthHandler)     // Liveness Probe
	http.HandleFunc("/actuator/health/readiness", readinessHandler) // Readiness Probe
	port := "8086"
	println("Server running on port:", port)
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		panic(err)
	}
}
