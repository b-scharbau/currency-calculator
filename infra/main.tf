terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "aws" {
  region = "ap-northeast-1"
}

locals {
  app_name = "currency-calculator"
  domain   = "currency.bscharbau.com"

  # The shared platform (ALB, ECS cluster, container instance, GitHub OIDC
  # provider) lives in ~/Projects/bscharbau-infra and is consumed here by
  # name/ARN via data sources (see data.tf) — no terraform_remote_state.
  # This root now manages only currency-calculator's own pieces: ECR repo,
  # ECS task definition + service, ALB target group + host-header listener
  # rule, DNS record, DB-password secret, and the GitHub deploy role.
}
