FILE=./properties/setup-tools.properties

OPENSHIFT_NAMESPACE=$(grep -i 'OPENSHIFT_NAMESPACE' $FILE  | cut -f2 -d'=')
CHES_CLIENT_ID=$(grep -i 'CHES_CLIENT_ID' $FILE  | cut -f2 -d'=')
CHES_CLIENT_SECRET=$(grep -i 'CHES_CLIENT_SECRET' $FILE  | cut -f2 -d'=')
CHES_TOKEN_URL=$(grep -i 'CHES_TOKEN_URL' $FILE  | cut -f2 -d'=')
CHES_ENDPOINT_URL=$(grep -i 'CHES_ENDPOINT_URL' $FILE  | cut -f2 -d'=')
SONARQUBE_URL=$(grep -i 'SONARQUBE_URL' $FILE  | cut -f2 -d'=')
SONARQUBE_USER=$(grep -i 'SONARQUBE_USER' $FILE  | cut -f2 -d'=')
SONARQUBE_PW=$(grep -i 'SONARQUBE_PW' $FILE  | cut -f2 -d'=')

echo Properties Defined
echo -----------------------------------------------------------
echo OPENSHIFT_NAMESPACE: $OPENSHIFT_NAMESPACE
echo Other properties omitted. 
echo -----------------------------------------------------------
#########################################################################################
envValue=tools

oc project $OPENSHIFT_NAMESPACE-$envValue

###########################################################
#Setup for pen-request-email-api-test-config-map
###########################################################

echo
echo Revoking SonarQube tokens
curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=Code Table API" "$SONARQUBE_URL/api/user_tokens/revoke"
curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=Digital ID API" "$SONARQUBE_URL/api/user_tokens/revoke"
curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=PEN Request" "$SONARQUBE_URL/api/user_tokens/revoke"
curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=PEN Request API" "$SONARQUBE_URL/api/user_tokens/revoke"
curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=PEN Request Email API" "$SONARQUBE_URL/api/user_tokens/revoke"
curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=SOAM API" "$SONARQUBE_URL/api/user_tokens/revoke"
curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=Student API" "$SONARQUBE_URL/api/user_tokens/revoke"
curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=Services Card API" "$SONARQUBE_URL/api/user_tokens/revoke"
curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=PEN Demog API" "$SONARQUBE_URL/api/user_tokens/revoke"

echo Creating SonarQube tokens
echo Creating Digital ID API token
SONAR_TOKEN_DIGITALID_API=$(curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=Digital ID API" "$SONARQUBE_URL/api/user_tokens/generate" | jq '.token')
echo Creating PEN Request token
SONAR_TOKEN_PEN_REQUEST=$(curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=PEN Request" "$SONARQUBE_URL/api/user_tokens/generate" | jq '.token')
echo Creating PEN Request API token
SONAR_TOKEN_PEN_REQUEST_API=$(curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=PEN Request API" "$SONARQUBE_URL/api/user_tokens/generate" | jq '.token')
echo Creating PEN Request Email API token
SONAR_TOKEN_PEN_REQUEST_EMAIL_API=$(curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=PEN Request Email API" "$SONARQUBE_URL/api/user_tokens/generate" | jq '.token')
echo Creating SOAM API token
SONAR_TOKEN_SOAM_API=$(curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=SOAM API" "$SONARQUBE_URL/api/user_tokens/generate" | jq '.token')
echo Creating Student API token
SONAR_TOKEN_STUDENT_API=$(curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=Student API" "$SONARQUBE_URL/api/user_tokens/generate" | jq '.token')
echo Creating Services Card API token
SONAR_TOKEN_SERVICES_CARD_API=$(curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=Services Card API" "$SONARQUBE_URL/api/user_tokens/generate" | jq '.token')
echo Creating PEN Demog API token
SONAR_TOKEN_PEN_DEMOG_API=$(curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=PEN Demog API" "$SONARQUBE_URL/api/user_tokens/generate" | jq '.token')


echo
echo Re-creating digitalid-api-secrets
oc delete secret digitalid-api-secrets
oc create secret generic digitalid-api-secrets --from-literal=sonarqube-token=${SONAR_TOKEN_DIGITALID_API//\"} --from-literal=sonarqube-host=$SONARQUBE_URL

echo Re-creating pen-request-secrets
oc delete secret pen-request-secrets 
oc create secret generic pen-request-secrets --from-literal=sonarqube-token=${SONAR_TOKEN_PEN_REQUEST//\"} --from-literal=sonarqube-host=$SONARQUBE_URL

echo Re-creating pen-request-api-secrets
oc delete secret pen-request-api-secrets
oc create secret generic pen-request-api-secrets --from-literal=sonarqube-token=${SONAR_TOKEN_PEN_REQUEST_API//\"} --from-literal=sonarqube-host=$SONARQUBE_URL

echo Re-creating pen-request-email-api-secrets
oc delete secret pen-request-email-api-secrets
oc create secret generic pen-request-email-api-secrets --from-literal=sonarqube-token=${SONAR_TOKEN_PEN_REQUEST_EMAIL_API//\"} --from-literal=sonarqube-host=$SONARQUBE_URL

echo Re-creating soam-api-secrets
oc delete secret soam-api-secrets
oc create secret generic soam-api-secrets --from-literal=sonarqube-token=${SONAR_TOKEN_SOAM_API//\"} --from-literal=sonarqube-host=$SONARQUBE_URL

echo Re-creating student-api-secrets
oc delete secret student-api-secrets
oc create secret generic student-api-secrets --from-literal=sonarqube-token=${SONAR_TOKEN_STUDENT_API//\"} --from-literal=sonarqube-host=$SONARQUBE_URL

echo Re-creating services-card-api-secrets
oc delete secret services-card-api-secrets
oc create secret generic services-card-api-secrets --from-literal=sonarqube-token=${SONAR_TOKEN_SERVICES_CARD_API//\"} --from-literal=sonarqube-host=$SONARQUBE_URL

echo Re-creating pen-demographics-api-secrets
oc delete secret pen-demographics-api-secrets
oc create secret generic pen-demographics-api-secrets --from-literal=sonarqube-token="${SONAR_TOKEN_PEN_DEMOG_API//\"}" --from-literal=sonarqube-host=$SONARQUBE_URL
echo Complete.