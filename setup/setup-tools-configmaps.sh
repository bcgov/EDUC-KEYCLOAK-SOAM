FILE=./setup.properties

OPENSHIFT_NAMESPACE=$(grep -i 'OPENSHIFT_NAMESPACE' $FILE  | cut -f2 -d'=')
CHES_CLIENT_ID=$(grep -i 'CHES_CLIENT_ID' $FILE  | cut -f2 -d'=')
CHES_CLIENT_SECRET=$(grep -i 'CHES_CLIENT_SECRET' $FILE  | cut -f2 -d'=')
CHES_TOKEN_URL=$(grep -i 'CHES_TOKEN_URL' $FILE  | cut -f2 -d'=')
CHES_ENDPOINT_URL=$(grep -i 'CHES_ENDPOINT_URL' $FILE  | cut -f2 -d'=')

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

echo Complete.


