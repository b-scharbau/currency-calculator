resource "aws_security_group" "alb" {
  name        = "${local.app_name}-alb"
  description = "Allow inbound HTTP/HTTPS to the ${local.app_name} ALB"
  vpc_id      = data.aws_vpc.main.id

  ingress {
    description = "HTTP from internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS from internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "ecs_instance" {
  name        = "${local.app_name}-ecs-instance"
  description = "Allow inbound from the ALB only, outbound to internet + RDS"
  vpc_id      = data.aws_vpc.main.id

  # bridge networking maps the container's 8080 to an ephemeral host port per task, so the ALB
  # reaches the instance somewhere in the dynamic port range rather than on a fixed 8080.
  ingress {
    description     = "Dynamic host ports from ALB"
    from_port       = 32768
    to_port         = 65535
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    description = "HTTP to internet (package + ECS agent updates)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "HTTPS to internet (Frankfurter API, ECR, CloudWatch Logs, SSM)"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description     = "Postgres to RDS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [data.aws_security_group.rds.id]
  }
}

# Additive rule on the existing shared RDS security group. Not managed as a full resource (see
# data.tf) to avoid touching the pre-existing 0.0.0.0/0:5432 rule other projects depend on.
resource "aws_security_group_rule" "rds_from_ecs_instance" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  security_group_id        = data.aws_security_group.rds.id
  source_security_group_id = aws_security_group.ecs_instance.id
  description              = "Allow ${local.app_name} ECS instance to reach Postgres"
}
