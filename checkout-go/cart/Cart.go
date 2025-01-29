/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package cart

import (
	"math/big"
	"time"
)

type Cart struct {
	ID             string
	Version        int64
	OrderPublished time.Time
	Submitted      time.Time
	Items          []Item
}

type Item struct {
	ID       string
	Quantity int
	Price    *big.Float
}

func NewItem(id string, quantity int, price *big.Float) Item {
	return Item{
		ID:       id,
		Quantity: quantity,
		Price:    price,
	}
}
