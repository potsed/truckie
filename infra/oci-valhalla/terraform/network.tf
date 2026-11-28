data "oci_identity_availability_domains" "ads" {
  compartment_id = var.tenancy_ocid
}

locals {
  ad_name = data.oci_identity_availability_domains.ads.availability_domains[var.availability_domain_number - 1].name
}

resource "oci_core_vcn" "vh" {
  compartment_id = var.compartment_ocid
  display_name   = "${var.display_name}-vcn"
  cidr_blocks    = ["10.43.0.0/16"]
  dns_label      = "truckievh"
}

resource "oci_core_internet_gateway" "vh" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.vh.id
  display_name   = "${var.display_name}-igw"
  enabled        = true
}

resource "oci_core_route_table" "vh_public" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.vh.id
  display_name   = "${var.display_name}-public-rt"

  route_rules {
    network_entity_id = oci_core_internet_gateway.vh.id
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
  }
}

resource "oci_core_security_list" "vh_default" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.vh.id
  display_name   = "${var.display_name}-sl"

  # Egress: allow all (updates, OSM download, Let’s Encrypt).
  egress_security_rules {
    protocol    = "all"
    destination = "0.0.0.0/0"
  }

  # SSH — prefer tightening allowed_ssh_cidr to your IP.
  ingress_security_rules {
    protocol = "6"
    source   = var.allowed_ssh_cidr
    tcp_options {
      min = 22
      max = 22
    }
  }

  # HTTPS only publicly (Caddy terminates TLS → Valhalla on loopback).
  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 443
      max = 443
    }
  }

  # HTTP for ACME HTTP-01 + redirect to HTTPS.
  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 80
      max = 80
    }
  }
}

resource "oci_core_subnet" "vh_public" {
  compartment_id             = var.compartment_ocid
  vcn_id                     = oci_core_vcn.vh.id
  display_name               = "${var.display_name}-public"
  cidr_block                 = "10.43.1.0/24"
  route_table_id             = oci_core_route_table.vh_public.id
  security_list_ids          = [oci_core_security_list.vh_default.id]
  prohibit_public_ip_on_vnic = false
  dns_label                  = "public"
}
