FROM --platform=$BUILDPLATFORM golang:1.25 AS builder
ARG BUILDPLATFORM
ARG TARGETPLATFORM
ARG TARGETOS
ARG TARGETARCH
ARG TARGETVARIANT

WORKDIR /app

# Copy the Go modules manifest files
COPY go.mod go.sum ./
RUN go mod download

# Copy the source code
COPY . .

# Build the application with static linking
RUN CGO_ENABLED=0 GOOS=$TARGETOS GOARCH=$TARGETARCH go build -a -o checkout main.go
RUN make licenses-report

# Use minimal image for deployment
FROM  alpine:3.18

WORKDIR /app

# Copy the binary from builder
COPY --from=builder /app/checkout .
RUN chgrp -R 0 /app/checkout && chmod -R g=u /app/checkout

# Expose the application port
EXPOSE 8085

# Run the application
CMD ["./checkout"]