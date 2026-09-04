resource "aws_route53_record" "app" {
  zone_id = data.aws_route53_zone.bscharbau.zone_id
  name    = local.domain
  type    = "A"

  alias {
    name                   = data.aws_lb.shared.dns_name
    zone_id                = data.aws_lb.shared.zone_id
    evaluate_target_health = true
  }
}
