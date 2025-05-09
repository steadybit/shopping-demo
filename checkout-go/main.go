/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package main

import (
	"checkout/cart"
	"checkout/chaos"
	"checkout/controller"
	stomp_connection "checkout/stomp_wrapper"
	stomp "github.com/go-stomp/stomp/v3"
	"github.com/gorilla/mux"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/rs/zerolog/log"
	"github.com/steadybit/extension-kit/extlogging"
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

func main() {
	log.Info().Msg("Starting Checkout Application...")
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

	http.Handle("/metrics", promhttp.Handler())

	repository := cart.NewCartRepository()
	stompWrapper := stomp_connection.NewStompConnWrapper(conn)
	controller := controller.NewCheckoutRestController(stompWrapper, repository)

	r := mux.NewRouter()
	r.HandleFunc("/checkout/direct", controller.CheckoutDirect).Methods("POST")
	r.HandleFunc("/checkout/buffered", controller.CheckoutAsync).Methods("POST")

	go func() {
		for {
			controller.PublishPendingOrders()
			time.Sleep(1 * time.Second)
		}
	}()

	chaosController := chaos.NewChaosRestController(stompWrapper)
	r.HandleFunc("/checkout/chaos/flood", chaosController.Flood).Methods("POST")

	// Setup HTTP server
	r.HandleFunc("/actuator/health/liveness", healthHandler)     // Liveness Probe
	r.HandleFunc("/actuator/health/readiness", readinessHandler) // Readiness Probe
	port := "8085"
	println("Server running on port:", port)
	if err := http.ListenAndServe(":"+port, r); err != nil {
		panic(err)
	}
}
