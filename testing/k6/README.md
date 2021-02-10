# K6 Load Testing 
These tests are made for testing the Ministry of Education's Student Online Access Module (SOAM), but the framework can be used to test any site. Just replace the K6 test and config.
## Running Locally
* Docker is installed
* Enter correct values into `src/config/config.json`
* Navigate to k6 folder and run the following command to setup monitoring
```
docker-compose up --remove-orphans -d influxdb grafana
```
* Run the following command to run tests
```
docker-compose run k6 run -e CONFIG=/config/config.json /scripts/soamLoadTest.ts
```
* You can view the test metrics on the [Grafana dashboard](http://localhost:3000/d/XJhgbUpil/soam-load-testing-dashboard)

## Deploy to OpenShift
### Influxdb
Coming soon
### Grafana
Coming soon
### K6
* Ensure `config.json` is filled out with correct values, navigate to the openshift/k6 folder, and run the log into OpenShift command
* Create config map
```
oc create -n <NAMESPACE> configmap k6-config --from-file=../../src/config/config.json
```
* Build K6 job
```
oc -n <NAMESPACE> process -f bc.yaml | oc -n <NAMESPACE> apply -f -
```
* Creating the K6 job will automatically run the load test on creation, and will terminate the container upon completion
```
oc -n <NAMESPACE> process -f dc.yaml -p IMAGE_NAMESPACE=<NAMESPACE> | oc -n <NAMESPACE> apply -f -
```
* Once tests are complete, clean up K6 job artifacts
```
oc -n <NAMESPACE> get all,configmap,secret,pvc -l group=educ-k6
oc -n <NAMESPACE> delete all,configmap,secret,pvc -l group=educ-k6
```

