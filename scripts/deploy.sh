#!/bin/bash
# Zero-downtime deployment script for Simplicity
# Implements blue/green deployment strategy with health checks

set -euo pipefail

# Configuration
APP_NAME="${APP_NAME:-simplicity}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/simplicity}"
HEALTH_URL="${HEALTH_URL:-http://localhost:3000/health}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-60}"  # seconds
HEALTH_CHECK_INTERVAL="${HEALTH_CHECK_INTERVAL:-2}"  # seconds
OLD_CONTAINER_GRACE_PERIOD="${OLD_CONTAINER_GRACE_PERIOD:-30}"  # seconds
ARTIFACT_TYPE="${ARTIFACT_TYPE:-docker}"  # docker or jar

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging
log() {
    echo -e "${BLUE}[$(date -u +"%Y-%m-%dT%H:%M:%SZ")]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] ✓${NC} $*"
}

log_error() {
    echo -e "${RED}[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] ✗${NC} $*" >&2
}

log_warn() {
    echo -e "${YELLOW}[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] ⚠${NC} $*"
}

# Cleanup on error
cleanup_on_error() {
    log_error "Deployment failed, rolling back..."
    
    if [ "$ARTIFACT_TYPE" = "docker" ]; then
        # Stop new container if it exists
        if docker ps -a --format '{{.Names}}' | grep -q "^${APP_NAME}-new$"; then
            log "Stopping new container: ${APP_NAME}-new"
            docker stop "${APP_NAME}-new" >/dev/null 2>&1 || true
            docker rm "${APP_NAME}-new" >/dev/null 2>&1 || true
        fi
        
        # Restore old container if it exists
        if docker ps -a --format '{{.Names}}' | grep -q "^${APP_NAME}-old$"; then
            log "Restoring old container"
            docker stop "$APP_NAME" >/dev/null 2>&1 || true
            docker rm "$APP_NAME" >/dev/null 2>&1 || true
            docker rename "${APP_NAME}-old" "$APP_NAME"
            docker start "$APP_NAME" >/dev/null 2>&1 || true
            log_success "Rollback complete, old version restored"
        fi
    else
        # For jar deployments, systemd will auto-restart
        log "Restarting service via systemd"
        sudo systemctl restart "$APP_NAME" || true
    fi
    
    exit 1
}

trap cleanup_on_error ERR

# Wait for health check
wait_for_health() {
    local url=$1
    local timeout=$2
    local elapsed=0
    
    log "Waiting for health check at $url (timeout: ${timeout}s)..."
    
    while [ $elapsed -lt $timeout ]; do
        if curl -sf --max-time 5 "$url" | jq -e '.status == "healthy"' >/dev/null 2>&1; then
            log_success "Health check passed"
            return 0
        fi
        
        sleep "$HEALTH_CHECK_INTERVAL"
        elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
        
        if [ $((elapsed % 10)) -eq 0 ]; then
            log "Still waiting... (${elapsed}/${timeout}s)"
        fi
    done
    
    log_error "Health check failed after ${timeout}s"
    return 1
}

