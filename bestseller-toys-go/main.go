/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package main

import (
	"bestseller-toys/db"
	"bestseller-toys/products"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/rs/zerolog/log"
	"github.com/steadybit/extension-kit/extlogging"
	"net/http"
)

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	_, err := w.Write([]byte("OK"))
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

func readinessHandler(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	_, err := w.Write([]byte("READY"))
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

func main() {
	log.Info().Msg("Starting Bestseller Toys Application...")
	extlogging.InitZeroLog()
	http.Handle("/metrics", promhttp.Handler())
	defer db.Stop()
	db.Init()
	// Setup HTTP server
	products.Init()
	http.HandleFunc("/products", func(w http.ResponseWriter, r *http.Request) {
		log.Info().Str("method", r.Method).Str("path", r.URL.Path).Msg("Incoming request")
		products.GetBestsellerProducts(w, r)
	})
	http.HandleFunc("/actuator/health/liveness", healthHandler)     // Liveness Probe
	http.HandleFunc("/actuator/health/readiness", readinessHandler) // Readiness Probe
	port := "8081"
	println("Server running on port:", port)
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		panic(err)
	}
}
