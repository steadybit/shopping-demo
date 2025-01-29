/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package main

import (
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
	log.Info().Msg("Starting Order Application...")
	extlogging.InitZeroLog()
	http.Handle("/metrics", promhttp.Handler())
	// Setup HTTP server
	http.HandleFunc("/actuator/health/liveness", healthHandler)     // Liveness Probe
	http.HandleFunc("/actuator/health/readiness", readinessHandler) // Readiness Probe
	port := "8086"
	println("Server running on port:", port)
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		panic(err)
	}
}