# Docker deployment
deploy_docker() {
    local image=$1
    
    log "Starting Docker deployment: $image"
    
    # Check if container is running
    if ! docker ps --format '{{.Names}}' | grep -q "^${APP_NAME}$"; then
        log_warn "No running container found, performing fresh deployment"
        
        docker run -d \
            --name "$APP_NAME" \
            -p 3000:3000 \
            -v "${APP_NAME}-data:/app/data" \
            -v "${APP_NAME}-logs:/app/logs" \
            -e PORT=3000 \
            -e LOG_LEVEL="${LOG_LEVEL:-WARN}" \
            -e ENABLE_HSTS="${ENABLE_HSTS:-false}" \
            --restart unless-stopped \
            "$image"
        
        wait_for_health "$HEALTH_URL" "$HEALTH_TIMEOUT"
        log_success "Fresh deployment complete"
        return 0
    fi
    
    # Blue/green deployment
    log "Performing blue/green deployment"
    
    # Step 1: Rename old container
    log "Renaming current container: $APP_NAME -> ${APP_NAME}-old"
    docker rename "$APP_NAME" "${APP_NAME}-old"
    
    # Step 2: Start new container
    log "Starting new container from image: $image"
    docker run -d \
        --name "$APP_NAME" \
        -p 3000:3000 \
        -v "${APP_NAME}-data:/app/data" \
        -v "${APP_NAME}-logs:/app/logs" \
        -e PORT=3000 \
        -e LOG_LEVEL="${LOG_LEVEL:-WARN}" \
        -e ENABLE_HSTS="${ENABLE_HSTS:-false}" \
        --restart unless-stopped \
        "$image"
    
    # Step 3: Wait for health check
    if ! wait_for_health "$HEALTH_URL" "$HEALTH_TIMEOUT"; then
        cleanup_on_error
    fi
    
    # Step 4: Grace period for old connections
    log "Waiting ${OLD_CONTAINER_GRACE_PERIOD}s for old connections to drain..."
    sleep "$OLD_CONTAINER_GRACE_PERIOD"
    
    # Step 5: Stop old container
    log "Stopping old container: ${APP_NAME}-old"
    docker stop "${APP_NAME}-old" >/dev/null 2>&1 || true
    docker rm "${APP_NAME}-old" >/dev/null 2>&1 || true
    
    # Step 6: Cleanup old images (keep last 3)
    log "Cleaning up old images..."
    docker images "$APP_NAME" --format "{{.ID}}" | tail -n +4 | xargs -r docker rmi >/dev/null 2>&1 || true
    
    log_success "Blue/green deployment complete"
}

# JAR deployment
deploy_jar() {
    local jar_path=$1
    
    log "Starting JAR deployment: $jar_path"
    
    if [ ! -f "$jar_path" ]; then
        log_error "JAR file not found: $jar_path"
        exit 1
    fi
    
    # Backup current JAR
    if [ -f "$DEPLOY_DIR/simplicity.jar" ]; then
        log "Backing up current JAR"
        cp "$DEPLOY_DIR/simplicity.jar" "$DEPLOY_DIR/simplicity.jar.backup"
    fi
    
    # Copy new JAR
    log "Copying new JAR to $DEPLOY_DIR"
    cp "$jar_path" "$DEPLOY_DIR/simplicity.jar.new"
    
    # Atomic swap
    log "Performing atomic swap"
    mv "$DEPLOY_DIR/simplicity.jar.new" "$DEPLOY_DIR/simplicity.jar"
    
    # Restart service
    log "Restarting systemd service: $APP_NAME"
    sudo systemctl restart "$APP_NAME"
    
    # Wait for health check
    if ! wait_for_health "$HEALTH_URL" "$HEALTH_TIMEOUT"; then
        log_error "Health check failed, rolling back"
        if [ -f "$DEPLOY_DIR/simplicity.jar.backup" ]; then
            cp "$DEPLOY_DIR/simplicity.jar.backup" "$DEPLOY_DIR/simplicity.jar"
            sudo systemctl restart "$APP_NAME"
        fi
        cleanup_on_error
    fi
    
    # Cleanup backup
    log "Removing backup"
    rm -f "$DEPLOY_DIR/simplicity.jar.backup"
    
    log_success "JAR deployment complete"
}

# Pre-deployment checks
pre_deploy_checks() {
    log "Running pre-deployment checks..."
    
    # Check dependencies
    for cmd in curl jq; do
        if ! command -v $cmd &> /dev/null; then
            log_error "Required command not found: $cmd"
            exit 1
        fi
    done
    
    if [ "$ARTIFACT_TYPE" = "docker" ]; then
        if ! command -v docker &> /dev/null; then
            log_error "Docker not found"
            exit 1
        fi
        
        if ! docker ps >/dev/null 2>&1; then
            log_error "Cannot connect to Docker daemon"
            exit 1
        fi
    else
        if ! command -v systemctl &> /dev/null; then
            log_error "systemctl not found (required for JAR deployments)"
            exit 1
        fi
    fi
    
    log_success "Pre-deployment checks passed"
}

