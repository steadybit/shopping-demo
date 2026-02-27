/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package chaos

import (
	"checkout/messaging"
	"github.com/rs/zerolog/log"
	"math/rand"
	"net/http"
	"time"
)

type ChaosRestController struct {
	publisher messaging.Publisher
	scheduler *time.Ticker
}

func NewChaosRestController(publisher messaging.Publisher) *ChaosRestController {
	return &ChaosRestController{
		publisher: publisher,
		scheduler: time.NewTicker(30 * time.Second),
	}
}

func (c *ChaosRestController) Flood(w http.ResponseWriter, r *http.Request) {
	queueName := r.URL.Query().Get("queueName")
	if queueName == "" {
		queueName = "junk"
	}
	go c.floodQueue(queueName)
	w.WriteHeader(http.StatusOK)
}

func (c *ChaosRestController) floodQueue(queueName string) {
	log.Info().Msgf("Starting flood of %s", queueName)
	count := 0
	for {
		select {
		case <-c.scheduler.C:
			log.Info().Msgf("Stopping flood of %s. %d messages with 1mb sent.", queueName, count)
			return
		default:
			message := randomString(1048567)
			err := c.publisher.Send(queueName, "text/plain", []byte(message), "")
			if err != nil {
				log.Error().Err(err).Msgf("Failed to send message to %s", queueName)
				return
			}
			count++
			if count%100 == 0 {
				log.Info().Msgf("Sent %d messages to %s", count, queueName)
			}
		}
	}
}

func randomString(n int) string {
	const letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
	b := make([]byte, n)
	for i := range b {
		b[i] = letters[rand.Intn(len(letters))]
	}
	return string(b)
}
