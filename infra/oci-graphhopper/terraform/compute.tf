data "oci_core_images" "ubuntu_aarch64" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Canonical Ubuntu"
  operating_system_version = "22.04"
  shape                    = var.shape
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

locals {
  cloud_init = templatefile("${path.module}/../cloud-init/user-data.yaml", {
    graphhopper_domain = var.graphhopper_domain
    letsencrypt_email  = var.letsencrypt_email
    gh_repo_ref        = var.gh_repo_ref
  })
}

resource "oci_core_instance" "gh" {
  compartment_id      = var.compartment_ocid
  availability_domain = local.ad_name
  display_name        = var.display_name
  shape               = var.shape

  shape_config {
    ocpus         = var.ocpus
    memory_in_gbs = var.memory_in_gbs
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.gh_public.id
    assign_public_ip = var.assign_public_ip
    display_name     = "${var.display_name}-vnic"
    hostname_label   = "graphhopper"
  }

  source_details {
    source_type             = "image"
    source_id               = data.oci_core_images.ubuntu_aarch64.images[0].id
    boot_volume_size_in_gbs = var.boot_volume_size_gbs
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    user_data           = base64encode(local.cloud_init)
  }

  agent_config {
    is_management_disabled = false
    is_monitoring_disabled = false
  }

  # Idempotent redeploy: recreate when cloud-init template changes.
  lifecycle {
    create_before_destroy = false
  }
}
