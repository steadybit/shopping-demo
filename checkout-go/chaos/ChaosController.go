/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package chaos

import (
	stomp "github.com/go-stomp/stomp/v3"
	"log"
	"math/rand"
	"net/http"
	"time"
)

type ChaosRestController struct {
	conn      *stomp.Conn
	scheduler *time.Ticker
}

func NewChaosRestController(conn *stomp.Conn) *ChaosRestController {
	return &ChaosRestController{
		conn:      conn,
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
	log.Printf("Flooding queue %s", queueName)
	count := 0
	for {
		select {
		case <-c.scheduler.C:
			log.Printf("Stopped flooding %s after %d messages.", queueName, count)
			return
		default:
			message := randomString(1048567)
			err := c.conn.Send(queueName, "text/plain", []byte(message))
			if err != nil {
				log.Printf("Error sending message: %v", err)
				return
			}
			count++
			if count%100 == 0 {
				log.Printf("Flooding %s. %d messages with 1mb sent.", queueName, count)
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

