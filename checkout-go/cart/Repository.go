package cart

import (
	"errors"
	"sync"
	"time"
)

type CartRepository struct {
	mu    sync.RWMutex
	carts map[string]Cart
}

// NewCartRepository returns a new in-memory CartRepository.
func NewCartRepository() *CartRepository {
	return &CartRepository{
		carts: make(map[string]Cart),
	}
}

// Save adds or updates a Cart in the repository.
func (r *CartRepository) Save(cart Cart) {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.carts[cart.ID] = cart
}

// MarkAsPublished sets the OrderPublished field for each Cart with the given IDs.
func (r *CartRepository) MarkAsPublished(ids []string, now time.Time) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	for _, id := range ids {
		cart, exists := r.carts[id]
		if !exists {
			return errors.New("cart not found: " + id)
		}
		cart.OrderPublished = now
		r.carts[id] = cart
	}
	return nil
}

// FindPublishPending returns a paginated list of Carts that have not been published.
// A cart is considered pending if its OrderPublished field is the zero value.
func (r *CartRepository) FindPublishPending() ([]*Cart, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	var pending []*Cart
	for _, cart := range r.carts {
		if cart.OrderPublished.IsZero() {
			pending = append(pending, &cart)
		}
	}

	return pending, nil
}
