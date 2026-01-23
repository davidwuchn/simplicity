#!/bin/bash
# Health monitoring script for Simplicity
# Supports multiple alerting channels: email, Slack, Discord, webhook
# Prometheus-compatible metrics output

set -euo pipefail

# Configuration
HEALTH_URL="${HEALTH_URL:-http://localhost:3000/health}"
CHECK_INTERVAL="${CHECK_INTERVAL:-60}"  # seconds
MAX_RETRIES="${MAX_RETRIES:-3}"
RETRY_DELAY="${RETRY_DELAY:-5}"
TIMEOUT="${TIMEOUT:-10}"

# Alert channels (set via environment)
ALERT_EMAIL="${ALERT_EMAIL:-}"
ALERT_SLACK_WEBHOOK="${ALERT_SLACK_WEBHOOK:-}"
ALERT_DISCORD_WEBHOOK="${ALERT_DISCORD_WEBHOOK:-}"
ALERT_CUSTOM_WEBHOOK="${ALERT_CUSTOM_WEBHOOK:-}"

# State file
STATE_DIR="${STATE_DIR:-/tmp/simplicity-monitor}"
STATE_FILE="$STATE_DIR/health-state.txt"
METRICS_FILE="$STATE_DIR/metrics.txt"

mkdir -p "$STATE_DIR"

# Logging
log() {
    echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] $*" | tee -a "$STATE_DIR/monitor.log"
}

# Prometheus metrics
write_metrics() {
    local status=$1
    local response_time=$2
    local timestamp=$(date +%s)
    
    cat > "$METRICS_FILE" <<EOF
# HELP simplicity_health_status Application health status (1=healthy, 0=unhealthy)
# TYPE simplicity_health_status gauge
simplicity_health_status $status $timestamp

# HELP simplicity_response_time_seconds Health endpoint response time in seconds
# TYPE simplicity_response_time_seconds gauge
simplicity_response_time_seconds $response_time $timestamp

# HELP simplicity_health_check_timestamp Last health check timestamp
# TYPE simplicity_health_check_timestamp gauge
simplicity_health_check_timestamp $timestamp
EOF
}

# Alert via email
alert_email() {
    local subject=$1
    local body=$2
    
    if [ -n "$ALERT_EMAIL" ]; then
        echo "$body" | mail -s "$subject" "$ALERT_EMAIL" 2>/dev/null || log "Failed to send email alert"
    fi
}

# Alert via Slack
alert_slack() {
    local status=$1
    local message=$2
    
    if [ -n "$ALERT_SLACK_WEBHOOK" ]; then
        local color="danger"
        [ "$status" = "healthy" ] && color="good"
        
        curl -X POST "$ALERT_SLACK_WEBHOOK" \
            -H "Content-Type: application/json" \
            -d "{\"attachments\":[{\"color\":\"$color\",\"title\":\"Simplicity Health Alert\",\"text\":\"$message\",\"ts\":$(date +%s)}]}" \
            --silent --max-time 10 >/dev/null 2>&1 || log "Failed to send Slack alert"
    fi
}

# Alert via Discord
alert_discord() {
    local status=$1
    local message=$2
    
    if [ -n "$ALERT_DISCORD_WEBHOOK" ]; then
        curl -X POST "$ALERT_DISCORD_WEBHOOK" \
            -H "Content-Type: application/json" \
            -d "{\"content\":\"**Simplicity Health Alert**\n$message\"}" \
            --silent --max-time 10 >/dev/null 2>&1 || log "Failed to send Discord alert"
    fi
}

# Alert via custom webhook
alert_webhook() {
    local status=$1
    local response=$2
    local response_time=$3
    
    if [ -n "$ALERT_CUSTOM_WEBHOOK" ]; then
        curl -X POST "$ALERT_CUSTOM_WEBHOOK" \
            -H "Content-Type: application/json" \
            -d "{\"service\":\"simplicity\",\"status\":\"$status\",\"timestamp\":\"$(date -u +"%Y-%m-%dT%H:%M:%SZ")\",\"response_time\":$response_time,\"response\":$response}" \
            --silent --max-time 10 >/dev/null 2>&1 || log "Failed to send webhook alert"
    fi
}

# Send alerts to all configured channels
send_alerts() {
    local status=$1
    local message=$2
    local response=$3
    local response_time=$4
    
    alert_email "Simplicity Health: $status" "$message"
    alert_slack "$status" "$message"
    alert_discord "$status" "$message"
    alert_webhook "$status" "$response" "$response_time"
}

