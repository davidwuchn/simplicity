# Example Configurations

This directory contains example configuration files for deploying Simplicity in various environments.

## Files

### Production Environment Variables
**File**: `production.env`

Template for production environment variables. Copy and customize for your deployment.

**Quick Start**:
```bash
cp examples/production.env .env
# Edit .env with your settings
# IMPORTANT: Generate a secure session secret
export SESSION_SECRET=$(openssl rand -base64 32)
```

### systemd Service
**File**: `systemd/simplicity.service`

systemd service unit for running Simplicity as a system service on Linux.

**Installation**:
```bash
# Copy service file
sudo cp examples/systemd/simplicity.service /etc/systemd/system/

# Create user
sudo useradd -r -s /bin/false simplicity

# Create directories
sudo mkdir -p /opt/simplicity /var/lib/simplicity /var/log/simplicity
sudo chown simplicity:simplicity /var/lib/simplicity /var/log/simplicity

# Copy jar
sudo cp target/simplicity-standalone.jar /opt/simplicity/

# Copy environment file
sudo mkdir -p /etc/simplicity
sudo cp examples/production.env /etc/simplicity/production.env
sudo chmod 600 /etc/simplicity/production.env

# Enable and start
sudo systemctl daemon-reload
sudo systemctl enable simplicity
sudo systemctl start simplicity

# Check status
sudo systemctl status simplicity
sudo journalctl -u simplicity -f
```

### Nginx Reverse Proxy
**File**: `nginx/simplicity.conf`

Nginx configuration for reverse proxy with HTTPS, rate limiting, and security headers.

**Installation**:
```bash
# Install certbot for Let's Encrypt
sudo apt install certbot python3-certbot-nginx

# Copy config
sudo cp examples/nginx/simplicity.conf /etc/nginx/sites-available/simplicity

# Edit with your domain
sudo nano /etc/nginx/sites-available/simplicity

# Enable site
sudo ln -s /etc/nginx/sites-available/simplicity /etc/nginx/sites-enabled/

# Get SSL certificate
sudo certbot --nginx -d example.com -d www.example.com

# Test and reload
sudo nginx -t
sudo systemctl reload nginx
```

### Docker Compose (Production)
**File**: `docker-compose.production.yml`

Production-ready Docker Compose configuration with:
- Resource limits
- Health checks
- Persistent volumes
- Logging configuration
- Security hardening

**Usage**:
```bash
# Build image
docker build -t simplicity:latest .

# Start services
docker-compose -f examples/docker-compose.production.yml up -d

# Check status
docker-compose -f examples/docker-compose.production.yml ps

# View logs
docker-compose -f examples/docker-compose.production.yml logs -f

# Stop services
docker-compose -f examples/docker-compose.production.yml down
```

## Deployment Scenarios

### Scenario 1: VPS with systemd
Best for: Dedicated servers, VPS

**Stack**: systemd + Nginx + Let's Encrypt
1. Install Java 17+
2. Build uberjar: `clojure -T:build uberjar`
3. Set up systemd service (see above)
4. Configure Nginx reverse proxy (see above)
5. Obtain SSL certificate with certbot

**Cost**: $5-12/month (VPS)

### Scenario 2: Docker on VPS
Best for: Containerized deployments, easy updates

**Stack**: Docker + Docker Compose + Nginx (optional)
1. Install Docker and Docker Compose
2. Build Docker image
3. Deploy with docker-compose.production.yml
4. (Optional) Set up Nginx for SSL termination

**Cost**: $5-12/month (VPS)

### Scenario 3: Cloudflare Tunnel
Best for: Zero exposed ports, maximum security

**Stack**: Cloudflare Tunnel + Docker
1. Install cloudflared
2. Create tunnel: `cloudflared tunnel create simplicity`
3. Configure routing to localhost:3000
4. Deploy app with Docker Compose
5. Run tunnel: `cloudflared tunnel run simplicity`

**Cost**: $0 (Cloudflare free tier)

See [docs/deployment-cloudflare.md](../docs/deployment-cloudflare.md) for detailed Cloudflare deployment guide.

## Security Checklist

Before deploying to production:

- [ ] Generate secure `SESSION_SECRET` (32+ bytes random)
- [ ] Set `ENABLE_HSTS=true` (only with HTTPS)
- [ ] Set `LOG_LEVEL=WARN` or `ERROR`
- [ ] Configure firewall (allow only 22, 80, 443)
- [ ] Enable automatic security updates
- [ ] Set up database backups
- [ ] Configure monitoring (health checks)
- [ ] Review [docs/security.md](../docs/security.md)
- [ ] Test rate limiting
- [ ] Verify CSRF protection
- [ ] Check security headers

## Monitoring

### Health Check Endpoint
```bash
curl http://localhost:3000/health
```

Expected response:
```json
{
  "status": "ok",
  "timestamp": "2026-01-22T10:30:00Z",
  "database": "ok",
  "version": "1.0.0"
}
```

### Logs
- **systemd**: `sudo journalctl -u simplicity -f`
- **Docker**: `docker-compose logs -f`
- **File**: `/var/log/simplicity/app.log` (or `/app/logs/app.log` in Docker)

### Metrics
Monitor these metrics for production:
- HTTP response times (p50, p95, p99)
- Error rates (4xx, 5xx)
- Database query times
- Memory usage (heap, non-heap)
- CPU usage
- Active sessions

## Backup and Restore

### Backup Database
```bash
# systemd deployment
sudo -u simplicity sqlite3 /var/lib/simplicity/simplicity.db ".backup /backup/simplicity-$(date +%Y%m%d).db"

# Docker deployment
docker exec simplicity-app sqlite3 /app/data/simplicity.db ".backup /app/data/backup-$(date +%Y%m%d).db"
docker cp simplicity-app:/app/data/backup-*.db ./backups/
```

### Restore Database
```bash
# systemd deployment
sudo systemctl stop simplicity
sudo -u simplicity cp /backup/simplicity-20260122.db /var/lib/simplicity/simplicity.db
sudo systemctl start simplicity

# Docker deployment
docker-compose down
docker cp ./backups/simplicity-20260122.db simplicity-app:/app/data/simplicity.db
docker-compose up -d
```

## Updating

### systemd Deployment
```bash
# Build new version
clojure -T:build uberjar

# Stop service
sudo systemctl stop simplicity

# Backup current jar
sudo cp /opt/simplicity/simplicity-standalone.jar /opt/simplicity/simplicity-standalone.jar.bak

# Copy new jar
sudo cp target/simplicity-standalone.jar /opt/simplicity/

# Start service
sudo systemctl start simplicity

# Check status
sudo systemctl status simplicity
```

### Docker Deployment
```bash
# Build new image
docker build -t simplicity:latest .

# Tag with version
docker tag simplicity:latest simplicity:1.0.1

# Update deployment
docker-compose -f examples/docker-compose.production.yml down
docker-compose -f examples/docker-compose.production.yml up -d

# Verify
docker-compose ps
curl http://localhost:3000/health
```

## Troubleshooting

See [docs/TROUBLESHOOTING.md](../docs/TROUBLESHOOTING.md) for detailed troubleshooting guide.

**Quick checks**:
```bash
# Check if service is running
systemctl status simplicity
# or
docker-compose ps

# Check logs
journalctl -u simplicity -n 50
# or
docker-compose logs --tail=50

# Check port
lsof -i :3000

# Check disk space
df -h

# Check database
sqlite3 /var/lib/simplicity/simplicity.db "PRAGMA integrity_check;"
```

---

*For more information, see the main [README.md](../README.md) and [deployment guide](../docs/deployment-cloudflare.md).*
