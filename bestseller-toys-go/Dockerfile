# Use official Golang image as builder
FROM golang:1.22 AS builder

WORKDIR /app

# Copy the Go modules manifest files
COPY go.mod go.sum ./
RUN go mod download

# Copy the source code
COPY . .

# Build the application with static linking
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -a -o bestseller-toys main.go
RUN make licenses-report

# Use minimal image for deployment
FROM alpine:3.18

WORKDIR /app

# Copy the binary from builder
COPY --from=builder /app/bestseller-toys .
RUN chgrp -R 0 /app/bestseller-toys && chmod -R g=u /app/bestseller-toys

# Expose the application port
EXPOSE 8081

# Run the application
CMD ["./bestseller-toys"]
