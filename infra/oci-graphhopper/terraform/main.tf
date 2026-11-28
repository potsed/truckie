# Truckie — OCI Free Tier GraphHopper
#
# Modules are flat in this directory (network.tf, compute.tf, outputs.tf).
# Apply from here after copying terraform.tfvars.example → terraform.tfvars.
#
# Credentials: operator-supplied Free Tier API key only. Nothing is invented
# or committed by agents.

locals {
  product = "truckie"
  role    = "graphhopper"
  tags = {
    Project = "Truckie"
    Role    = "oss-graphhopper"
    Tier    = "select-user"
  }
}
