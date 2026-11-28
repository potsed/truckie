output "instance_id" {
  description = "OCID of the GraphHopper VM."
  value       = oci_core_instance.gh.id
}

output "public_ip" {
  description = "Public IPv4 — point your DNS A record here, then set GRAPHHOPPER_BASE_URL=https://<domain>."
  value       = oci_core_instance.gh.public_ip
}

output "ssh_hint" {
  description = "SSH as ubuntu with your private key; password auth is off."
  value       = "ssh -i <private_key> ubuntu@${oci_core_instance.gh.public_ip}"
}

output "install_root" {
  description = "Self-contained stack root on the VM (no Android checkout)."
  value       = "/opt/oci-graphhopper"
}

output "redeploy_hint" {
  description = "Idempotent stack redeploy from a laptop (no Gradle)."
  value       = "SSH_KEY=<private_key> ./infra/oci-graphhopper/scripts/sync-to-host.sh ubuntu@${oci_core_instance.gh.public_ip}"
}

output "graphhopper_base_url_hint" {
  description = "HTTPS base URL for clients (Truckie GRAPHHOPPER_BASE_URL is optional/later)."
  value       = var.graphhopper_domain != "" ? "https://${var.graphhopper_domain}" : "https://<your-domain>"
}

output "availability_domain" {
  value = local.ad_name
}
