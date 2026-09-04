data "aws_caller_identity" "current" {}

data "aws_vpc" "main" {
  id = "vpc-030d2f085ef6deb3a"
}

data "aws_route53_zone" "bscharbau" {
  name         = "bscharbau.com."
  private_zone = false
}

data "aws_db_instance" "shared" {
  db_instance_identifier = "bscharbau-com"
}

# ── Shared platform (managed in ~/Projects/bscharbau-infra) ───────────────────
# Looked up by name/ARN rather than terraform_remote_state — same pattern the
# shared VPC/RDS/zone above were always consumed with.

data "aws_lb" "shared" {
  name = "currency-calculator" # historical name of the shared ALB; see bscharbau-infra/infra/main.tf
}

data "aws_lb_listener" "https" {
  load_balancer_arn = data.aws_lb.shared.arn
  port              = 443
}

data "aws_ecs_cluster" "shared" {
  cluster_name = "currency-calculator" # historical name of the shared cluster
}

data "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"
}
