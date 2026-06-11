/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package main

import (
	"fmt"
	stomp "github.com/go-stomp/stomp/v3"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/rs/zerolog/log"
	"github.com/steadybit/extension-kit/extlogging"
	"net/http"
	"os"
	"sync/atomic"
	"time"
)

// consumerReady is true while the active consumer is connected to its broker and
// subscribed. It backs the readiness probe so a pod with a dead broker connection
// is taken out of the Service endpoints.
var consumerReady atomic.Bool

// lastProgressUnix holds the unix-nano time of the last consumer "progress" event:
// a (re)connect attempt or a consumed message. The liveness probe uses it to detect
// a wedged consumer goroutine (one making no attempts at all), while still tolerating
// a broker outage, where reconnect attempts keep progress fresh but consumerReady stays false.
var lastProgressUnix atomic.Int64

// livenessTimeout is how long the consumer may make no progress before the liveness
// probe fails and Kubernetes restarts the pod.
const livenessTimeout = 60 * time.Second

func markProgress() { lastProgressUnix.Store(time.Now().UnixNano()) }

// nextBackoff doubles cur up to max, for reconnect loops.
func nextBackoff(cur, max time.Duration) time.Duration {
	cur *= 2
	if cur > max {
		return max
	}
	return cur
}

// healthHandler backs the liveness probe. A currently-connected consumer is alive by
// definition — the broker's own session/heartbeat keeps it honest, and an idle topic
// must not look "wedged". The progress-timeout check only applies while disconnected,
// to catch a reconnect loop that has truly stopped making attempts.
func healthHandler(w http.ResponseWriter, r *http.Request) {
	if !consumerReady.Load() {
		last := lastProgressUnix.Load()
		if last != 0 && time.Since(time.Unix(0, last)) > livenessTimeout {
			http.Error(w, "consumer wedged: no progress", http.StatusServiceUnavailable)
			return
		}
	}
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("OK"))
}

// readinessHandler backs the readiness probe. It is ready only while the consumer is
// connected to its broker and subscribed, so a disconnected pod is removed from endpoints.
func readinessHandler(w http.ResponseWriter, r *http.Request) {
	if !consumerReady.Load() {
		http.Error(w, "consumer not connected", http.StatusServiceUnavailable)
		return
	}
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("READY"))
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

var rabbitPublisher *RabbitMQPublisher

func main() {
	log.Info().Msg("Starting Order Application...")
	extlogging.InitZeroLog()

	if rabbitmqURL, ok := os.LookupEnv("RABBITMQ_URL"); ok {
		var err error
		rabbitPublisher, err = NewRabbitMQPublisher(rabbitmqURL)
		if err != nil {
			log.Fatal().Err(err).Msg("Failed to connect to RabbitMQ")
		}
		defer rabbitPublisher.Close()
		log.Info().Msg("RabbitMQ publisher initialized")
	}

	if kafkaBrokers, ok := os.LookupEnv("KAFKA_BROKERS"); ok {
		// Use Kafka consumer
		log.Info().Msg("Using Kafka consumer")
		startKafkaConsumer(kafkaBrokers)
	} else {
		// Use ActiveMQ/STOMP consumer
		log.Info().Msg("Using ActiveMQ/STOMP consumer")
		startStompConsumer()
	}

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

func startStompConsumer() {
	// Retrieve ActiveMQ connection info from environment variables (or use defaults).
	brokerAddr, found := os.LookupEnv("ACTIVEMQ_BROKER_HOST")
	if !found {
		brokerAddr, found = os.LookupEnv("activemq.broker.host")
	}
	if !found {
		brokerAddr = "localhost:61613"
	}
	user := os.Getenv("ACTIVEMQ_USER")
	pass := os.Getenv("ACTIVEMQ_PASS")

	// Reconnect loop: a dropped connection ("connection closed") must not silently kill
	// the consumer. runStompSession blocks until the connection ends, then we back off and
	// reconnect. consumerReady reflects the live connection for the readiness probe.
	go func() {
		const maxBackoff = 30 * time.Second
		backoff := time.Second
		for {
			markProgress()
			if err := runStompSession(brokerAddr, user, pass); err != nil {
				consumerReady.Store(false)
				log.Error().Err(err).Dur("retryIn", backoff).Msg("ActiveMQ/STOMP consumer disconnected, reconnecting")
				time.Sleep(backoff)
				backoff = nextBackoff(backoff, maxBackoff)
				continue
			}
			backoff = time.Second
		}
	}()
}

// runStompSession connects to ActiveMQ, subscribes, and consumes until the connection
// drops. It returns an error whenever the session ends so the caller can reconnect.
func runStompSession(brokerAddr, user, pass string) error {
	opts := []func(*stomp.Conn) error{stomp.ConnOpt.HeartBeat(5*time.Second, 5*time.Second)}
	if user != "" {
		opts = append(opts, stomp.ConnOpt.Login(user, pass))
	}

	conn, err := stomp.Dial("tcp", brokerAddr, opts...)
	if err != nil {
		return fmt.Errorf("connect to ActiveMQ: %w", err)
	}
	defer conn.Disconnect()

	sub, err := conn.Subscribe("order_created", stomp.AckAuto)
	if err != nil {
		return fmt.Errorf("subscribe to order_created: %w", err)
	}

	consumerReady.Store(true)
	defer consumerReady.Store(false)
	log.Info().Str("broker", brokerAddr).Msg("ActiveMQ/STOMP consumer connected")

	// Ranging over sub.C exits when the connection closes (the channel is closed),
	// which ends the session and triggers a reconnect in the caller.
	for msg := range sub.C {
		if msg == nil {
			continue
		}
		if msg.Err != nil {
			return fmt.Errorf("receive message: %w", msg.Err)
		}
		if msg.Body == nil {
			continue
		}
		markProgress()
		processOrderMessage(msg.Body)
	}
	return fmt.Errorf("subscription closed")
}
