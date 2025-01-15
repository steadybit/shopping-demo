/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package products

import (
	"database/sql"
	"encoding/json"
	"fmt"
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
	db               *sql.DB
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

	// Connect to the PostgreSQL database
	connStr := "host=localhost port=6432 user=postgres dbname=postgres sslmode=disable password=password"
	var err error
	db, err = sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to connect to PostgreSQL database")
	}
}

// getBestsellerProducts handles the "/products" endpoint.
func GetBestsellerProducts(w http.ResponseWriter, r *http.Request) {
	// Query products from the database
	rows, err := db.Query("SELECT id, name, category, imageId, price FROM products_toys")
	if err != nil {
		log.Error().Err(err).Msg("Failed to query products from database")
		http.Error(w, "Failed to query products", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var products []Product
	for rows.Next() {
		var product Product
		err := rows.Scan(&product.ID, &product.Name, &product.Category, &product.ImageID, &product.Price)
		if err != nil {
			log.Error().Err(err).Msg("Failed to scan product")
			continue
		}

		// Set availability
		if disableInventory {
			product.Availability = "AVAILABLE"
		} else {
			product.Availability = getProductAvailability(product.ID)
		}

		products = append(products, product)
	}

	// Write the response as JSON
	w.Header().Set("Content-Type", "application/json")
	err = json.NewEncoder(w).Encode(products)
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
