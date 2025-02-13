/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package cart

import (
	_ "gorm.io/gorm"
	"math/big"
	"time"
)

type Cart struct {
	ID             string `json:"id" gorm:"primaryKey"`
	Version        int64
	OrderPublished time.Time
	Submitted      time.Time
	Items          []Item `json:"items" gorm:"embedded"`
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

type OrderItem struct {
	ProductID string  `json:"productId"`
	Quantity  int     `json:"quantity"`
	Price     *big.Float `json:"price"`
}

type ShoppingCart struct {
	Id    string      `json:"id"`
	Items []OrderItem `json:"items"`
}

type Order struct {
	ID        string      `json:"id"`
	Submitted time.Time      `json:"submitted"`
	Items     []OrderItem `json:"items"`
}
