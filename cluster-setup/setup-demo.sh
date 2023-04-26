#!/usr/bin/env bash

# shellcheck disable=SC2034
NEXUS_USERNAME='admin'
NEXUS_PASSWORD='p4ssw0rd1!'



##########
cluster_base_domain=$(oc get route console -n openshift-console -o=jsonpath="{.spec.host}" | cut -c 32-)
nexus_auth_key=$(echo -n "$NEXUS_USERNAME:$NEXUS_PASSWORD" | base64)


## Setup projects
oc new-project airline-demo-ci
oc new-project airline-demo-dev
oc new-project airline-demo-qa
oc new-project airline-demo-prod

## Install operators
oc apply -f operators/openshift-gitops.yaml
oc apply -f operators/openshift-pipelines.yaml

echo "Waiting for operators to finish installing..."
#sleep 60

## Install Monitoring stack
oc apply -f monitoring-setup/monitoring-devs-group.yaml
oc apply -f monitoring-setup/monitoring-devs-rb.yaml
oc apply -f monitoring-setup/cluster-monitoring-config.yaml
oc apply -f monitoring-setup/user-workload-monitoring.yaml


echo "Waiting for monitoring stack to settle..."
#sleep 60

## Setup CI pipeline
oc project airline-demo-ci
oc apply -f pipeline/generate-build-id-task.yaml
oc apply -f pipeline/get-maven-version-task.yaml
oc apply -f pipeline/yq-task.yaml
oc apply -f pipeline/mvn-settings-cm.yaml
oc apply -f pipeline/push-secret.yaml
oc apply -f operators/gitops-sa-crb.yaml
oc apply -f pipeline/tekton-pipeline.yaml

oc create secret generic git-ssh \
    --from-file=config=pipeline/gitconfig \
    --from-file=id_rsa=pipeline/id_rsa

## Setup ArgoCD apps

oc adm policy add-role-to-user edit system:serviceaccount:openshift-gitops:openshift-gitops-argocd-application-controller -n airline-demo-dev
oc adm policy add-role-to-user edit system:serviceaccount:openshift-gitops:openshift-gitops-argocd-application-controller -n airline-demo-qa
oc adm policy add-role-to-user edit system:serviceaccount:openshift-gitops:openshift-gitops-argocd-application-controller -n airline-demo-prod

oc apply -f argo/airline-dev.yaml
oc apply -f argo/airline-qa.yaml
oc apply -f argo/airline-prod.yaml



