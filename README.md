# Kogito Openshift Demo

## Description
This project contains a simple DMN service to evaluate a traffic violation to demonstrate
Kogito and OpenShift capabilities, including REST interface code generation and Promtheus metrics.

## Installing and Running
### Prerequisites
You will need:
- Java 11 installed locally
- Maven 3.8.1 or higher
- OpenShift 4.x:
    - OpenShift Pipelines operator installed
    - `cluster-admin` permissions

## Running Locally
To build the project locally, navigate to the root of the project and run the command

`mvn clean compile quarkus:dev`

The server will then start at `localhost:8080`

## Testing

You can test the server locally by running the command

```shell
curl --location 'http://localhost:8080/Traffic%20Violation' \
     --header 'Accept: application/json' \
     --header 'Content-Type: application/json' \
     --data '{"Driver":{"Points":2},"Violation":{"Type":"speed","Actual Speed":120,"Speed Limit":100}}'
```

This should return a result:
```json
{
  "Violation": {
    "Type": "speed",
    "Speed Limit": 100,
    "Actual Speed": 120,
    "Code": null,
    "Date": null
  },
  "Driver": {
    "Points": 2,
    "State": null,
    "City": null,
    "Age": null,
    "Name": null
  },
  "__timerKey": "ac2bb015-3951-49e7-8bf4-98f6c4c9ea7d",
  "Fine": {
    "Points": 3,
    "Amount": 500
  },
  "Should the driver be suspended?": "No"
}
```

## Viewing Swagger Documentation
This project will automatically generate Swagger API docs for each DMN service.
You can view the documentation by pasting this url into your browser: http://localhost:8080/q/swagger-ui/


## Deploying to OpenShift

### 1. Configure the cluster
**Run the following steps while logged in as a user developer permissions**

1. Create a new project with the command

```shell
oc new-project kogito-demo
oc new-project nexus
```

**Run the following steps while logged in as a user with cluster-admin permissions**


2. Install the OpenShift Pipelines Operator
3. Set up the openshift monitoring stack.
```shell
cd monitoring-setup
chmod ugo+x setup-monitoring-stack.sh
./setup-monitoring-stack.sh
```

Wait 5 minutes for the cluster to settle after configuring the logging stack.
You can monitor the progress by watching the cluster status in the admin console

---

### 2. Set up Nexus
**Run the following steps while logged in as a regular user in OpenShift**



1. Install nexus onto the cluster
```shell
oc project nexus
oc create -f https://raw.githubusercontent.com/philipfoster/nexus/master/nexus3-persistent-template.yaml
oc new-app nexus3-persistent
```
Wait a few minutes until the Nexus instance is ready.

2. Get the route URL for the `nexus` route.
```shell
oc get route -n nexus
```

Open the page in your browser. Click the Sign In button and log in as the admin user.
You can obtain the default password by running the commands

```shell
NEXUS_POD=`oc get pod -n nexus |  grep Running | awk '{ print $1 }'`
oc exec -it $NEXUS_POD -n nexus -- cat /nexus-data/admin.password
```

Go through the setup wizard. When prompted if you want to configure anonymous access, choose **Enable**


3. Set up a new `maven2 (proxy)` repository named `redhat-ga` that points to https://maven.repository.redhat.com/ga/.
   Add the newly created `redhat-ga` repo to the pre-existing `maven-public` repo.

4. Create a new `docker (hosted)` repository. Set the name to `docker-imgs` and check the "HTTP" repository connector box.
    Set the http connector port to 9999. Leave the rest of the settings as default and click the **Create repository** button at the bottom
    of the screen.

### 3. Install the project

**Run the following steps while logged in as a regular user in OpenShift**

1. Update the following URLs to point to your OpenShift cluster
    1. all `url` fields in the file [settings-cm.yaml](cluster-setup/pipeline/mvn-settings-cm.yaml) to point to the `maven-public` nexus repo
        - Note: In the nexus repositories list, click the **Copy** button next to the repo to get the URL
    2. the `newName` field in [dev kustomization.yaml](ocp/overlays/dev/kustomization.yaml)
    3. the URL in the json content in the file [push-secret.yaml](cluster-setup/pipeline/push-secret.yaml)
        - Note: Look at the routes created in the `nexus` project. Use the docker one
        - Also, update the password. Nexus uses a random password that you must change on first login
        - Also, update the "auth" field. This is the base64 encoded version of the string `username:your-password`
          you can get this value by running the command 
       ```shell 
       echo -n "admin:p4ssw0rd1\!" | base64
       ````
    4. the value of the buildah param `IMAGE` in the file [tekton.yaml](ocp/overlays/dev/tekton.yaml) 

2. Deploy the kubernetes resources
```shell
oc project kogito-demo
cd ocp/overlays/dev
oc apply -k .
```

3. Open the OpenShift web console in your browser. Navigate to the kogito-demo project.
   Click **Pipelines** > **new-pipeline** > **Actions Dropdown** > **Start**.

A start pipeline dialog box should pop up. Fill the box to look like the image.

![Start Pipeline Dialog](docs/StartPipelineDialog.png)

Then, click start.

5. The build will start. Monitor the PipelineRun until it is completed. Clicking on any of the 3
   task bubbles will show the logs for that step.

![Monitor progres](docs/PipelineRunMonitoring.png)

After all bubbles are green, your build is complete.

6. Navigate to the Deployment. If a pod is in the `ImagePullBackOff` state, delete it so that the deployment will re-run.
7. The deployment is complete. Check the routes in the `kogito-demo` project for the URL to test with.

## Viewing Metrics

1. After the project is running, send some sample traffic with the command
   (note: update the domain to point to your cluster)
```shell

for i in {1..100}
do
  curl --location 'https://quarkus-demo-kogito-demo.apps.cluster-wvlpl.wvlpl.sandbox3007.opentlc.com/Traffic%20Violation' \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --data '{"Driver":{"Points":2},"Violation":{"Type":"speed","Actual Speed":120,"Speed Limit":100}}'  -sS > /dev/null
  
  echo "sent request $i"
done
```

2. Open the OpenShift web console in your browser. Navigate to the `kogito-demo` project. Click the **Observe** tab on the left side of the screen
3. Click **Metrics** -> **Select query dropdown** -> **Custom query**
4. In the expression box, type `rule_execution_duration_seconds` and press the Enter key
You should see metrics that look like the chart below

![Rule execution times](docs/RuleExecutionMetricsTimed.png)
This metric shows the rule execution times at the 10%, 25%, 50%, 75%, 90%, and 99% percentiles over time. This
can be useful for troubleshooting performance issues.

5. Clear the expression box, and type in `jvm_memory_used_bytes`.
![JVM Memory Metrics](docs/JvmMemoryMetrics.png)
This metric shows statistics on memory usage over time. 

6. You can lookup a full list of metrics with the command
```shell
curl --location 'https://quarkus-demo-kogito-demo.apps.cluster-wvlpl.wvlpl.sandbox3007.opentlc.com/q/metrics' 
```
