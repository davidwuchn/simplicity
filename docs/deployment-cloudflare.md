# Cloudflare Deployment Guide

This guide covers deploying the Simplicity application to Cloudflare infrastructure.

## Overview

Since Simplicity is a JVM-based Clojure application, it cannot run directly on Cloudflare Workers (which only supports JavaScript/Wasm). Instead, we deploy the application as a container or traditional server and use Cloudflare for:

1. **CDN & Caching** - Static asset delivery
2. **DDoS Protection** - Automatic mitigation
3. **SSL/TLS** - Automatic HTTPS
4. **DNS Management** - Fast, secure DNS
5. **Web Application Firewall (WAF)** - Security rules

## Quick Start

**Automated Deployment (Recommended)**:
- **Terraform + GitHub Actions**: Full automation with IaC → See [terraform/README.md](../terraform/README.md)
- **Cost**: ~$13-15/month (DigitalOcean + Cloudflare Free)
- **Time**: 5 minutes to production

**Manual Deployment**:
- Follow Option 1 below for step-by-step VPS setup

## Deployment Automation

### GitHub Actions CI/CD

Continuous integration and deployment pipeline automatically:
- ✅ Runs tests on every push/PR
- ✅ Builds Docker images (multi-arch: amd64/arm64)
- ✅ Deploys to staging (on `develop` branch push)
- ✅ Deploys to production (on release)
- ✅ Zero-downtime blue/green deployments
- ✅ Security scanning with Trivy

**Setup**: See `.github/workflows/ci-cd.yml`

**Required Secrets** (in GitHub repo settings):
- `STAGING_SSH_KEY` / `PRODUCTION_SSH_KEY`: SSH private key
- `STAGING_HOST` / `PRODUCTION_HOST`: Server IP address
- `STAGING_USER` / `PRODUCTION_USER`: SSH username (usually `root`)

**Usage**:
```bash
# Auto-deploy to staging
git push origin develop

# Auto-deploy to production
gh release create v1.0.0 --generate-notes
```

### Infrastructure as Code (Terraform)

Provision complete infrastructure in one command:

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your credentials
terraform init
terraform apply
```

**Provisions**:
- DigitalOcean Droplet (VPS) with Docker
- Persistent volume for data/logs
- Firewall (Cloudflare IPs only)
- Cloudflare DNS + SSL + WAF + Rate Limiting
- Automated backups + health monitoring

**See**: [terraform/README.md](../terraform/README.md) for full guide

## Deployment Options

### Option 1: Cloudflare Pages + VPS (Recommended for Manual Setup)

Deploy the backend to a VPS and use Cloudflare as a reverse proxy.

#### 1.1. Build the Docker Image

**Using Babashka (Recommended):**

```bash
# Build the Docker image
bb docker:build

# Or full build with tests
bb build && bb docker:build
```

**Using direct Docker commands:**

```bash
# Build the Docker image
docker build -t simplicity:latest .

# Or use docker-compose
docker-compose build
```

#### 1.2. Deploy to VPS

**Popular VPS Options:**
- DigitalOcean Droplet
- Hetzner Cloud
- Linode
- Vultr
- AWS Lightsail

**Deployment Steps (Automated)**:

```bash
# Use the zero-downtime deployment script
scp scripts/deploy.sh user@your-vps-ip:/opt/simplicity/
ssh user@your-vps-ip

cd /opt/simplicity
./deploy.sh deploy simplicity:latest
```

**Deployment Steps (Manual)**:

```bash
# 1. Save Docker image
docker save simplicity:latest | gzip > simplicity-latest.tar.gz

# 2. Copy to VPS
scp simplicity-latest.tar.gz user@your-vps-ip:/tmp/

# 3. SSH into VPS
ssh user@your-vps-ip

# 4. Load image
docker load < /tmp/simplicity-latest.tar.gz

# 5. Run container with zero-downtime script (recommended)
cd /opt/simplicity
ARTIFACT_TYPE=docker ./scripts/deploy.sh deploy simplicity:latest

# OR: Manual Docker run (not recommended, causes downtime)
docker run -d \
  --name simplicity \
  -p 3000:3000 \
  -v simplicity-data:/app/data \
  -v simplicity-logs:/app/logs \
  -e PORT=3000 \
  -e LOG_LEVEL=WARN \
  --restart unless-stopped \
  simplicity:latest

