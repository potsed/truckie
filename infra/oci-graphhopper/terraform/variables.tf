variable "tenancy_ocid" {
  type        = string
  description = "OCI tenancy OCID (from Console → Administration → Tenancy Details)."
}

variable "user_ocid" {
  type        = string
  description = "API user OCID (Identity → Users → your user)."
}

variable "fingerprint" {
  type        = string
  description = "API key fingerprint for the uploaded public key."
}

variable "private_key_path" {
  type        = string
  description = "Absolute path to the PEM private key on the operator machine (not in git)."
}

variable "region" {
  type        = string
  description = "OCI region close to AU users (e.g. ap-sydney-1 or ap-melbourne-1)."
  default     = "ap-sydney-1"
}

variable "compartment_ocid" {
  type        = string
  description = "Compartment OCID for Truckie infra (create under root or a dedicated compartment)."
}

variable "ssh_public_key" {
  type        = string
  description = "SSH public key for the ubuntu admin user. Password SSH is disabled."
}

variable "ssh_private_key_path" {
  type        = string
  description = "Path to the matching SSH private key on the operator machine (for stack sync provisioner). Not committed."
  default     = ""
}

variable "enable_stack_sync" {
  type        = bool
  description = "If true, terraform apply pushes infra/oci-graphhopper (compose/scripts only) onto the VM via SSH. Requires ssh_private_key_path. No Android/Gradle."
  default     = true
}

variable "availability_domain_number" {
  type        = number
  description = "AD index (1-based) within the region. Free Tier Ampere is usually AD-1."
  default     = 1
}

variable "display_name" {
  type        = string
  description = "Display name prefix for VCN / instance / NSG."
  default     = "truckie-graphhopper"
}

variable "shape" {
  type        = string
  description = "Always Free Ampere flex shape (preferred for GraphHopper memory)."
  default     = "VM.Standard.A1.Flex"
}

variable "ocpus" {
  type        = number
  description = "OCPUs for A1.Flex (Always Free pool: up to 4 shared)."
  default     = 2
}

variable "memory_in_gbs" {
  type        = number
  description = "Memory GB for A1.Flex (Always Free pool: up to 24 shared). SEQ import needs ≥12."
  default     = 12
}

variable "boot_volume_size_gbs" {
  type        = number
  description = "Boot volume GB. OSM + graph cache need headroom (Always Free block up to 200 GB shared)."
  default     = 100
}

variable "assign_public_ip" {
  type        = bool
  description = "Assign a public IPv4 (required for Let’s Encrypt + phone clients)."
  default     = true
}

variable "allowed_ssh_cidr" {
  type        = string
  description = "CIDR allowed to SSH (never 0.0.0.0/0 in production dogfood if avoidable)."
  default     = "0.0.0.0/0"
}

variable "graphhopper_domain" {
  type        = string
  description = "FQDN for Caddy / Let’s Encrypt (A record → instance public IP)."
  default     = ""
}

variable "letsencrypt_email" {
  type        = string
  description = "Email for Let’s Encrypt registration (passed into cloud-init)."
  default     = ""
}

