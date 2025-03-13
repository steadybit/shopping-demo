/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package stomp_wrapper

import (
	"fmt"
	"github.com/go-stomp/stomp/v3"
	"os"
	"sync"
	"time"
)

type ConnWrapper struct {
	conn   *stomp.Conn
	closed bool
	mu     sync.RWMutex
}

func NewStompConnWrapper(conn *stomp.Conn) *ConnWrapper {
	return &ConnWrapper{
		conn: conn,
	}
}

// send sends a message and updates the state if the connection is closed.
func (w *ConnWrapper) send(destination, contentType string, body []byte) error {
	w.mu.RLock()
	if w.closed {
		w.mu.RUnlock()
		return fmt.Errorf("connection is closed")
	}
	w.mu.RUnlock()
	err := w.conn.Send(destination, contentType, body)
	if err != nil {
		// Optionally mark the connection as closed if the error indicates so.
		w.mu.Lock()
		w.closed = true
		w.mu.Unlock()
	}
	return err
}

// Close safely closes the connection.
func (w *ConnWrapper) Close() error {
	w.mu.Lock()
	defer w.mu.Unlock()
	w.closed = true
	return w.conn.Disconnect()
}

func (c *ConnWrapper) Send(destination string, contentType string, body []byte, ID string) error {
	const maxRetries = 3
	var err error

	for i := 0; i < maxRetries; i++ {
		// Check if the connection is marked as closed.
		c.mu.RLock()
		closed := c.closed
		c.mu.RUnlock()

		if closed {
			// Attempt to reconnect.
			var newConn *stomp.Conn
			newConn, err = reconnectStomp()
			if err != nil {
				return fmt.Errorf("failed to reconnect: %w", err)
			}
			c.mu.Lock()
			c.conn = newConn
			c.closed = false
			c.mu.Unlock()
		}

		// Try sending using the internal send method.
		err = c.send(destination, contentType, body)
		if err != nil {
			// If the error indicates the connection is closed, wait and retry.
			if err.Error() == "connection is closed" || err.Error() == "connection already closed" {
				time.Sleep(1 * time.Second)
				continue
			}
			// For any other error, return immediately.
			return fmt.Errorf("failed to publish %s: %w", ID, err)
		}

		// Success!
		return nil
	}

	return fmt.Errorf("failed to publish %s after %d retries: %w", ID, maxRetries, err)
}

func reconnectStomp() (*stomp.Conn, error) {
	brokerAddr, found := os.LookupEnv("SPRING_ACTIVEMQ_BROKER_URL")
	if !found {
		brokerAddr, found = os.LookupEnv("spring.activemq.broker-url")
	}
	if !found {
		brokerAddr = "localhost:61613"
	}
	user := os.Getenv("ACTIVEMQ_USER")
	pass := os.Getenv("ACTIVEMQ_PASS")

	if user == "" {
		return stomp.Dial("tcp", brokerAddr, stomp.ConnOpt.HeartBeat(5*time.Second, 5*time.Second))
	}
	return stomp.Dial("tcp", brokerAddr, stomp.ConnOpt.Login(user, pass), stomp.ConnOpt.HeartBeat(5*time.Second, 5*time.Second))
}