# 6. Verify it's running
curl http://localhost:3000/health
```

#### 1.3. Configure Cloudflare DNS

1. Log in to [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. Add your domain (or transfer DNS)
3. Create an A record:
   - **Type**: A
   - **Name**: @ (or subdomain like `app`)
   - **IPv4 Address**: Your VPS IP
   - **Proxy status**: Proxied (orange cloud) ✅
   - **TTL**: Auto

#### 1.4. Configure Cloudflare SSL/TLS

1. Go to **SSL/TLS** → **Overview**
2. Set encryption mode to **Full (strict)**
3. Go to **SSL/TLS** → **Edge Certificates**
4. Enable:
   - ✅ Always Use HTTPS
   - ✅ Automatic HTTPS Rewrites
   - ✅ Minimum TLS Version: TLS 1.2

#### 1.5. Configure Cloudflare Firewall Rules

1. Go to **Security** → **WAF**
2. Enable **Managed Rules**
3. Create custom rules:

```
# Block suspicious traffic
(cf.threat_score gt 14) → Block

# Rate limit login endpoint
(http.request.uri.path eq "/login" and http.request.method eq "POST") 
→ Rate Limit (5 requests per minute)

# Block non-browser traffic to login
(http.request.uri.path contains "/login" and not cf.client.bot) 
→ Challenge (CAPTCHA)
```

#### 1.6. Enable HSTS on Backend

Once HTTPS is verified working:

```bash
# Update container environment
docker stop simplicity
docker rm simplicity
docker run -d \
  --name simplicity \
  -p 3000:3000 \
  -v simplicity-data:/app/data \
  -v simplicity-logs:/app/logs \
  -e PORT=3000 \
  -e LOG_LEVEL=WARN \
  -e ENABLE_HSTS=true \
  --restart unless-stopped \
  simplicity:latest
```

### Option 2: Cloudflare Tunnel (Zero Trust)

Use Cloudflare Tunnel to expose your application without opening firewall ports.

#### 2.1. Install cloudflared

```bash
# On VPS
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o cloudflared
chmod +x cloudflared
sudo mv cloudflared /usr/local/bin/
```

#### 2.2. Authenticate

```bash
cloudflared tunnel login
```

#### 2.3. Create Tunnel

```bash
# Create tunnel
cloudflared tunnel create simplicity

# Note the tunnel ID from output
```

#### 2.4. Configure Tunnel

Create `~/.cloudflared/config.yml`:

```yaml
tunnel: <TUNNEL_ID>
credentials-file: /home/user/.cloudflared/<TUNNEL_ID>.json

ingress:
  - hostname: app.yourdomain.com
    service: http://localhost:3000
  - service: http_status:404
```

#### 2.5. Route DNS

```bash
cloudflared tunnel route dns simplicity app.yourdomain.com
```

#### 2.6. Run Tunnel

```bash
# Start application
docker-compose up -d

# Start tunnel
cloudflared tunnel run simplicity
```

#### 2.7. Run as Service (systemd)

Create `/etc/systemd/system/cloudflared.service`:

```ini
[Unit]
Description=Cloudflare Tunnel
After=network.target

[Service]
Type=simple
User=cloudflared
ExecStart=/usr/local/bin/cloudflared tunnel run simplicity
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable cloudflared
sudo systemctl start cloudflared
sudo systemctl status cloudflared
```

### Option 3: Build Standalone Uberjar (No Docker)

For simpler deployments without Docker:

#### 3.1. Build Uberjar

```bash
# On development machine
clojure -T:build uberjar

# This creates: target/simplicity-standalone.jar
```

#### 3.2. Deploy to VPS

```bash
# Copy jar to VPS
scp target/simplicity-standalone.jar user@vps-ip:/opt/simplicity/

# SSH to VPS
ssh user@vps-ip

# Install Java 21
sudo apt update
sudo apt install openjdk-21-jre-headless

# Run application
cd /opt/simplicity
java -jar simplicity-standalone.jar
```

#### 3.3. Create systemd Service

Create `/etc/systemd/system/simplicity.service`:

```ini
[Unit]
Description=Simplicity Game of Life
After=network.target

