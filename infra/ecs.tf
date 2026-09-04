# The cluster, the EC2 container instance, and the container-instance IAM role
# are shared and live in ~/Projects/bscharbau-infra. This file keeps only
# currency-calculator's own task execution role, task definition, service, and
# log group.

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${local.app_name}"
  retention_in_days = 30
}

# --- Task execution role -----------------------------------------------------------------------
# Used by the ECS agent to pull the image and read the SSM SecureString secret when starting a
# task. Per-app (the container-instance role is the shared one).

resource "aws_iam_role" "execution" {
  name = "${local.app_name}-ecs-execution"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "execution_managed" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# The managed policy above covers ECR pull + CloudWatch Logs, but not reading SSM SecureString
# parameters or decrypting them — without this, task startup fails with
# "ResourceInitializationError: unable to pull secrets".
resource "aws_iam_role_policy" "execution_secrets" {
  name = "${local.app_name}-secrets-access"
  role = aws_iam_role.execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ssm:GetParameters"]
        Resource = [aws_ssm_parameter.db_password.arn]
      },
      {
        Effect   = "Allow"
        Action   = ["kms:Decrypt"]
        Resource = ["arn:aws:kms:ap-northeast-1:${data.aws_caller_identity.current.account_id}:alias/aws/ssm"]
      }
    ]
  })
}

# --- Task definition + service ---------------------------------------------------------------------

resource "aws_ecs_task_definition" "app" {
  family                   = local.app_name
  requires_compatibilities = ["EC2"]
  # bridge networking (not awsvpc): the task shares the instance's ENI, so it reaches the
  # internet via the instance's public IP and the ALB targets the instance on a dynamic host
  # port. awsvpc on EC2 would give the task its own ENI with no public IP and no route out.
  network_mode       = "bridge"
  execution_role_arn = aws_iam_role.execution.arn

  container_definitions = jsonencode([
    {
      name              = local.app_name
      image             = "${aws_ecr_repository.app.repository_url}:latest"
      essential         = true
      memoryReservation = 512
      portMappings = [
        # hostPort 0 => ECS assigns an ephemeral host port (32768-65535); the ALB target group
        # is registered with that port automatically.
        { containerPort = 8080, hostPort = 0, protocol = "tcp" }
      ]
      environment = [
        { name = "DB_URL", value = "jdbc:postgresql://${data.aws_db_instance.shared.endpoint}/currency_calculator" },
        { name = "DB_USERNAME", value = "currency_prod" }
      ]
      secrets = [
        { name = "DB_PASSWORD", valueFrom = aws_ssm_parameter.db_password.arn }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.app.name
          "awslogs-region"        = "ap-northeast-1"
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "app" {
  name            = local.app_name
  cluster         = data.aws_ecs_cluster.shared.arn
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "EC2"

  # Free up the single instance's host port before starting the replacement task on a deploy.
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = local.app_name
    container_port   = 8080
  }

  depends_on = [aws_lb_listener_rule.currency]
}
