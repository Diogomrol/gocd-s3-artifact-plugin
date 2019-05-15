
variable "suffix" {
  default = "dev"
}

# Setup our aws provider
provider "aws" {
  region = "eu-west-1"
}

terraform {
  backend "s3" {
    bucket = "kudu-terraform-infra"
    region = "eu-west-1"
    dynamodb_table = "kudu-terraform-locks"
  }
}

resource "aws_s3_bucket" "gocd_artifact_test" {
  bucket = "gocd-s3-artifact-test-${var.suffix}"
  acl    = "private"

  tags = {
    Name        = "GoCD S3 plugin test bucket ${var.suffix}"
    createdBy   = "gocd-s3-artifact-plugin"
  }
}

resource "aws_iam_user" "test_user" {
  name = "gocd-s3-artifact-test-${var.suffix}"
  path = "/"
  tags = {
    Name        = "GoCD S3 plugin test user ${var.suffix}"
    createdBy   = "gocd-s3-artifact-plugin"
  }
}

resource "aws_iam_access_key" "test_user" {
  user = "${aws_iam_user.test_user.name}"
}

resource "aws_iam_user_policy" "test_user_policy" {
  name = "gocd-s3-artifact-test-${var.suffix}"
  user = "${aws_iam_user.test_user.name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
     "Resource": [
        "${aws_s3_bucket.gocd_artifact_test.arn}"
      ]
    },
    {
      "Action": [
        "s3:DeleteObject",
        "s3:GetObject",
        "s3:HeadBucket",
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": "${aws_s3_bucket.gocd_artifact_test.arn}/*"
    }
  ]
}
EOF
}

output "s3_key_id" {
  value = "${aws_iam_access_key.test_user.id}"
}
output "s3_key" {
  value = "${aws_iam_access_key.test_user.secret}"
}
output "s3_bucket" {
  value = "${aws_s3_bucket.gocd_artifact_test.bucket}"
}
