# Use official Golang image as builder
FROM golang:1.22 AS builder

WORKDIR /app

# Copy the Go modules manifest files
COPY go.mod go.sum ./
RUN go mod download

# Copy the source code
COPY . .

# Build the application with static linking
RUN CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -a -o order main.go
RUN make licenses-report

# Use minimal image for deployment
FROM alpine:3.18

WORKDIR /app

# Copy the binary from builder
COPY --from=builder /app/order .
RUN chgrp -R 0 /app/order && chmod -R g=u /app/order

# Expose the application port
EXPOSE 8081

# Run the application
CMD ["./order"]