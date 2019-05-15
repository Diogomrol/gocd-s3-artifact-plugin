#!/bin/bash

set -Eeuo pipefail

GOCD_ENDPOINT="http://gocd:8153/go"

KUDU_SERVICE=gocd-s3-artifact-plugin-test

function tf_ops {
  operation=$1

  cd e2e/terraform/
  terraform init -backend-config key=kudu-${KUDU_SERVICE}/terraform.tfstate
  terraform get # modules
  if [[ "${operation}" == "create" ]]; then
    terraform plan -out="kudu_deployment.tfplan"
  elif [[ "${operation}" == "destroy" ]]; then
    terraform plan -out="kudu_deployment.tfplan" -destroy
  fi
  terraform apply kudu_deployment.tfplan

  terraform output -json > tf-out.json
}

command="$1"
case "${command}" in
  _tf_apply)
      tf_ops "create"
      ;;
  tf_apply)
      if [ -z "${AWS_ACCESS_KEY_ID}" ] || [ -z "${AWS_SECRET_ACCESS_KEY}" ] || [ -z "${TF_VAR_public_dns_secret_access_key}" ]; then
        echo "AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and TF_VAR_public_dns_secret_access_key must be set"
        exit 1
      fi
      dojo -c e2e/terraform/Dojofile "./tasks _tf_apply"
      ;;
  wait_online)
    sleep 10
    for i in {1..200}; do
      HTTP_RESPONSE=$(curl --silent --write-out "HTTPSTATUS:%{http_code}" \
        "${GOCD_ENDPOINT}/api/v1/health" --insecure \
        -H 'Accept: application/vnd.go.cd.v1+json' \
        -H 'Content-Type: application/json' \
        )
      HTTP_STATUS=$(echo $HTTP_RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
      if [ ! $HTTP_STATUS -eq 200  ]; then
        echo "GoCD is not up yet" >&2
        sleep 1
        continue
      fi
      HTTP_BODY=$(echo $HTTP_RESPONSE | sed -e 's/HTTPSTATUS\:.*//g')
      echo $HTTP_BODY
      exit 0
    done
    ;;
  _setup_gocd)
    ./tasks wait_online
    bash
    ;;
  setup_gocd)
    export GOCDCLI_SERVER_URL=$GOCD_ENDPOINT
    dojo "./tasks _setup_gocd"
    ;;
  *)
      echo "Invalid command: '${command}'"
      exit 1
      ;;
esac
set +e
