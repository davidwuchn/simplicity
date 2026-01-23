# Simplicity Infrastructure as Code
# Terraform configuration for VPS deployment

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    digitalocean = {
      source  = "digitalocean/digitalocean"
      version = "~> 2.0"
    }
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 4.0"
    }
  }
  
  # Uncomment for remote state (recommended for production)
  # backend "s3" {
  #   bucket = "your-terraform-state-bucket"
  #   key    = "simplicity/terraform.tfstate"
  #   region = "us-east-1"
  # }
}

# Variables
variable "do_token" {
  description = "DigitalOcean API token"
  type        = string
  sensitive   = true
}

variable "cf_api_token" {
  description = "Cloudflare API token"
  type        = string
  sensitive   = true
}

variable "cf_zone_id" {
  description = "Cloudflare Zone ID"
  type        = string
}

variable "domain" {
  description = "Domain name for the application"
  type        = string
  default     = "app.example.com"
}

variable "ssh_keys" {
  description = "SSH key fingerprints for droplet access"
  type        = list(string)
}

variable "environment" {
  description = "Environment name (staging, production)"
  type        = string
  default     = "production"
}

variable "droplet_size" {
  description = "DigitalOcean droplet size"
  type        = string
  default     = "s-1vcpu-2gb"  # $12/month
}

variable "region" {
  description = "DigitalOcean region"
  type        = string
  default     = "nyc3"
}

variable "enable_monitoring" {
  description = "Enable DigitalOcean monitoring"
  type        = bool
  default     = true
}

variable "enable_backups" {
  description = "Enable weekly backups (20% additional cost)"
  type        = bool
  default     = true
}

# Providers
provider "digitalocean" {
  token = var.do_token
}

provider "cloudflare" {
  api_token = var.cf_api_token
}

# Data sources
data "digitalocean_image" "ubuntu" {
  slug = "ubuntu-22-04-x64"
}

# Firewall
resource "digitalocean_firewall" "simplicity" {
  name = "simplicity-${var.environment}"
  
  droplet_ids = [digitalocean_droplet.simplicity.id]
  
  # SSH (from anywhere - consider restricting to specific IPs)
  inbound_rule {
    protocol         = "tcp"
    port_range       = "22"
    source_addresses = ["0.0.0.0/0", "::/0"]
  }
  
  # HTTP/HTTPS (only from Cloudflare)
  inbound_rule {
    protocol         = "tcp"
    port_range       = "80"
    source_addresses = [
      # Cloudflare IPv4 ranges (subset - see https://www.cloudflare.com/ips/)
      "173.245.48.0/20",
      "103.21.244.0/22",
      "103.22.200.0/22",
      "103.31.4.0/22",
      "141.101.64.0/18",
      "108.162.192.0/18",
      "190.93.240.0/20",
      "188.114.96.0/20",
      "197.234.240.0/22",
      "198.41.128.0/17",
      "162.158.0.0/15",
      "104.16.0.0/13",
      "104.24.0.0/14",
      "172.64.0.0/13",
      "131.0.72.0/22"
    ]
  }
  
  inbound_rule {
    protocol         = "tcp"
    port_range       = "443"
    source_addresses = [
      "173.245.48.0/20",
      "103.21.244.0/22",
      "103.22.200.0/22",
      "103.31.4.0/22",
      "141.101.64.0/18",
      "108.162.192.0/18",
      "190.93.240.0/20",
      "188.114.96.0/20",
      "197.234.240.0/22",
      "198.41.128.0/17",
      "162.158.0.0/15",
      "104.16.0.0/13",
      "104.24.0.0/14",
      "172.64.0.0/13",
      "131.0.72.0/22"
    ]
  }
  
  # Application port (internal)
  inbound_rule {
    protocol         = "tcp"
    port_range       = "3000"
    source_addresses = ["10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"]
  }
  
  # Outbound (allow all)
  outbound_rule {
    protocol              = "tcp"
    port_range            = "1-65535"
    destination_addresses = ["0.0.0.0/0", "::/0"]
  }
  
  outbound_rule {
    protocol              = "udp"
    port_range            = "1-65535"
    destination_addresses = ["0.0.0.0/0", "::/0"]
  }
  
  outbound_rule {
    protocol              = "icmp"
    destination_addresses = ["0.0.0.0/0", "::/0"]
  }
}

# Droplet
resource "digitalocean_droplet" "simplicity" {
  image    = data.digitalocean_image.ubuntu.id
  name     = "simplicity-${var.environment}"
  region   = var.region
  size     = var.droplet_size
  ssh_keys = var.ssh_keys
  
  monitoring = var.enable_monitoring
  backups    = var.enable_backups
  
  user_data = templatefile("${path.module}/cloud-init.yml", {
    environment = var.environment
    domain      = var.domain
  })
  
  tags = [
    "simplicity",
    "environment:${var.environment}",
    "managed-by:terraform"
  ]
}

# Volume for persistent data
resource "digitalocean_volume" "simplicity_data" {
  region                  = var.region
  name                    = "simplicity-data-${var.environment}"
  size                    = 10  # GB
  initial_filesystem_type = "ext4"
  description             = "Simplicity persistent data (database, logs)"
  
  tags = [
    "simplicity",
    "environment:${var.environment}"
  ]
}

resource "digitalocean_volume_attachment" "simplicity_data" {
  droplet_id = digitalocean_droplet.simplicity.id
  volume_id  = digitalocean_volume.simplicity_data.id
}

# Cloudflare DNS
resource "cloudflare_record" "simplicity" {
  zone_id = var.cf_zone_id
  name    = var.domain
  value   = digitalocean_droplet.simplicity.ipv4_address
  type    = "A"
  ttl     = 1  # Auto
  proxied = true
}

# Cloudflare Page Rule (caching)
resource "cloudflare_page_rule" "cache_static" {
  zone_id  = var.cf_zone_id
  target   = "${var.domain}/public/*"
  priority = 1
  
  actions {
    cache_level = "cache_everything"
    edge_cache_ttl = 2592000  # 30 days
    browser_cache_ttl = 604800  # 7 days
  }
}

resource "cloudflare_page_rule" "bypass_api" {
  zone_id  = var.cf_zone_id
  target   = "${var.domain}/api/*"
  priority = 2
  
  actions {
    cache_level = "bypass"
  }
}

# Cloudflare WAF Rule (rate limiting)
resource "cloudflare_rate_limit" "login" {
  zone_id   = var.cf_zone_id
  threshold = 5
  period    = 60
  
  match {
    request {
      url_pattern = "${var.domain}/login"
      schemes      = ["HTTPS", "HTTP"]
      methods      = ["POST"]
    }
  }
  
  action {
    mode    = "challenge"
    timeout = 300
  }
  
  description = "Rate limit login attempts"
}

# Outputs
output "droplet_ip" {
  description = "Droplet public IP address"
  value       = digitalocean_droplet.simplicity.ipv4_address
}

output "droplet_id" {
  description = "Droplet ID"
  value       = digitalocean_droplet.simplicity.id
}

output "volume_id" {
  description = "Volume ID"
  value       = digitalocean_volume.simplicity_data.id
}

output "domain" {
  description = "Application domain"
  value       = var.domain
}

output "cloudflare_dns_id" {
  description = "Cloudflare DNS record ID"
  value       = cloudflare_record.simplicity.id
}

output "ssh_command" {
  description = "SSH command to connect to droplet"
  value       = "ssh root@${digitalocean_droplet.simplicity.ipv4_address}"
}
