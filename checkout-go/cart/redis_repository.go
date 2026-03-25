/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package cart

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/redis/go-redis/v9"
)

type RedisCartRepository struct {
	client *redis.Client
}

func NewRedisCartRepository(redisURL string) (*RedisCartRepository, error) {
	opts, err := redis.ParseURL(redisURL)
	if err != nil {
		return nil, fmt.Errorf("invalid redis URL: %w", err)
	}
	client := redis.NewClient(opts)

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := client.Ping(ctx).Err(); err != nil {
		return nil, fmt.Errorf("failed to connect to redis: %w", err)
	}

	return &RedisCartRepository{client: client}, nil
}

func (r *RedisCartRepository) Save(c Cart) {
	ctx := context.Background()
	data, err := json.Marshal(c)
	if err != nil {
		return
	}
	r.client.Set(ctx, redisKey(c.ID), data, 24*time.Hour)
}

func (r *RedisCartRepository) MarkAsPublished(ids []string, now time.Time) error {
	ctx := context.Background()
	for _, id := range ids {
		data, err := r.client.Get(ctx, redisKey(id)).Bytes()
		if err != nil {
			return fmt.Errorf("cart not found: %s", id)
		}
		var c Cart
		if err := json.Unmarshal(data, &c); err != nil {
			return fmt.Errorf("failed to unmarshal cart %s: %w", id, err)
		}
		c.OrderPublished = now
		updated, _ := json.Marshal(c)
		r.client.Set(ctx, redisKey(id), updated, 24*time.Hour)
	}
	return nil
}

func (r *RedisCartRepository) FindPublishPending() ([]*Cart, error) {
	ctx := context.Background()
	var pending []*Cart
	var cursor uint64
	for {
		keys, next, err := r.client.Scan(ctx, cursor, "cart:*", 100).Result()
		if err != nil {
			return nil, err
		}
		for _, key := range keys {
			data, err := r.client.Get(ctx, key).Bytes()
			if err != nil {
				continue
			}
			var c Cart
			if err := json.Unmarshal(data, &c); err != nil {
				continue
			}
			if c.OrderPublished.IsZero() {
				pending = append(pending, &c)
			}
		}
		cursor = next
		if cursor == 0 {
			break
		}
	}
	return pending, nil
}

func redisKey(id string) string {
	if strings.HasPrefix(id, "cart:") {
		return id
	}
	return "cart:" + id
}
