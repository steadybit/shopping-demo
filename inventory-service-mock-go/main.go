/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package main

import (
	"encoding/json"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/rs/zerolog/log"
	"github.com/steadybit/extension-kit/extlogging"
	"math/rand"
	"net/http"
)

func isAvailable(w http.ResponseWriter, r *http.Request) {
	id := r.URL.Query().Get("id")
	if id == "" {
		http.Error(w, "'id' needs to be set", http.StatusBadRequest)
		return
	}
	response := rand.Float64() > 0.01
	log.Debug().Str("Id", id).Str("response", "inventoryHandler").Msgf("response: %v", response)
	w.Header().Set("Content-Type", "application/json")
	err := json.NewEncoder(w).Encode(response)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

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
	log.Info().Msg("Starting up Inventory Service Mock.")
	extlogging.InitZeroLog()
	http.HandleFunc("/inventory", isAvailable)
	http.Handle("/metrics", promhttp.Handler())
	http.HandleFunc("/actuator/health/liveness", healthHandler)     // Liveness Probe
	http.HandleFunc("/actuator/health/readiness", readinessHandler) // Readiness Probe
	port := "8084"
	println("Server running on port:", port)
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		panic(err)
	}
}
