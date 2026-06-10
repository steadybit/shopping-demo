/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package main

import (
	"context"
	"encoding/json"
	"os"
	"strings"
	"time"

	"github.com/IBM/sarama"
	"github.com/rs/zerolog/log"
)

type orderConsumerHandler struct{}

func (h *orderConsumerHandler) Setup(_ sarama.ConsumerGroupSession) error {
	consumerReady.Store(true)
	markProgress()
	return nil
}
func (h *orderConsumerHandler) Cleanup(_ sarama.ConsumerGroupSession) error {
	consumerReady.Store(false)
	return nil
}

func (h *orderConsumerHandler) ConsumeClaim(session sarama.ConsumerGroupSession, claim sarama.ConsumerGroupClaim) error {
	for msg := range claim.Messages() {
		markProgress()
		processOrderMessage(msg.Value)
		session.MarkMessage(msg, "")
	}
	return nil
}

func processOrderMessage(body []byte) {
	var order Order
	if err := json.Unmarshal(body, &order); err != nil {
		log.Error().Err(err).Msg("Failed to unmarshal message")
		return
	}
	log.Debug().Str("orderID", order.ID).Msg("Received order")

	if rabbitPublisher != nil {
		rabbitPublisher.Publish(body)
		log.Debug().Str("orderID", order.ID).Msg("Published order to RabbitMQ")
	}
}

func startKafkaConsumer(brokers string) {
	brokerList := strings.Split(brokers, ",")

	config := sarama.NewConfig()
	config.Consumer.Group.Rebalance.GroupStrategies = []sarama.BalanceStrategy{sarama.NewBalanceStrategyRoundRobin()}
	config.Consumer.Offsets.Initial = sarama.OffsetOldest

	configureSASL(config)

	handler := &orderConsumerHandler{}

	// Reconnect loop: a broker that is briefly unreachable must not kill the process.
	// We retry NewConsumerGroup with backoff instead of exiting fatally, and rebuild the
	// group if Consume returns. consumerReady (set by the handler) backs the readiness probe.
	go func() {
		const maxBackoff = 30 * time.Second
		backoff := time.Second
		for {
			markProgress()
			group, err := sarama.NewConsumerGroup(brokerList, "order-service", config)
			if err != nil {
				consumerReady.Store(false)
				log.Error().Err(err).Dur("retryIn", backoff).Msg("Kafka consumer connect failed, retrying")
				time.Sleep(backoff)
				backoff = nextBackoff(backoff, maxBackoff)
				continue
			}
			backoff = time.Second

			for {
				if err := group.Consume(context.Background(), []string{"order_created"}, handler); err != nil {
					log.Error().Err(err).Msg("Kafka consumer error")
					break
				}
				markProgress()
			}

			consumerReady.Store(false)
			_ = group.Close()
			time.Sleep(backoff)
			backoff = nextBackoff(backoff, maxBackoff)
		}
	}()
}

func configureSASL(config *sarama.Config) {
	user := os.Getenv("KAFKA_SASL_USERNAME")
	pass := os.Getenv("KAFKA_SASL_PASSWORD")
	if user == "" || pass == "" {
		return
	}
	mechanism := os.Getenv("KAFKA_SASL_MECHANISM")
	config.Net.SASL.Enable = true
	config.Net.SASL.User = user
	config.Net.SASL.Password = pass
	switch mechanism {
	case "SCRAM-SHA-512":
		config.Net.SASL.Mechanism = sarama.SASLTypeSCRAMSHA512
		config.Net.SASL.SCRAMClientGeneratorFunc = func() sarama.SCRAMClient { return &XDGSCRAMClient{HashGeneratorFcn: SHA512} }
	default:
		config.Net.SASL.Mechanism = sarama.SASLTypePlaintext
	}
	log.Info().Str("user", user).Str("mechanism", string(config.Net.SASL.Mechanism)).Msg("Kafka SASL enabled")
}
