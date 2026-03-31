/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package main

import (
	"context"
	"encoding/json"
	"os"
	"strings"

	"github.com/IBM/sarama"
	"github.com/rs/zerolog/log"
)

type orderConsumerHandler struct{}

func (h *orderConsumerHandler) Setup(_ sarama.ConsumerGroupSession) error   { return nil }
func (h *orderConsumerHandler) Cleanup(_ sarama.ConsumerGroupSession) error { return nil }

func (h *orderConsumerHandler) ConsumeClaim(session sarama.ConsumerGroupSession, claim sarama.ConsumerGroupClaim) error {
	for msg := range claim.Messages() {
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
	log.Info().Str("orderID", order.ID).Msg("Received order")

	if rabbitPublisher != nil {
		rabbitPublisher.Publish(body)
		log.Info().Str("orderID", order.ID).Msg("Published order to RabbitMQ")
	}
}

func startKafkaConsumer(brokers string) error {
	brokerList := strings.Split(brokers, ",")

	config := sarama.NewConfig()
	config.Consumer.Group.Rebalance.GroupStrategies = []sarama.BalanceStrategy{sarama.NewBalanceStrategyRoundRobin()}
	config.Consumer.Offsets.Initial = sarama.OffsetOldest

	configureSASL(config)

	group, err := sarama.NewConsumerGroup(brokerList, "order-service", config)
	if err != nil {
		return err
	}

	handler := &orderConsumerHandler{}

	go func() {
		for {
			if err := group.Consume(context.Background(), []string{"order_created"}, handler); err != nil {
				log.Error().Err(err).Msg("Kafka consumer error")
			}
		}
	}()

	return nil
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
