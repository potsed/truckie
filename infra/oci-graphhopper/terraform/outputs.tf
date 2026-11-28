output "instance_id" {
  description = "OCID of the GraphHopper VM."
  value       = oci_core_instance.gh.id
}

output "public_ip" {
  description = "Public IPv4 — point your DNS A record here, then set GRAPHHOPPER_BASE_URL=https://<domain>."
  value       = oci_core_instance.gh.public_ip
}

output "ssh_hint" {
  description = "SSH as ubuntu (cloud image) with your private key; password auth is off."
  value       = "ssh -i <private_key> ubuntu@${oci_core_instance.gh.public_ip}"
}

output "graphhopper_base_url_hint" {
  description = "Value for TRUCKIE_GRAPHHOPPER_BASE_URL / -Ptruckie.graphhopper.baseUrl once DNS + TLS are up."
  value       = var.graphhopper_domain != "" ? "https://${var.graphhopper_domain}" : "https://<your-domain>"
}

output "availability_domain" {
  value = local.ad_name
}