[Service]
Type=simple
User=simplicity
WorkingDirectory=/opt/simplicity
Environment="PORT=3000"
Environment="DB_PATH=/opt/simplicity/data/simplicity.db"
Environment="LOG_PATH=/opt/simplicity/logs"
Environment="LOG_LEVEL=WARN"
ExecStart=/usr/bin/java -Xmx512m -jar /opt/simplicity/simplicity-standalone.jar
Restart=on-failure
RestartSec=10s

[Install]
WantedBy=multi-user.target
```

```bash
# Create directories
sudo mkdir -p /opt/simplicity/data /opt/simplicity/logs
sudo useradd -r -s /bin/false simplicity
sudo chown -R simplicity:simplicity /opt/simplicity

# Enable and start
sudo systemctl daemon-reload
sudo systemctl enable simplicity
sudo systemctl start simplicity
sudo systemctl status simplicity
```

Then follow Option 1.3-1.5 for Cloudflare configuration.

## Cloudflare Optimization

### Caching Rules

1. Go to **Rules** → **Page Rules**
2. Create rules:

```
# Cache static assets aggressively
URL: app.yourdomain.com/public/*
Settings:
  - Cache Level: Cache Everything
  - Edge Cache TTL: 1 month
  - Browser Cache TTL: 1 week

# Don't cache API/auth endpoints
URL: app.yourdomain.com/api/*
Settings:
  - Cache Level: Bypass
  
URL: app.yourdomain.com/login
Settings:
  - Cache Level: Bypass
```

### Speed Optimization

1. **Auto Minify**: Enable for HTML, CSS, JS
2. **Brotli**: Enable compression
3. **HTTP/3**: Enable QUIC
4. **Early Hints**: Enable for faster loading
5. **Rocket Loader**: Disable (conflicts with game's audio context)

### Security Headers

Cloudflare automatically adds some headers, but you can enhance with **Transform Rules**:

1. Go to **Rules** → **Transform Rules** → **Modify Response Header**
2. Add rules:

```
# Already handled by application:
# - Content-Security-Policy
# - X-Frame-Options
# - X-Content-Type-Options
# - X-XSS-Protection

# Add Cloudflare-specific headers:
Permissions-Policy: interest-cohort=()
Cross-Origin-Embedder-Policy: require-corp
Cross-Origin-Opener-Policy: same-origin
```

## Monitoring & Analytics

### Automated Health Monitoring

Use the built-in health monitoring script with multi-channel alerting:

```bash
# On VPS
cd /opt/simplicity

# Continuous monitoring with Slack alerts
ALERT_SLACK_WEBHOOK=https://hooks.slack.com/... \
  ./scripts/health-monitor.sh monitor

# Or as systemd service (recommended)
sudo systemctl enable simplicity-monitor
sudo systemctl start simplicity-monitor

# One-shot health check (for cron)
HEALTH_URL=https://app.example.com/health \
ALERT_EMAIL=admin@example.com \
  ./scripts/health-monitor.sh check
```

**Features**:
- ✅ Automatic retries with exponential backoff
- ✅ Multi-channel alerts (Email, Slack, Discord, Webhook)
- ✅ Prometheus-compatible metrics export
- ✅ State tracking (alerts only on state change)

**Alert Channels** (via environment variables):
- `ALERT_EMAIL` - Email notifications
- `ALERT_SLACK_WEBHOOK` - Slack incoming webhook
- `ALERT_DISCORD_WEBHOOK` - Discord webhook
- `ALERT_CUSTOM_WEBHOOK` - Custom JSON webhook

**Prometheus Integration**:
```bash
# Expose metrics for Prometheus scraping
./scripts/health-monitor.sh metrics

# Output:
# simplicity_health_status 1
# simplicity_response_time_seconds 0.123
```

### Cloudflare Analytics

1. **Traffic Analytics**: View requests, bandwidth, threats blocked
2. **Performance**: Monitor response times, cache hit ratio
3. **Security Events**: Track blocked requests, bot traffic

### Application Monitoring

1. **Health Checks**: Use `/health` endpoint
2. **Logs**: View application logs:
   ```bash
   docker logs -f simplicity
   # or
   tail -f /opt/simplicity/logs/simplicity.log
   ```
3. **Metrics**: Monitor:
   - Response times
   - Error rates (5xx responses)
   - Database performance
   - Memory usage

### Alerting

**Cloudflare Notifications:**
1. Go to **Notifications**
2. Enable alerts for:
   - DDoS attacks
   - SSL certificate expiration
   - High error rates
   - Origin unreachable

**Application Health (Automated)**:
See "Automated Health Monitoring" section above for multi-channel alerting.

## Scaling Considerations

### Horizontal Scaling

The application is currently single-instance. For horizontal scaling:

1. **Database**: Migrate from SQLite to PostgreSQL
2. **Sessions**: Use Redis for distributed sessions
3. **Rate Limiting**: Use Redis-backed rate limiting
4. **Load Balancer**: Use Cloudflare Load Balancing

### Vertical Scaling

Adjust container resources:

```bash
# Increase memory
docker run -d \
  --name simplicity \
  -p 3000:3000 \
  -e JAVA_OPTS="-Xmx1024m -Xms512m" \
  simplicity:latest
```

## Costs

**Cloudflare Free Tier Includes:**
- ✅ Unlimited DDoS protection
- ✅ Global CDN
- ✅ Free SSL certificates
- ✅ Basic WAF rules
- ✅ DNS management

**Paid Features (Optional):**
- Cloudflare Pro ($20/month): Advanced analytics, WAF, Image optimization
- Load Balancing ($5/month + $0.50 per 500k requests)
- Argo Smart Routing ($5/month + $0.10/GB)

**VPS Costs:**
- DigitalOcean: $6-12/month (1-2GB RAM)
- Hetzner: €4-8/month (2-4GB RAM)
- Linode: $5-10/month (1-2GB RAM)

## Troubleshooting

### Application Not Accessible

```bash
# Check if container is running
docker ps

# Check container logs
docker logs simplicity

# Check if port is listening
netstat -tlnp | grep 3000

# Test from VPS
curl http://localhost:3000/health
```

### Cloudflare 522 Error (Origin Unreachable)

1. Verify VPS is running: `ping your-vps-ip`
2. Check firewall allows port 3000
3. Verify Cloudflare IP ranges are not blocked
4. Check application health: `docker logs simplicity`

### Cloudflare 524 Error (Timeout)

1. Check application performance
2. Increase Cloudflare timeout (Pro plan)
3. Optimize database queries
4. Add application-level caching

### SSL Certificate Errors

1. Ensure SSL mode is **Full (strict)**
2. Verify origin certificate is valid
3. Check that HSTS is not causing issues

## Backup & Recovery

### Database Backup

```bash
# Automated daily backup
cat > /opt/simplicity/backup.sh <<'EOF'
#!/bin/bash
BACKUP_DIR=/opt/simplicity/backups
mkdir -p $BACKUP_DIR
DATE=$(date +%Y%m%d)
docker cp simplicity:/app/data/simplicity.db $BACKUP_DIR/simplicity-$DATE.db
# Keep last 30 days
find $BACKUP_DIR -name "simplicity-*.db" -mtime +30 -delete
EOF

chmod +x /opt/simplicity/backup.sh

# Add to crontab
echo "0 2 * * * /opt/simplicity/backup.sh" | crontab -
```

### Disaster Recovery

```bash
# Restore from backup
docker cp /opt/simplicity/backups/simplicity-20260122.db simplicity:/app/data/simplicity.db
docker restart simplicity

# Or use zero-downtime rollback
cd /opt/simplicity
./scripts/deploy.sh rollback
```

## Next Steps

After deployment:

1. ✅ Verify `/health` endpoint returns `{"status": "healthy"}`
2. ✅ Test user signup and login
3. ✅ Verify game functionality
4. ✅ Check security headers with https://securityheaders.com
5. ✅ Test SSL configuration with https://ssllabs.com
6. ✅ Monitor logs for errors
7. ✅ Setup automated backups
8. ✅ Configure monitoring and alerts

## Support

For deployment issues:
- Check logs: `docker logs simplicity`
- Review Cloudflare Analytics
- Consult [docs/security.md](./security.md) for security configuration
- GitHub Issues: https://github.com/davidwuchn/simplicity/issues

---

*Last Updated: 2026-01-22*
