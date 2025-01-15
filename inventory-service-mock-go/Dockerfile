# Use official Golang image as builder
FROM golang:1.23 AS builder

WORKDIR /app

# Copy the Go modules manifest files
COPY go.mod go.sum ./
RUN go mod download

# Copy the source code
COPY . .

# Build the application with static linking
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -a -o inventory-service-mock main.go
RUN make licenses-report

# Use minimal image for deployment
FROM alpine:3.18

WORKDIR /root/

# Copy the binary from builder
COPY --from=builder /app/inventory-service-mock .

# Expose the application port
EXPOSE 8084

# Run the application
CMD ["./inventory-service-mock"]