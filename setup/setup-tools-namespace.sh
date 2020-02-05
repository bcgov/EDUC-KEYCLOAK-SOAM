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

echo Creating config map pen-request-email-api-test-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap pen-request-email-api-test-config-map --from-literal=SOAM_PUBLIC_KEY="ABC" --from-literal=CHES_CLIENT_ID=$CHES_CLIENT_ID --from-literal=CHES_CLIENT_SECRET=$CHES_CLIENT_SECRET --from-literal=CHES_TOKEN_URL=$CHES_TOKEN_URL --from-literal=CHES_ENDPOINT_URL=$CHES_ENDPOINT_URL --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=EMAIL_TEMPLATE_COMPLETED_REQUEST="<!DOCTYPE html><html><head><meta charset="ISO-8859-1"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello {0},<br><br><b>We have located your PEN!</b><br><br>Steps to provide additional information:<ol><li>Click this link <a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca/">here</a></li><li>Log in using (the same method you did when submitting the original request)</li></ol>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca">https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --from-literal=EMAIL_TEMPLATE_REJECTED_REQUEST="<!DOCTYPE html><html><head><meta charset="ISO-8859-1"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello,<br><br><b>Your Personal Education Number (PEN) request could not be fulfilled</b> for the following reason(s):<br><br><b><i>{0}</i></b><br><br>Please click <a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca/">here</a> and log in using the same method you did when submitting the original request to submit a new PEN request if necessary.<br><br>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca">https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --from-literal=EMAIL_TEMPLATE_ADDITIONAL_INFO="<!DOCTYPE html><html><head><meta charset="ISO-8859-1"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello,<br><br><b>Your Personal Education Number (PEN) request is in progress but we have determined that we do not have enough information to locate your PEN.</b><br><br>Steps to provide additional information:<ol><li>Click this link <a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca/">here</a></li><li>Log in using the same method you did when submitting the original request) and</li><li>Respond to the additional information request</li></ol>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca">https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --dry-run -o yaml | oc apply -f -
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

echo Creating SonarQube tokens
echo Creating Code Table API token
SONAR_TOKEN_CODETABLE_API=$(curl --silent -u $SONARQUBE_USER:$SONARQUBE_PW -X POST --data "name=Code Table API" "$SONARQUBE_URL/api/user_tokens/generate" | jq '.token')
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


echo
echo Re-creating code-table-api-secrets
oc delete secret code-table-api-secrets
oc create secret generic code-table-api-secrets --from-literal=sonarqube-token=${SONAR_TOKEN_CODETABLE_API//\"} --from-literal=sonarqube-host=$SONARQUBE_URL

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

echo Complete.


