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
- In realm settings, turn off login with email and turn on duplicate emails (Needs to be done in both the SOAM & BCDevExchange KC)

```

## Cleanup
#Switch to the correct project/namespace
```
oc delete all,rc,svc,dc,route,pvc,secret -l app-name=sso
```