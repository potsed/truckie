# Sync the self-contained stack (compose/scripts/secrets) onto the VM.
# Uses the operator machine's local infra/oci-graphhopper tree — never the
# Android app modules, never a git clone of the monorepo on the instance.

locals {
  stack_root = abspath("${path.module}/..")
  # Hash tracked stack files so terraform apply re-syncs on content change
  stack_file_list = sort(concat(
    tolist(fileset(local.stack_root, "compose/**")),
    tolist(fileset(local.stack_root, "scripts/**")),
    tolist(fileset(local.stack_root, "secrets/env.example")),
    tolist(fileset(local.stack_root, "cloud-init/**")),
  ))
  stack_hash = sha256(join("", [
    for rel in local.stack_file_list : filesha256("${local.stack_root}/${rel}")
  ]))
}

resource "null_resource" "sync_stack" {
  count = var.enable_stack_sync ? 1 : 0

  triggers = {
    instance_id = oci_core_instance.gh.id
    stack_hash  = local.stack_hash
    domain      = var.graphhopper_domain
    email       = var.letsencrypt_email
  }

  lifecycle {
    precondition {
      condition     = length(var.ssh_private_key_path) > 0
      error_message = "ssh_private_key_path is required when enable_stack_sync is true (or set enable_stack_sync=false and use scripts/sync-to-host.sh)."
    }
  }

  connection {
    type        = "ssh"
    host        = oci_core_instance.gh.public_ip
    user        = "ubuntu"
    private_key = file(var.ssh_private_key_path)
    timeout     = "10m"
  }

  # Wait until cloud-init finished host bootstrap (docker + UFW)
  provisioner "remote-exec" {
    inline = [
      "for i in $(seq 1 60); do test -f /opt/oci-graphhopper/HOST_READY && exit 0; sleep 10; done; echo 'Timed out waiting for HOST_READY'; exit 1",
    ]
  }

  provisioner "remote-exec" {
    inline = [
      "rm -rf /tmp/oci-graphhopper-stack && mkdir -p /tmp/oci-graphhopper-stack",
    ]
  }

  provisioner "file" {
    source      = "${local.stack_root}/compose"
    destination = "/tmp/oci-graphhopper-stack/compose"
  }

  provisioner "file" {
    source      = "${local.stack_root}/scripts"
    destination = "/tmp/oci-graphhopper-stack/scripts"
  }

  provisioner "file" {
    source      = "${local.stack_root}/secrets"
    destination = "/tmp/oci-graphhopper-stack/secrets"
  }

  provisioner "file" {
    source      = "${local.stack_root}/README.md"
    destination = "/tmp/oci-graphhopper-stack/README.md"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod +x /tmp/oci-graphhopper-stack/scripts/*.sh",
      "sudo GRAPHHOPPER_DOMAIN='${var.graphhopper_domain}' LETSENCRYPT_EMAIL='${var.letsencrypt_email}' bash /tmp/oci-graphhopper-stack/scripts/install.sh --root /opt/oci-graphhopper --domain '${var.graphhopper_domain}' --email '${var.letsencrypt_email}'",
    ]
  }
}
