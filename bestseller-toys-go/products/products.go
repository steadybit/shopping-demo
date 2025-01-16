/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package products

import (
	"encoding/json"
	"fmt"
	"github.com/google/uuid"
	"net/http"
	"os"

	"github.com/rs/zerolog/log"
)

// Product represents a product in the database.
type Product struct {
	ID           string  `json:"id"`
	Name         string  `json:"name"`
	Category     string  `json:"category"`
	ImageID      string  `json:"imageId"`
	Price        float64 `json:"price"`
	Availability string  `json:"availability"`
}

var (
	products         []Product
	inventoryURL     string
	disableInventory bool
)

func Init() {
	// Load environment variables or defaults
	inventoryURL = os.Getenv("REST_ENDPOINT_INVENTORY")
	if inventoryURL == "" {
		inventoryURL = "http://localhost:8084/inventory"
	}
	disableInventory = os.Getenv("REST_ENDPOINT_INVENTORY_DISABLE") == "true"
	products = []Product{
		{ID: uuid.New().String(), Name: "Steadybit Stickers", Category: "TOYS", ImageID: "sticker", Price: 0.99},
		{ID: uuid.New().String(), Name: "Steadybit Keychain", Category: "TOYS", ImageID: "keychain", Price: 3.99},
		{ID: uuid.New().String(), Name: "Steadybit Pillow", Category: "TOYS", ImageID: "pillow", Price: 8.99},
	}
}

// getBestsellerProducts handles the "/products" endpoint.
func GetBestsellerProducts(w http.ResponseWriter, r *http.Request) {
	// Query products from the database

	for i := range products {
		// Set availability
		if disableInventory {
			products[i].Availability = "AVAILABLE"
		} else {
			products[i].Availability = getProductAvailability(products[i].ID)
		}
	}

	// Write the response as JSON
	w.Header().Set("Content-Type", "application/json")
	err := json.NewEncoder(w).Encode(products)
	if err != nil {
		log.Error().Err(err).Msg("Failed to encode products to JSON")
		http.Error(w, "Failed to encode response", http.StatusInternalServerError)
	}
	log.Info().Int("productCount", len(products)).Msg("Successfully retrieved products")
}

// getProductAvailability queries the inventory service for product availability.
func getProductAvailability(productID string) string {
	url := fmt.Sprintf("%s?id=%s", inventoryURL, productID)
	resp, err := http.Get(url)
	if err != nil {
		log.Warn().Err(err).Str("productID", productID).Msg("Failed to retrieve availability")
		return "UNKNOWN"
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		log.Warn().
			Int("statusCode", resp.StatusCode).
			Str("productID", productID).
			Msg("Non-OK response from inventory service")
		return "UNKNOWN"
	}

	var isAvailable bool
	err = json.NewDecoder(resp.Body).Decode(&isAvailable)
	if err != nil {
		log.Warn().Err(err).Str("productID", productID).Msg("Failed to decode availability response")
		return "UNKNOWN"
	}

	if isAvailable {
		return "AVAILABLE"
	}
	return "UNAVAILABLE"
}
