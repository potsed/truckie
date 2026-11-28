# Truckie — OCI Free Tier Valhalla
#
# Modules are flat in this directory (network.tf, compute.tf, outputs.tf).
# Apply from here after copying terraform.tfvars.example → terraform.tfvars.
#
# Credentials: operator-supplied Free Tier API key only. Nothing is invented
# or committed by agents.

locals {
  product = "truckie"
  role    = "valhalla"
  tags = {
    Project = "Truckie"
    Role    = "oss-valhalla"
    Tier    = "select-user"
  }
}
