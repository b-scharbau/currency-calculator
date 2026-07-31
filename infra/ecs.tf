resource "aws_ecs_cluster" "app" {
  name = local.app_name
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${local.app_name}"
  retention_in_days = 30
}

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

resource "aws_ecs_task_definition" "app" {
  family                   = local.app_name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.execution.arn

  container_definitions = jsonencode([
    {
      name      = local.app_name
      image     = "${aws_ecr_repository.app.repository_url}:latest"
      essential = true
      portMappings = [
        { containerPort = 8080, protocol = "tcp" }
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
  cluster         = aws_ecs_cluster.app.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = local.public_subnet_ids
    security_groups  = [aws_security_group.ecs_task.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = local.app_name
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.https]
}