# Health check with retries
check_health() {
    local attempt=1
    local response=""
    local http_code=""
    local response_time=""
    
    while [ $attempt -le $MAX_RETRIES ]; do
        log "Health check attempt $attempt/$MAX_RETRIES"
        
        # Measure response time
        local start_time=$(date +%s%N)
        response=$(curl -s -w "\n%{http_code}" --max-time "$TIMEOUT" "$HEALTH_URL" 2>&1) || true
        local end_time=$(date +%s%N)
        response_time=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc)
        
        http_code=$(echo "$response" | tail -n1)
        response=$(echo "$response" | head -n-1)
        
        if [ "$http_code" = "200" ]; then
            # Validate JSON response
            if echo "$response" | jq -e '.status == "healthy"' >/dev/null 2>&1; then
                log "Health check passed (${response_time}s)"
                write_metrics 1 "$response_time"
                
                # Check if recovering from failure
                if [ -f "$STATE_FILE" ] && [ "$(cat "$STATE_FILE")" = "unhealthy" ]; then
                    local message="Application recovered: $HEALTH_URL is now healthy (response time: ${response_time}s)"
                    log "$message"
                    send_alerts "healthy" "$message" "$response" "$response_time"
                fi
                
                echo "healthy" > "$STATE_FILE"
                return 0
            fi
        fi
        
        log "Health check failed: HTTP $http_code, response: $response"
        
        if [ $attempt -lt $MAX_RETRIES ]; then
            log "Retrying in ${RETRY_DELAY}s..."
            sleep "$RETRY_DELAY"
        fi
        
        attempt=$((attempt + 1))
    done
    
    # All retries failed
    write_metrics 0 "${response_time:-0}"
    
    # Check if this is a new failure
    if [ ! -f "$STATE_FILE" ] || [ "$(cat "$STATE_FILE")" != "unhealthy" ]; then
        local message="Application unhealthy: $HEALTH_URL failed after $MAX_RETRIES attempts\nLast HTTP code: $http_code\nLast response: $response\nResponse time: ${response_time}s"
        log "$message"
        send_alerts "unhealthy" "$message" "$response" "${response_time:-0}"
    else
        log "Application still unhealthy (alert already sent)"
    fi
    
    echo "unhealthy" > "$STATE_FILE"
    return 1
}

# Main monitoring loop
monitor() {
    log "Starting health monitoring: $HEALTH_URL (interval: ${CHECK_INTERVAL}s)"
    
    # Validate configuration
    if [ -z "$HEALTH_URL" ]; then
        log "ERROR: HEALTH_URL not set"
        exit 1
    fi
    
    # Check dependencies
    for cmd in curl jq bc; do
        if ! command -v $cmd &> /dev/null; then
            log "ERROR: Required command not found: $cmd"
            exit 1
        fi
    done
    
    log "Alert channels configured:"
    [ -n "$ALERT_EMAIL" ] && log "  - Email: $ALERT_EMAIL"
    [ -n "$ALERT_SLACK_WEBHOOK" ] && log "  - Slack webhook"
    [ -n "$ALERT_DISCORD_WEBHOOK" ] && log "  - Discord webhook"
    [ -n "$ALERT_CUSTOM_WEBHOOK" ] && log "  - Custom webhook"
    [ -z "$ALERT_EMAIL$ALERT_SLACK_WEBHOOK$ALERT_DISCORD_WEBHOOK$ALERT_CUSTOM_WEBHOOK" ] && log "  - None (monitoring only)"
    
    # Continuous monitoring
    while true; do
        check_health || true  # Don't exit on failure
        sleep "$CHECK_INTERVAL"
    done
}

# One-shot check (for cron)
check_once() {
    if ! check_health; then
        exit 1
    fi
}

# Show metrics
show_metrics() {
    if [ -f "$METRICS_FILE" ]; then
        cat "$METRICS_FILE"
    else
        echo "No metrics available"
        exit 1
    fi
}

# Main
case "${1:-monitor}" in
    monitor)
        monitor
        ;;
    check)
        check_once
        ;;
    metrics)
        show_metrics
        ;;
    *)
        echo "Usage: $0 {monitor|check|metrics}"
        echo ""
        echo "Commands:"
        echo "  monitor   - Continuous monitoring (default)"
        echo "  check     - One-shot health check (for cron)"
        echo "  metrics   - Show Prometheus metrics"
        echo ""
        echo "Environment variables:"
        echo "  HEALTH_URL              - Health endpoint URL (default: http://localhost:3000/health)"
        echo "  CHECK_INTERVAL          - Check interval in seconds (default: 60)"
        echo "  MAX_RETRIES             - Max retry attempts (default: 3)"
        echo "  RETRY_DELAY             - Delay between retries in seconds (default: 5)"
        echo "  TIMEOUT                 - Request timeout in seconds (default: 10)"
        echo "  ALERT_EMAIL             - Email address for alerts"
        echo "  ALERT_SLACK_WEBHOOK     - Slack webhook URL"
        echo "  ALERT_DISCORD_WEBHOOK   - Discord webhook URL"
        echo "  ALERT_CUSTOM_WEBHOOK    - Custom webhook URL (receives JSON)"
        echo "  STATE_DIR               - Directory for state files (default: /tmp/simplicity-monitor)"
        echo ""
        echo "Examples:"
        echo "  # Continuous monitoring with Slack alerts"
        echo "  ALERT_SLACK_WEBHOOK=https://hooks.slack.com/... $0 monitor"
        echo ""
        echo "  # Cron job (every 5 minutes)"
        echo "  */5 * * * * HEALTH_URL=https://app.example.com/health ALERT_EMAIL=admin@example.com $0 check"
        echo ""
        echo "  # Prometheus metrics scrape"
        echo "  $0 metrics"
        exit 1
        ;;
esac
