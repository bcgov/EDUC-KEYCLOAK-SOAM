# K6 Load Testing 
These tests are made for testing the Ministry of Education's Student Online Access Module (SOAM), but the framework can be used to test any site. Just replace the K6 test and config.
## Running Locally
* Docker is installed
* Enter correct values into `src/config/local.json`
* Navigate to k6 folder and run the following command to setup monitoring
```
docker-compose up --remove-orphans -d influxdb grafana
```
* Run the following command to run tests
```
docker-compose run k6 run -e CONFIG=/config/local.json /scripts/soamLoadTest.ts
```
* You can view the test metrics on the [Grafana dashboard](http://localhost:3000/d/XJhgbUpil/soam-load-testing-dashboard)
