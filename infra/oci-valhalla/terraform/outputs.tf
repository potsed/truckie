output "instance_id" {
  description = "OCID of the Valhalla VM."
  value       = oci_core_instance.vh.id
}

output "public_ip" {
  description = "Public IPv4 — point your DNS A record here, then set VALHALLA_BASE_URL=https://<domain>."
  value       = oci_core_instance.vh.public_ip
}

output "ssh_hint" {
  description = "SSH as ubuntu with your private key; password auth is off."
  value       = "ssh -i <private_key> ubuntu@${oci_core_instance.vh.public_ip}"
}

output "install_root" {
  description = "Self-contained stack root on the VM (no Android checkout)."
  value       = "/opt/oci-valhalla"
}

output "redeploy_hint" {
  description = "Idempotent stack redeploy from a laptop (no Gradle)."
  value       = "SSH_KEY=<private_key> ./infra/oci-valhalla/scripts/sync-to-host.sh ubuntu@${oci_core_instance.vh.public_ip}"
}

output "valhalla_base_url_hint" {
  description = "HTTPS base URL for Truckie VALHALLA_BASE_URL (clear VALHALLA_API_KEY)."
  value       = var.valhalla_domain != "" ? "https://${var.valhalla_domain}" : "https://<your-domain>"
}

output "availability_domain" {
  value = local.ad_name
}
