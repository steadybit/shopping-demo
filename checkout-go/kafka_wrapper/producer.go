/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package kafka_wrapper

import (
	"fmt"
	"os"
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

	configureSASL(config)

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

func configureSASL(config *sarama.Config) {
	user := os.Getenv("KAFKA_SASL_USERNAME")
	pass := os.Getenv("KAFKA_SASL_PASSWORD")
	if user == "" || pass == "" {
		return
	}
	config.Net.SASL.Enable = true
	config.Net.SASL.User = user
	config.Net.SASL.Password = pass
	config.Net.SASL.Mechanism = sarama.SASLTypeSCRAMSHA512
	config.Net.SASL.SCRAMClientGeneratorFunc = func() sarama.SCRAMClient { return &XDGSCRAMClient{HashGeneratorFcn: SHA512} }
	log.Info().Str("user", user).Msg("Kafka SASL/SCRAM-SHA-512 enabled")
}
