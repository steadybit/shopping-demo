/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package cart

import (
	"gorm.io/gorm"
	"time"
)

type CartRepository struct {
	db *gorm.DB
}

func (r *CartRepository) Save(cart Cart) {
	// Save cart to database
}

func NewCartRepository(db *gorm.DB) *CartRepository {
	return &CartRepository{db: db}
}

func (r *CartRepository) MarkAsPublished(ids []string, now time.Time) error {
	return r.db.Model(&Cart{}).Where("id IN ?", ids).Update("order_published", now).Error
}

func (r *CartRepository) FindPublishPending(page, pageSize int) ([]Cart, error) {
	var carts []Cart
	offset := (page - 1) * pageSize
	result := r.db.Where("order_published IS NULL").Offset(offset).Limit(pageSize).Find(&carts)
	return carts, result.Error
}
