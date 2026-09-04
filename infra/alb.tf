# The ALB and its listeners are shared (~/Projects/bscharbau-infra). This file
# keeps currency-calculator's own target group and the host-header listener rule
# that forwards currency.bscharbau.com to it. The shared HTTPS listener has a
# plain 404 default action; every project registers a rule like this one.

resource "aws_lb_target_group" "app" {
  # name_prefix (not a fixed name) + create_before_destroy so a future target_type / attribute
  # change can roll the group without the "delete before the listener lets go" deadlock.
  name_prefix = "cc-"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.main.id
  target_type = "instance"

  health_check {
    path                = "/"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_lb_listener_rule" "currency" {
  listener_arn = data.aws_lb_listener.https.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }

  condition {
    host_header {
      values = [local.domain]
    }
  }
}
