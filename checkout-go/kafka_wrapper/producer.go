/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package kafka_wrapper

import (
	"fmt"
	"strings"

	"github.com/IBM/sarama"
	"github.com/rs/zerolog/log"
)

type Producer struct {
	producer sarama.SyncProducer
	topic    string
}

func NewKafkaProducer(brokers string, topic string) (*Producer, error) {
	brokerList := strings.Split(brokers, ",")

	config := sarama.NewConfig()
	config.Producer.Return.Successes = true
	config.Producer.RequiredAcks = sarama.WaitForAll

	producer, err := sarama.NewSyncProducer(brokerList, config)
	if err != nil {
		return nil, fmt.Errorf("failed to create kafka producer: %w", err)
	}

	return &Producer{producer: producer, topic: topic}, nil
}

// Send publishes a message to Kafka. The destination parameter is ignored (topic is fixed).
func (p *Producer) Send(destination, contentType string, body []byte, ID string) error {
	msg := &sarama.ProducerMessage{
		Topic: p.topic,
		Value: sarama.ByteEncoder(body),
	}
	if ID != "" {
		msg.Key = sarama.StringEncoder(ID)
	}

	partition, offset, err := p.producer.SendMessage(msg)
	if err != nil {
		return fmt.Errorf("failed to publish %s to kafka: %w", ID, err)
	}

	log.Debug().
		Str("id", ID).
		Int32("partition", partition).
		Int64("offset", offset).
		Msg("Published message to Kafka")
	return nil
}

func (p *Producer) Close() error {
	return p.producer.Close()
}
