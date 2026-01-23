# Terraform Deployment Guide

This directory contains Infrastructure as Code (IaC) for provisioning Simplicity infrastructure on DigitalOcean with Cloudflare.

## Architecture

- **VPS**: DigitalOcean Droplet (Ubuntu 22.04)
- **CDN/Security**: Cloudflare (DNS, SSL, WAF, Rate Limiting)
- **Storage**: DigitalOcean Volume (persistent data)
- **Monitoring**: Built-in health checks + DigitalOcean monitoring
- **Backups**: Automated daily backups + weekly snapshots

## Prerequisites

1. **Terraform** (>= 1.0)
   ```bash
   brew install terraform  # macOS
   # or download from https://www.terraform.io/downloads
   ```

2. **DigitalOcean Account**
   - Create account: https://www.digitalocean.com/
   - Generate API token: https://cloud.digitalocean.com/account/api/tokens
   - Add SSH key: https://cloud.digitalocean.com/account/security

3. **Cloudflare Account**
   - Add domain to Cloudflare
   - Get Zone ID from dashboard
   - Create API token: https://dash.cloudflare.com/profile/api-tokens
     - Permissions: Zone.DNS, Zone.Page Rules, Zone.Rate Limits

## Setup

### 1. Configure Variables

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars`:

```hcl
do_token      = "dop_v1_xxxxx"
cf_api_token  = "xxxxx"
cf_zone_id    = "xxxxx"
domain        = "app.yourdomain.com"
ssh_keys      = ["aa:bb:cc:dd:..."]
environment   = "production"
```

### 2. Update Cloud-Init

Edit `cloud-init.yml` and replace:
- `OWNER/REPO` with your GitHub repo (e.g., `davidwuchn/simplicity`)
- `USERNAME` and `TOKEN` with your GitHub credentials for pulling Docker images

### 3. Initialize Terraform

```bash
terraform init
```

### 4. Plan Deployment

```bash
terraform plan
```

Review the resources that will be created:
- 1 Droplet ($12/month)
- 1 Volume ($1/month for 10GB)
- 1 Firewall (free)
- Cloudflare records (free)

**Estimated cost**: ~$13-15/month (with backups)

### 5. Deploy

```bash
terraform apply
```

Type `yes` to confirm.

Deployment takes ~5 minutes:
1. Provision droplet (30s)
2. Run cloud-init script (3-4 min)
3. Pull Docker image (1 min)
4. Start application (30s)

### 6. Verify

```bash
# Get droplet IP
terraform output droplet_ip

# SSH to server
terraform output ssh_command | bash

# Check application
curl http://$(terraform output -raw droplet_ip):3000/health

# Check via Cloudflare (after DNS propagates)
curl https://app.yourdomain.com/health
```

## Post-Deployment

### 1. Configure Cloudflare SSL

1. Go to **SSL/TLS** → **Overview**
2. Set encryption mode to **Full (strict)**
3. Go to **SSL/TLS** → **Edge Certificates**
4. Enable:
   - ✅ Always Use HTTPS
   - ✅ Automatic HTTPS Rewrites
   - ✅ Minimum TLS Version: TLS 1.2

### 2. Enable HSTS

Once HTTPS is verified working:

```bash
ssh root@$(terraform output -raw droplet_ip)
cd /opt/simplicity
docker stop simplicity
docker rm simplicity
# Edit docker-compose.yml, set ENABLE_HSTS=true
docker-compose up -d
```

### 3. Configure Monitoring Alerts

Set up email/Slack alerts:

```bash
ssh root@$(terraform output -raw droplet_ip)

# Edit /etc/systemd/system/simplicity-monitor.service
# Add environment variables:
# Environment="ALERT_EMAIL=admin@example.com"
# Environment="ALERT_SLACK_WEBHOOK=https://hooks.slack.com/..."

systemctl daemon-reload
systemctl restart simplicity-monitor
```

### 4. Setup GitHub Actions Deployment

Add secrets to your GitHub repository:

- `PRODUCTION_SSH_KEY`: Private SSH key for deployment
- `PRODUCTION_HOST`: Droplet IP (from terraform output)
- `PRODUCTION_USER`: `root`

## Management

### Update Infrastructure

```bash
# Modify main.tf or terraform.tfvars
terraform plan
terraform apply
```

### Scale Droplet

```bash
# Edit terraform.tfvars
droplet_size = "s-2vcpu-4gb"  # $24/month

