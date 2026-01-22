# Multi-stage Dockerfile for Simplicity
# Stage 1: Build the uberjar
FROM docker.io/clojure:temurin-21-tools-deps-alpine AS builder

WORKDIR /build

# Copy dependency files first (for layer caching)
COPY deps.edn .
COPY build.clj .

# Download dependencies
RUN clojure -P -T:build

# Copy source code
COPY components/ components/
COPY bases/ bases/

# Build uberjar
RUN clojure -T:build uberjar

# Verify jar was created
RUN ls -lh target/simplicity-standalone.jar

# Stage 2: Runtime image
FROM docker.io/eclipse-temurin:21-jre-alpine

# Install runtime dependencies
RUN apk add --no-cache \
    curl \
    bash

# Create non-root user
RUN addgroup -g 1000 simplicity && \
    adduser -D -u 1000 -G simplicity simplicity

# Set working directory
WORKDIR /app

# Copy the uberjar from builder
COPY --from=builder /build/target/simplicity-standalone.jar /app/simplicity.jar

# Create directories for data and logs
RUN mkdir -p /app/data /app/logs && \
    chown -R simplicity:simplicity /app

# Switch to non-root user
USER simplicity

# Environment variables (with defaults)
ENV PORT=3000 \
    DB_PATH=/app/data/simplicity.db \
    LOG_PATH=/app/logs \
    LOG_LEVEL=INFO \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"

# Expose port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:${PORT}/health || exit 1

# Run the application
CMD java $JAVA_OPTS -jar /app/simplicity.jar
