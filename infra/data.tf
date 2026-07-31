data "aws_vpc" "main" {
  id = "vpc-030d2f085ef6deb3a"
}

data "aws_subnets" "main" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.main.id]
  }
}

# The default VPC has two subnets per AZ, but they are NOT equivalent: three are associated with
# a "bscharbau-com-private" route table with no internet route at all, and the other three fall
# back to the VPC's main route table ("bscharbau-com-public"), which does have the 0.0.0.0/0 -> IGW
# route. Both the internet-facing ALB and the ECS task (which needs outbound access to ECR,
# CloudWatch Logs, SSM, and the Frankfurter API) must land only in the public ones.
data "aws_route_table" "private" {
  filter {
    name   = "tag:Name"
    values = ["bscharbau-com-private"]
  }
}

locals {
  private_subnet_ids = [for a in data.aws_route_table.private.associations : a.subnet_id]
  public_subnet_ids  = [for id in data.aws_subnets.main.ids : id if !contains(local.private_subnet_ids, id)]
}

data "aws_route53_zone" "bscharbau" {
  name         = "bscharbau.com."
  private_zone = false
}

# Existing shared RDS security group ("PostgreSQL Database Access"). Looked up rather than
# managed as a full aws_security_group resource — that would be authoritative and could delete
# the existing 0.0.0.0/0:5432 rule other personal projects rely on. We only add an additive rule
# to it in security_groups.tf.
data "aws_security_group" "rds" {
  id = "sg-0f4514dfc7e7b3479"
}

data "aws_caller_identity" "current" {}

data "aws_db_instance" "shared" {
  db_instance_identifier = "bscharbau-com"
}