terraform apply
```

**Note**: Resizing requires a reboot (1-2 min downtime).

### Deploy New Version

```bash
ssh root@$(terraform output -raw droplet_ip)
cd /opt/simplicity
./deploy-wrapper.sh
```

Or use GitHub Actions (automatic on release).

### Check Logs

```bash
ssh root@$(terraform output -raw droplet_ip)

# Application logs
docker logs -f simplicity

# Health monitor logs
tail -f /tmp/simplicity-monitor/monitor.log

# Backup logs
tail -f /var/log/simplicity-backup.log
```

### Manual Backup

```bash
ssh root@$(terraform output -raw droplet_ip)
/opt/simplicity/backup.sh
```

### Restore Backup

```bash
ssh root@$(terraform output -raw droplet_ip)
cd /mnt/simplicity-data/backups
gunzip simplicity-20260122-020000.db.gz
docker cp simplicity-20260122-020000.db simplicity:/app/data/simplicity.db
docker restart simplicity
```

## Disaster Recovery

### Full Server Recovery

If droplet is lost/corrupted:

```bash
# Volume persists independently
terraform destroy -target=digitalocean_droplet.simplicity
terraform apply

# Volume will auto-attach with data intact
```

### Restore from Snapshot

DigitalOcean automatically takes weekly snapshots (if enabled):

```bash
# In DigitalOcean dashboard:
# 1. Go to Images → Snapshots
# 2. Create droplet from snapshot
# 3. Update Terraform state:
terraform import digitalocean_droplet.simplicity NEW_DROPLET_ID
```

## Cost Optimization

### Reduce Costs (~$5/month)

For development/staging:

```hcl
droplet_size      = "s-1vcpu-1gb"    # $6/month (instead of $12)
enable_backups    = false             # Save 20%
enable_monitoring = false             # Optional
```

### Alternative Providers

To use Hetzner (cheaper, EU-based):

1. Replace `digitalocean` provider with `hcloud`
2. Adjust resource names
3. Costs: €3-5/month for similar specs

## Cleanup

```bash
# Destroy all resources
terraform destroy

# Or destroy specific resources
terraform destroy -target=digitalocean_droplet.simplicity
```

**Warning**: This deletes all data unless you have backups!

## Troubleshooting

### Cloud-init Failed

```bash
ssh root@$(terraform output -raw droplet_ip)
cat /var/log/cloud-init-output.log
```

### Application Not Starting

```bash
ssh root@$(terraform output -raw droplet_ip)
docker logs simplicity
systemctl status simplicity-monitor
```

### Cloudflare 522 Error

Check firewall allows Cloudflare IPs:

```bash
ssh root@$(terraform output -raw droplet_ip)
ufw status
# Should allow 80/443 from Cloudflare IPs (set in main.tf)
```

### Volume Not Mounted

```bash
ssh root@$(terraform output -raw droplet_ip)
lsblk
mount | grep simplicity
# Re-run mount command from cloud-init
```

## Security Notes

- ✅ Firewall restricts HTTP/HTTPS to Cloudflare IPs only
- ✅ SSH from anywhere (consider IP whitelisting)
- ✅ Fail2ban enabled (brute force protection)
- ✅ Automatic security updates enabled
- ✅ Non-root user for application (Docker)
- ⚠️  Update GitHub credentials in cloud-init before deployment
- ⚠️  Add `terraform.tfvars` to `.gitignore` (contains secrets)

## Next Steps

1. ✅ Verify health endpoint: `https://app.yourdomain.com/health`
2. ✅ Test user signup and login
3. ✅ Check security headers: https://securityheaders.com
4. ✅ Monitor logs for errors
5. ✅ Setup alerting (email/Slack)
6. ✅ Test backup/restore procedure
7. ✅ Configure GitHub Actions deployment
8. ✅ Document runbook for your team

---

**Support**: See main [deployment-cloudflare.md](../docs/deployment-cloudflare.md) for additional configuration.
