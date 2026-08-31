resource "aws_ecs_cluster" "app" {
  name = local.app_name
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${local.app_name}"
  retention_in_days = 30
}

# --- Task execution role -------------------------------------------------------------------------
# Used by the ECS agent to pull the image and read the SSM SecureString secret when starting a
# task. Distinct from the EC2 container-instance role below (which registers the box with the
# cluster and ships container logs).

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

# --- EC2 container instances -------------------------------------------------------------------
# A single t3.micro registered with the cluster via an Auto Scaling Group. Cheaper than Fargate
# for an always-on service this small: one on-demand t3.micro (~US$8/mo in ap-northeast-1, less
# with a Savings Plan) vs. Fargate's 0.5 vCPU + 1GB running 24/7 (~US$18/mo).

resource "aws_iam_role" "ecs_instance" {
  name = "${local.app_name}-ecs-instance"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# Lets the box register with the cluster, pull images, and ship container stdout/stderr to
# CloudWatch Logs (the awslogs driver uses the instance role on EC2, not the execution role).
resource "aws_iam_role_policy_attachment" "ecs_instance_ecs" {
  role       = aws_iam_role.ecs_instance.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

# SSM Session Manager access, so the instance can be reached for debugging without opening SSH
# or attaching a key pair.
resource "aws_iam_role_policy_attachment" "ecs_instance_ssm" {
  role       = aws_iam_role.ecs_instance.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ecs_instance" {
  name = "${local.app_name}-ecs-instance"
  role = aws_iam_role.ecs_instance.name
}

resource "aws_launch_template" "ecs" {
  name_prefix   = "${local.app_name}-ecs-"
  image_id      = data.aws_ssm_parameter.ecs_ami.value
  instance_type = "t3.micro"

  iam_instance_profile {
    arn = aws_iam_instance_profile.ecs_instance.arn
  }

  # Public subnet + public IP: the instance needs outbound access to ECR, CloudWatch Logs, SSM
  # and the Frankfurter API, and there is no NAT gateway in this VPC.
  network_interfaces {
    associate_public_ip_address = true
    security_groups             = [aws_security_group.ecs_instance.id]
  }

  user_data = base64encode(<<-EOF
    #!/bin/bash
    echo "ECS_CLUSTER=${aws_ecs_cluster.app.name}" >> /etc/ecs/ecs.config
  EOF
  )

  tag_specifications {
    resource_type = "instance"
    tags          = { Name = "${local.app_name}-ecs" }
  }
}

resource "aws_autoscaling_group" "ecs" {
  name                = "${local.app_name}-ecs"
  vpc_zone_identifier = local.public_subnet_ids
  min_size            = 1
  max_size            = 1
  desired_capacity    = 1
  health_check_type   = "EC2"

  launch_template {
    id      = aws_launch_template.ecs.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "${local.app_name}-ecs"
    propagate_at_launch = true
  }

  # Replace the instance on launch-template changes (e.g. a newer ECS-optimized AMI) rather than
  # leaving the running box on the old template.
  instance_refresh {
    strategy = "Rolling"
  }
}

# --- Task definition + service --------------------------------------------------------------------

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
      name      = local.app_name
      image     = "${aws_ecr_repository.app.repository_url}:latest"
      essential = true
      # Soft limit only — one task owns the box, so let it use whatever RAM is free rather than
      # risking an OOM kill at a hard cap on a 1GB instance.
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
  cluster         = aws_ecs_cluster.app.id
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

  depends_on = [aws_lb_listener.https, aws_autoscaling_group.ecs]
}