# Main deployment
deploy() {
    local artifact=$1
    
    log "========================================="
    log "Simplicity - Zero-Downtime Deployment"
    log "========================================="
    log "Artifact: $artifact"
    log "Type: $ARTIFACT_TYPE"
    log "Health URL: $HEALTH_URL"
    log ""
    
    pre_deploy_checks
    
    if [ "$ARTIFACT_TYPE" = "docker" ]; then
        deploy_docker "$artifact"
    else
        deploy_jar "$artifact"
    fi
    
    log ""
    log "========================================="
    log_success "Deployment completed successfully"
    log "========================================="
    log ""
    log "Verify deployment:"
    log "  curl $HEALTH_URL"
    log ""
}

# Rollback
rollback() {
    log "========================================="
    log "Simplicity - Rollback"
    log "========================================="
    
    if [ "$ARTIFACT_TYPE" = "docker" ]; then
        # Find previous image
        local current_image=$(docker inspect "$APP_NAME" --format '{{.Config.Image}}' 2>/dev/null || echo "")
        if [ -z "$current_image" ]; then
            log_error "No current deployment found"
            exit 1
        fi
        
        local previous_image=$(docker images "$APP_NAME" --format "{{.Repository}}:{{.Tag}}" | grep -v "$current_image" | head -n1)
        if [ -z "$previous_image" ]; then
            log_error "No previous image found for rollback"
            exit 1
        fi
        
        log "Rolling back from $current_image to $previous_image"
        deploy_docker "$previous_image"
    else
        if [ ! -f "$DEPLOY_DIR/simplicity.jar.backup" ]; then
            log_error "No backup JAR found for rollback"
            exit 1
        fi
        
        log "Rolling back to previous JAR"
        deploy_jar "$DEPLOY_DIR/simplicity.jar.backup"
    fi
    
    log_success "Rollback complete"
}

# Show status
status() {
    log "========================================="
    log "Simplicity - Deployment Status"
    log "========================================="
    
    if [ "$ARTIFACT_TYPE" = "docker" ]; then
        echo ""
        echo "Running containers:"
        docker ps --filter "name=${APP_NAME}" --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
        
        echo ""
        echo "Available images:"
        docker images "$APP_NAME" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
    else
        echo ""
        echo "Service status:"
        sudo systemctl status "$APP_NAME" --no-pager || true
        
        echo ""
        echo "JAR files:"
        ls -lh "$DEPLOY_DIR"/*.jar 2>/dev/null || echo "No JAR files found"
    fi
    
    echo ""
    echo "Health check:"
    if curl -sf --max-time 5 "$HEALTH_URL" 2>/dev/null; then
        log_success "Application is healthy"
    else
        log_error "Application is unhealthy"
    fi
}

# Usage
usage() {
    cat <<EOF
Usage: $0 <command> [artifact]

Commands:
  deploy <artifact>  - Deploy new version (Docker image or JAR path)
  rollback           - Rollback to previous version
  status             - Show deployment status

Environment Variables:
  APP_NAME                      - Application name (default: simplicity)
  DEPLOY_DIR                    - Deployment directory for JAR (default: /opt/simplicity)
  HEALTH_URL                    - Health check URL (default: http://localhost:3000/health)
  HEALTH_TIMEOUT                - Health check timeout in seconds (default: 60)
  HEALTH_CHECK_INTERVAL         - Health check interval in seconds (default: 2)
  OLD_CONTAINER_GRACE_PERIOD    - Grace period for old container in seconds (default: 30)
  ARTIFACT_TYPE                 - Artifact type: docker or jar (default: docker)
  LOG_LEVEL                     - Log level for new deployment (default: WARN)
  ENABLE_HSTS                   - Enable HSTS header (default: false)

Examples:
  # Deploy Docker image
  $0 deploy simplicity:latest

  # Deploy JAR file
  ARTIFACT_TYPE=jar $0 deploy /tmp/simplicity-standalone.jar

  # Deploy with custom health URL
  HEALTH_URL=https://app.example.com/health $0 deploy simplicity:v2.0

  # Rollback to previous version
  $0 rollback

  # Check deployment status
  $0 status

EOF
}

# Main
case "${1:-}" in
    deploy)
        if [ -z "${2:-}" ]; then
            log_error "Artifact required"
            usage
            exit 1
        fi
        deploy "$2"
        ;;
    rollback)
        rollback
        ;;
    status)
        status
        ;;
    *)
        usage
        exit 1
        ;;
esac
