/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package main

import (
	"encoding/json"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/rs/zerolog/log"
	"math/rand"
	"net/http"
)

func isAvailable(w http.ResponseWriter, r *http.Request) {
	id := r.URL.Query().Get("id")
	response := rand.Float64() > 0.005
	log.Debug().Str("Id", id).Str("response", "inventoryHandler").Msgf("response: %v", response)
	w.Header().Set("Content-Type", "application/json")
	err := json.NewEncoder(w).Encode(response)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

func main() {
	http.HandleFunc("/inventory", isAvailable)
	http.Handle("/metrics", promhttp.Handler())
	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, err := w.Write([]byte("ok"))
		if err != nil {
			log.Error().Err(err).Msg("Failed to write response")
			return
		}
	})
	port := "8084"
	println("Server running on port:", port)
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		panic(err)
	}
}
