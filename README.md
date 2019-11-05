# Scripted Installation 

## Building
```
#Switch to the correct project/namespace (tools)
#Navigate to /.pipeline folder in project
./npmw build -- --pr=2
```

## Deploying
```
./npmw deploy -- --pr=2 --env=dev
```

## Setup
```
- Create an identity provider which is BC DevExchange Keycloak
- Create a test client
```

## Cleanup
#Switch to the correct project/namespace
```
oc delete all,rc,svc,dc,route,pvc,secret -l app-name=sso
```