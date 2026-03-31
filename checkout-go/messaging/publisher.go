/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package messaging

// Publisher abstracts message publishing so that both STOMP (ActiveMQ) and Kafka
// can be used interchangeably at runtime.
type Publisher interface {
	Send(destination, contentType string, body []byte, ID string) error
	Close() error
}
