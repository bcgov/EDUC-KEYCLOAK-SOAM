# Scripted Installation 

## Building
* Switch to the correct project/namespace (tools)
* Navigate to /.pipeline folder in project
* Run the following command where the `--pr` value is the latest known working pull request

```
./npmw build -- --pr=71
```

## Deploying
Run the following command where:
* `--pr` is the pull request number which was built
* `--env` is the environment value [dev,test,prod]

```
./npmw deploy -- --pr=71 --env=dev
```

# Setup
## Requirements
In order to complete the automated setup, the keycloak binaries are required. They need to be downloaded (unzipped) into a folder on your machine:

[Keycloak Downloads](https://www.keycloak.org/downloads.html)
(Please select Server Standalone Distribution)

## Completing Setup
Once Keycloak & Patroni are deployed and running, log into the administrator console following the application URL (found on the OpenShift console). Once logged in using the admin credentials (found in the Secrets), proceed with adding a new user:

* Named `loaduser`
* Add the `admin` role to the user
* Create a password for the user and ensure it is not temporary
* Run the following script to setup the environment for SOAM (follow the prompts)  

```
#Navigate to the root of the EDUC-KEYCLOAK-SOAM folder
./soamsetup.sh
```

# Cleanup
## NOTE THIS WILL REMOVE YOUR ENTIRE ENVIRONMENT AND DATA!!!!
Switch to the correct project/namespace

```
oc delete all,rc,svc,dc,route,pvc,secret -l app-name=sso
oc delete pvc -l statefulset=sso-pgsql-test
oc delete cm -l cluster-name=sso-pgsql-test
```