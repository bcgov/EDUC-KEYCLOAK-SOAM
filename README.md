# Scripted Installation 

## Building
* Switch to the correct project/namespace (tools)
* Navigate to `/.pipeline` folder in project
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

The application should now be deployed and running in your target namespace environment.
# Setup
## Requirements
In order to complete the automated setup, the keycloak binaries are required. They need to be downloaded (unzipped) into a folder on your machine:

[Keycloak Downloads](https://www.keycloak.org/downloads.html)
(Please select Server Standalone Distribution)

## Completing Setup

### Setup Property File
Navigate to the `/setup` folder from the root. Edit the `setup.properties` file. The following values should be modified for your environment:

| Property  | Description |
| ------------- | ------------- |
| SOAM_KC_LOAD_USER_ADMIN  | Contains the username of the admin user used for load in the SOAM Keycloak instance. The initially created admin user info can be found in the `Secrets` area of your OpenShift namespace environment, in the secret named `sso-admin-env`.  |
| DEVEXCHANGE_KC_LOAD_USER_ADMIN  | Contains the username of the admin user used for load in your DevExchange realm. This user must be created before this script will function (user is used to complete the `kcadm` functions). It will need to be configured with a password and the `admin` role or group.   |
| KCADM_FILE_BIN_FOLDER  | This is the `bin` folder found on your disk which contains the `kcadm.sh` script. [Download a fresh version of Keycloak](https://www.keycloak.org/downloads.html) if you do not have the binaries.  |
| SOAM_KC_REALM_ID  | Your SOAM keycloak realm ID [e.g. `master`]  |
| DEVEXCHANGE_KC_REALM_ID  | The BCDevExchange realm ID for your application [e.g. `v15rh2ab`]  |
| DB_JDBC_CONNECT_STRING  | This is the namespace of your OpenShift environment e.g. `d2vwrs`  |
| DB_CONNECT_USER  | Contains the DB connection user  |
| DB_CONNECT_PASS  | Contains the DB connection PW  |
| CHES_CLIENT_ID  | Contains the CHES client ID  |
| CHES_CLIENT_SECRET  | Contains the CHES client secret  |
| CHES_TOKEN_URL  | Contains the CHES token endpoint  |
| CHES_ENDPOINT_URL  | Contains the CHES endpoint  |

### Run the setup for the Keycloak DevExchange realm
```
#Navigate to the /setup folder, run the following and follow the prompts
./devexchange-keycloak-setup.sh
```

#### Expected Output
The following output is expected while running the `devexchange-keycloak-setup.sh` script:

```
Properties Defined
-----------------------------------------------------------
KC_LOAD_USER_ADMIN: abcuser
KCADM_FILE_BIN_FOLDER: /mnt/c/MyPC/Apps/keycloak-8.0.1/bin
SOAM_KC_REALM_ID: master
OPENSHIFT_NAMESPACE: c4sdss
DEVEXCHANGE_KC_REALM_ID: yourrealm
-----------------------------------------------------------
This script will setup the target keycloak instance for SOAM configuration
Note a user will need to be created in the UI prior to running this script [see properties file]

DevExhange Realm defined in property file is: yourrealm
Which keycloak environment would you like to update? [dev,test,prod]
test
Please enter BC Services Card client ID for SSO BCDevExchange:
abc.urn.user
Please enter BC Services Card client secret for SSO BCDevExchange:
Logging in
Logging into https://sso-yourenv.pathfinder.gov.bc.ca/auth as user abcuser of realm yourrealm
Enter password: ********
Updating realm details
Removing BCSC IDP if exists...
Resource not found for url: 
https://sso-yourenv.pathfinder.gov.bc.ca/auth/admin/realms/yourrealm/identity-provider/instances/bcsc
Creating BC Services Card IDP...
Created new instance with id 'bcsc'
Creating mappers for BC Services Card DevExchange IDP...
Created new mapper with id '9b76ffbd-9db0-4d14-8cb7-382403ae687d'
Created new mapper with id '91a87054-1758-42e1-8ca0-aa5b9bd7bbf7'
Created new mapper with id 'f6e80f19-323b-43b8-a47d-37652ebab925'
Created new mapper with id '5717682b-6ca9-4288-867e-d738e966b458'
Created new mapper with id 'e4e4686f-8475-48f1-aabb-eaadb28fd0b1'
Created new mapper with id '4c9b9376-bb05-4343-aea5-ab06b759696a'
Created new mapper with id '7603adba-d7af-4d0a-9a9a-85158ec52c09'
Created new mapper with id '50f684cb-61bb-447f-bd81-8a19de012ac5'
Created new mapper with id '6167e314-fd18-476f-a2c2-e79ca16794f9'
Created new mapper with id '558c9843-a63a-4dd7-965c-fab0afbf1609'
Created new mapper with id '04480443-e98c-44fb-9459-bfe079e7546b'
Created new mapper with id '5a5ead80-62d3-403e-96de-54d6ce0e71bf'
Created new mapper with id '3a8795e2-a9c2-4b38-a5ec-44e7d938dcc4'
Created new mapper with id '68536479-4264-4010-a514-0513fbffad96'
Created new mapper with id 'ba601374-6830-4fbe-ac19-099c6a2c2391'
Created new mapper with id 'f4aea905-82ec-4188-b3be-33c8561dc6b9'
Created new mapper with id '9f559ae6-bbd1-41e4-a6f9-891c5a79f1d5'
Created new mapper with id 'eac41130-d67d-4d2d-920c-44bd0b5b3d40'
Creating mappers for IDIR DevExchange IDP if not exist...
Created new mapper with id 'adeda34f-a467-476c-862b-187b033cda2a'
Created new mapper with id '2193183d-b699-4311-8cdf-d8dba55edd1e'
Creating mappers for BCeID DevExchange IDP if not exist...
Created new mapper with id '7172f293-739b-4f68-a3d3-431603908c44'
Created new mapper with id '7f931e20-d3f2-4238-bc6b-10168cc6615a'
Creating SOAM client
Created new client with id '82ab4b0a-98ed-4c34-863b-e43fe4978814'
Complete.
```

### Run the setup for the SOAM Keycloak Instance

```
#Navigate to the /setup folder, run the following and follow the prompts
./soam-keycloak-setup.sh
```

#### Expected Output
The following output is expected while running the `soam-keycloak-setup.sh` script:

```
Properties Defined
-----------------------------------------------------------
KC_LOAD_USER_ADMIN: abcuser
KCADM_FILE_BIN_FOLDER: /mnt/c/MyPC/Apps/keycloak-8.0.1/bin
SOAM_KC_REALM_ID: master
OPENSHIFT_NAMESPACE: c4sdss
DEVEXCHANGE_KC_REALM_ID: yourrealm
-----------------------------------------------------------
This script will setup the target keycloak instance for SOAM configuration
Note a user will need to be created prior to running this script

Which keycloak environment would you like to update? [dev,test,prod]
test
Please enter client secret for soam user in SSO BCDevExchange:
Thank you.
Logging in
Logging into https://c4sdss-test.pathfinder.gov.bc.ca/auth as user abcuser of realm master
Enter password: ********
Updating realm details
Creating STUDENT_ADMIN role
Created new role with id 'STUDENT_ADMIN'
Creating Client Scopes
Created new client-scope with id 'READ_CODETABLE_SET'
Created new client-scope with id 'READ_DIGITALID'
Created new client-scope with id 'WRITE_DIGITALID'
Created new client-scope with id 'READ_PEN_REQUEST'
Created new client-scope with id 'WRITE_PEN_REQUEST'
Created new client-scope with id 'READ_STUDENT'
Created new client-scope with id 'WRITE_STUDENT'
Created new client-scope with id 'SOAM_LOGIN'
Creating authenticators
Created new flow with id '5760789d-7f72-42b9-9829-382660f302db'
Created new flow with id 'f7fe5dd2-bef5-4c9e-bb35-cd109e8fa8d8'
Creating executors
Created new execution with id 'd88545b1-52b5-4363-8609-39f895ecaba6'
Created new execution with id 'd4136d90-9d2e-435a-acc5-5f073b58c473'

Updating first login executor to required

Updating post login executor to required
Creating DevExchange IDP
Building IDP instance...
Created new instance with id 'keycloak_bcdevexchange'
Creating mappers for IDP...
Created new mapper with id '98a4e1e6-b573-4667-a2de-fa16a90fb182'
Created new mapper with id '1900a355-6419-4796-851c-1c441e21ac87'
Created new mapper with id '6d0aeaea-dac5-4628-954f-aa4bbdcb5645'
Created new mapper with id '07606042-80d7-45c9-b825-003fabdae74e'
Creating clients
Creating soam-kc-service client
Created new client with id 'ae944938-8c2b-46d4-b85c-10fa01ac8b58'
Creating pen-request-soam Keycloak client
Created new client with id '8fcd7a11-d72c-4092-b35c-44f725a86932'
Creating soam-api-service Keycloak client
Created new client with id '13f9fccf-2462-4f4d-a254-0b08658b6194'
Creating student-admin-soam Keycloak client
Created new client with id 'f98ecd95-9e7d-41ce-8ed7-b9cae9b4ad45'
Complete.
```

# Cleanup
## NOTE THIS WILL REMOVE YOUR ENTIRE ENVIRONMENT AND DATA!!!!
Switch to the correct project/namespace

```
oc delete all,rc,svc,dc,route,pvc,secret -l app-name=sso
oc delete pvc -l statefulset=sso-pgsql-dev
oc delete cm -l cluster-name=sso-pgsql-dev
```