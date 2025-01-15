/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */
package db

import (
	"database/sql"

	"github.com/fergusstrange/embedded-postgres"
	"github.com/google/uuid"
	_ "github.com/lib/pq" // PostgreSQL driver
	"github.com/rs/zerolog/log"
)

type Product struct {
	ID       string
	Name     string
	Category string
	ImageID  string
	Price    float64
}

var embeddedDB *embeddedpostgres.EmbeddedPostgres

func Init() {
	// Start the embedded PostgreSQL database

	embeddedDB = embeddedpostgres.NewDatabase(embeddedpostgres.DefaultConfig().Port(6432).Username("postgres").Database("postgres").Password("password"))
	err := embeddedDB.Start()
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to start embedded PostgreSQL database")
	}

	// Connect to the PostgreSQL database
	connStr := "host=localhost port=6432 user=postgres dbname=postgres sslmode=disable password=password"
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to connect to PostgreSQL database")
	}
	defer db.Close()

	// Drop table if exists
	log.Info().Msg("Dropping table if exists")
	_, err = db.Exec("DROP TABLE IF EXISTS products_toys")
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to drop table")
	}

	// Create table
	log.Info().Msg("Creating table products_toys")
	createTableQuery := `
        CREATE TABLE products_toys (
            id TEXT PRIMARY KEY,
            name TEXT,
            category TEXT,
            imageId TEXT,
            price REAL
        )
    `
	_, err = db.Exec(createTableQuery)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to create table")
	}

	// Insert products
	log.Info().Msg("Inserting data into products_toys")
	products := []Product{
		{ID: uuid.New().String(), Name: "Steadybit Stickers", Category: "TOYS", ImageID: "sticker", Price: 0.99},
		{ID: uuid.New().String(), Name: "Steadybit Keychain", Category: "TOYS", ImageID: "keychain", Price: 3.99},
		{ID: uuid.New().String(), Name: "Steadybit Pillow", Category: "TOYS", ImageID: "pillow", Price: 8.99},
	}

	insertQuery := "INSERT INTO products_toys (id, name, category, imageId, price) VALUES ($1, $2, $3, $4, $5)"
	for _, product := range products {
		_, err := db.Exec(insertQuery, product.ID, product.Name, product.Category, product.ImageID, product.Price)
		if err != nil {
			log.Fatal().Err(err).Str("product", product.Name).Msg("Failed to insert product")
		}
	}

	log.Info().Msg("Data inserted")

	// Query and print products
	log.Info().Msg("Querying products from database")
	rows, err := db.Query("SELECT id, name, category, imageId, price FROM products_toys")
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to query products")
	}
	defer rows.Close()

	for rows.Next() {
		var product Product
		err := rows.Scan(&product.ID, &product.Name, &product.Category, &product.ImageID, &product.Price)
		if err != nil {
			log.Fatal().Err(err).Msg("Failed to scan product")
		}
		log.Info().
			Str("ID", product.ID).
			Str("Name", product.Name).
			Str("Category", product.Category).
			Str("ImageID", product.ImageID).
			Float64("Price", product.Price).
			Msg("Product details")
	}
}

func Stop() {
	if embeddedDB == nil {
		return
	}
	err := embeddedDB.Stop()
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to stop embedded PostgreSQL database")
		return
	}
}
