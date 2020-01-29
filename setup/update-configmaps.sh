echo Which keycloak environment would you like to setup/update? [dev,test,prod]
read envValue

FILE=./properties/setup-$envValue.properties

SOAM_KC_LOAD_USER_ADMIN=$(grep -i 'SOAM_KC_LOAD_USER_ADMIN' $FILE  | cut -f2 -d'=')
KCADM_FILE_BIN_FOLDER=$(grep -i 'KCADM_FILE_BIN_FOLDER' $FILE  | cut -f2 -d'=')
SOAM_KC_REALM_ID=$(grep -i 'SOAM_KC_REALM_ID' $FILE  | cut -f2 -d'=')
OPENSHIFT_NAMESPACE=$(grep -i 'OPENSHIFT_NAMESPACE' $FILE  | cut -f2 -d'=')
DEVEXCHANGE_KC_REALM_ID=$(grep -i 'DEVEXCHANGE_KC_REALM_ID' $FILE  | cut -f2 -d'=')
DB_JDBC_CONNECT_STRING=$(grep -i 'DB_JDBC_CONNECT_STRING' $FILE  | cut -f2 -d'=')
DB_CONNECT_USER=$(grep -i 'DB_CONNECT_USER' $FILE  | cut -f2 -d'=')
DB_CONNECT_PASS=$(grep -i 'DB_CONNECT_PASS' $FILE  | cut -f2 -d'=')
CHES_CLIENT_ID=$(grep -i 'CHES_CLIENT_ID' $FILE  | cut -f2 -d'=')
CHES_CLIENT_SECRET=$(grep -i 'CHES_CLIENT_SECRET' $FILE  | cut -f2 -d'=')
CHES_TOKEN_URL=$(grep -i 'CHES_TOKEN_URL' $FILE  | cut -f2 -d'=')
CHES_ENDPOINT_URL=$(grep -i 'CHES_ENDPOINT_URL' $FILE  | cut -f2 -d'=')

echo Properties Defined
echo -----------------------------------------------------------
echo SOAM_KC_LOAD_USER_ADMIN: $SOAM_KC_LOAD_USER_ADMIN
echo KCADM_FILE_BIN_FOLDER: $KCADM_FILE_BIN_FOLDER
echo SOAM_KC_REALM_ID: $SOAM_KC_REALM_ID
echo OPENSHIFT_NAMESPACE: $OPENSHIFT_NAMESPACE
echo DEVEXCHANGE_KC_REALM_ID: $DEVEXCHANGE_KC_REALM_ID
echo DB properties omitted. 
echo -----------------------------------------------------------
#########################################################################################

oc project $OPENSHIFT_NAMESPACE-$envValue

echo Logging in
$KCADM_FILE_BIN_FOLDER/kcadm.sh config credentials --server https://$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/auth --realm $SOAM_KC_REALM_ID --user $SOAM_KC_LOAD_USER_ADMIN


###########################################################
#Fetch the public key
###########################################################
getPublicKey(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get keys -r $SOAM_KC_REALM_ID | grep -Po 'publicKey" : "\K([^"]*)'
}

echo Fetching public key from SOAM
soamFullPublicKey="-----BEGIN PUBLIC KEY----- $(getPublicKey) -----END PUBLIC KEY-----"

###########################################################
#Setup for soam-sso-config-map
###########################################################
getSoamKCServiceClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | python3 -c "import sys, json; data = json.load(sys.stdin); output_dict = [x for x in data if x['clientId'] == 'soam-kc-service'];  print(output_dict)" | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

getSoamKCServiceClientSecret(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$soamKCServiceClientID/client-secret -r $SOAM_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

echo Fetching client ID for soam-kc-service client
soamKCServiceClientID=$(getSoamKCServiceClientID)
echo Fetching client secret for soam-kc-service client
soamKCServiceClientSecret=$(getSoamKCServiceClientSecret)

echo Creating config map soam-sso-config-map 
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap soam-sso-config-map --from-literal=clientID=soam-kc-service --from-literal=clientSecret=$soamKCServiceClientSecret --from-literal=soamApiURL=https://soam-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=tokenURL=https://$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for sso-$envValue application
oc set env --from=configmap/soam-sso-config-map dc/sso-$envValue

###########################################################
#Setup for codetable-api-config-map
###########################################################

echo
echo Creating config map codetable-api-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap codetable-api-config-map --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME=$DB_CONNECT_USER --from-literal=ORACLE_PASSWORD=$DB_CONNECT_PASS --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for codetable-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/codetable-api-config-map dc/codetable-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for digitalid-api-config-map
###########################################################

echo
echo Creating config map digitalid-api-config-map 
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap digitalid-api-config-map --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME=$DB_CONNECT_USER --from-literal=ORACLE_PASSWORD=$DB_CONNECT_PASS --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for digitalid-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/digitalid-api-config-map dc/digitalid-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for student-api-config-map
###########################################################

echo
echo Creating config map student-api-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap student-api-config-map --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME=$DB_CONNECT_USER --from-literal=ORACLE_PASSWORD=$DB_CONNECT_PASS --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for student-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/student-api-config-map dc/student-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for pen-request-api-config-map
###########################################################

echo
echo Creating config map pen-request-api-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap pen-request-api-config-map --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME=$DB_CONNECT_USER --from-literal=ORACLE_PASSWORD=$DB_CONNECT_PASS --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for pen-request-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/pen-request-api-config-map dc/pen-request-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for document-api-config-map
###########################################################
getDocumentAPIServiceClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | python3 -c "import sys, json; data = json.load(sys.stdin); output_dict = [x for x in data if x['clientId'] == 'document-api-service'];  print(output_dict)" | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

getDocumentAPIServiceClientSecret(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$documentAPIServiceClientID/client-secret -r $SOAM_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
echo
echo Fetching client ID for document-api-service client
documentAPIServiceClientID=$(getDocumentAPIServiceClientID)
echo Fetching client secret for document-api-service client
DOC_API_CLIENT_SECRET=$(getDocumentAPIServiceClientSecret)

echo
echo Creating config map document-api-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap document-api-config-map --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME=$DB_CONNECT_USER --from-literal=ORACLE_PASSWORD=$DB_CONNECT_PASS --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=CLIENT_ID=document-api-service --from-literal=CLIENT_SECRET=$DOC_API_CLIENT_SECRET --from-literal=CODETABLE_URL=https://codetable-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=FILE_EXTENSIONS="jpeg,jpg,png,pdf" --from-literal=FILE_MAXSIZE=10485760 --from-literal=HIBERNATE_LOG_LEVEL=INFO --from-literal=TOKEN_URL=https://$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for pen-request-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/document-api-config-map dc/document-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for pen-request-email-api-config-map
###########################################################

echo
echo Creating config map pen-request-email-api-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap pen-request-email-api-config-map --from-literal=SOAM_PUBLIC_KEY="$soamFullPublicKey" --from-literal=CHES_CLIENT_ID=$CHES_CLIENT_ID --from-literal=CHES_CLIENT_SECRET=$CHES_CLIENT_SECRET --from-literal=CHES_TOKEN_URL=$CHES_TOKEN_URL --from-literal=CHES_ENDPOINT_URL=$CHES_ENDPOINT_URL --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=EMAIL_TEMPLATE_COMPLETED_REQUEST="<!DOCTYPE html><html><head><meta charset="ISO-8859-1"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello {0},<br><br><b>We have located your PEN!</b><br><br>Steps to access your PEN:<ol><li>Click this link <a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca/">here</a></li><li>Log in using your BCeID (the same method you did when submitting the original request)</li></ol>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca">https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --from-literal=EMAIL_TEMPLATE_REJECTED_REQUEST="<!DOCTYPE html><html><head><meta charset="ISO-8859-1"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello,<br><br><b>Your Personal Education Number (PEN) request could not be fulfilled</b> for the following reason(s):<br><br><b><i>{0}</i></b><br><br>Please click <a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca/">here</a> and log in using your BCeID to submit a new PEN request if necessary.<br><br>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca">https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --from-literal=EMAIL_TEMPLATE_ADDITIONAL_INFO="<!DOCTYPE html><html><head><meta charset="ISO-8859-1"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello,<br><br><b>Your Personal Education Number (PEN) request is in progress but we have determined that we do not have enough information to locate your PEN.</b><br><br>Steps to provide additional information:<ol><li>Click this link <a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca/">here</a></li><li>Log in using the same method you did when submitting the original request) and</li><li>Respond to the additional information request</li></ol>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href="https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca">https://pen-request-c2mvws-$envValue.pathfinder.gov.bc.ca</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for pen-request-email-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/pen-request-email-api-config-map dc/pen-request-email-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for soam-api-config-map
###########################################################
getSoamAPIServiceClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | python3 -c "import sys, json; data = json.load(sys.stdin); output_dict = [x for x in data if x['clientId'] == 'soam-api-service'];  print(output_dict)" | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

getSoamAPIServiceClientSecret(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$soamAPIServiceClientID/client-secret -r $SOAM_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
echo
echo Fetching client ID for soam-api-service client
soamAPIServiceClientID=$(getSoamAPIServiceClientID)
echo Fetching client secret for soam-api-service client
soamAPIServiceClientSecret=$(getSoamAPIServiceClientSecret)

echo Creating config map soam-api-config-map 
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap soam-api-config-map --from-literal=CLIENT_ID=soam-api-service --from-literal=CLIENT_SECRET=$soamAPIServiceClientSecret --from-literal=CODETABLE_URL=https://codetable-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=DIGITALID_URL=https://digitalid-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=STUDENT_URL=https://student-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=TOKEN_URL=https://$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for soam-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/soam-api-config-map dc/soam-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for pen-request-backend-config-map
###########################################################
getPenRequestServiceClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | python3 -c "import sys, json; data = json.load(sys.stdin); output_dict = [x for x in data if x['clientId'] == 'pen-request-soam'];  print(output_dict)" | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

getPenRequestServiceClientSecret(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$penRequestServiceClientID/client-secret -r $SOAM_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
echo
echo Fetching client ID for pen-request-soam client
penRequestServiceClientID=$(getPenRequestServiceClientID)
echo Fetching client secret for pen-request-soam client
penRequestServiceClientSecret=$(getPenRequestServiceClientSecret)
echo
echo Generating private and public keys
ssh-keygen -b 4096 -t rsa -f tempPenBackendkey -q -N ""
UI_PRIVATE_KEY_VAL="$(cat tempPenBackendkey)"
UI_PUBLIC_KEY_VAL="$(ssh-keygen -f tempPenBackendkey -e -m pem)"

echo Removing key files
rm tempPenBackendkey
rm tempPenBackendkey.pub

echo Creating config map pen-request-backend-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap pen-request-backend-config-map --from-literal=UI_PRIVATE_KEY="$UI_PRIVATE_KEY_VAL" --from-literal=UI_PUBLIC_KEY="$UI_PUBLIC_KEY_VAL" --from-literal=SOAM_CLIENT_ID=pen-request-soam --from-literal=SOAM_CLIENT_SECRET=$penRequestServiceClientSecret --from-literal=SERVER_FRONTEND=https://pen-request-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=ISSUER=PEN_Retrieval_Application --from-literal=CODE_TABLE_ENDPOINT=https://codetable-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=PEN_REQUEST_API_ENDPOINT=https://pen-request-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=SOAM_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SOAM_DISCOVERY=https://$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/auth/realms/$SOAM_KC_REALM_ID/.well-known/openid-configuration --from-literal=SOAM_URL=https://$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for pen-request-backend-$SOAM_KC_REALM_ID application
oc set env --from=configmap/pen-request-backend-config-map dc/pen-request-backend-$SOAM_KC_REALM_ID

###########################################################
#Setup for pen-request-frontend-config-map
###########################################################

echo Creating config map pen-request-frontend-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap pen-request-frontend-config-map --from-literal=HOST_ROUTE=pen-request-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca  --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for pen-request-frontend-$SOAM_KC_REALM_ID application
oc set env --from=configmap/pen-request-frontend-config-map dc/pen-request-frontend-$SOAM_KC_REALM_ID

###########################################################
#Setup for student-admin-backend-config-map
###########################################################
getStudentAdminClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | python3 -c "import sys, json; data = json.load(sys.stdin); output_dict = [x for x in data if x['clientId'] == 'student-admin-soam'];  print(output_dict)" | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

getStudentAdminClientSecret(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$studentAdminClientID/client-secret -r $SOAM_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
echo
echo Fetching client ID for student-admin-soam client
studentAdminClientID=$(getStudentAdminClientID)
echo Fetching client secret for student-admin-soam client
studentAdminClientSecret=$(getStudentAdminClientSecret)
echo
echo Generating private and public keys
ssh-keygen -b 4096 -t rsa -f tempStudentAdminBackendkey -q -N ""
UI_PRIVATE_KEY_VAL="$(cat tempStudentAdminBackendkey)"
UI_PUBLIC_KEY_VAL="$(ssh-keygen -f tempStudentAdminBackendkey -e -m pem)"

echo Removing key files
rm tempStudentAdminBackendkey
rm tempStudentAdminBackendkey.pub

echo Creating config map student-admin-backend-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap student-admin-backend-config-map --from-literal=UI_PRIVATE_KEY="$UI_PRIVATE_KEY_VAL" --from-literal=UI_PUBLIC_KEY="$UI_PUBLIC_KEY_VAL" --from-literal=ID=student-admin-soam --from-literal=SECRET=$studentAdminClientSecret --from-literal=SERVER_FRONTEND=https://student-admin-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=CODETABLE_API_URL=https://codetable-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=ISSUER=STUDENT_ADMIN_APPLICATION --from-literal=SOAM_PUBLIC_KEY="$soamFullPublicKey" --from-literal=PEN_REQUEST_API_URL=https://pen-request-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=PUBLIC_KEY="$soamFullPublicKey" --from-literal=DISCOVERY=https://$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/auth/realms/$SOAM_KC_REALM_ID/.well-known/openid-configuration --from-literal=KC_DOMAIN=https://$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/auth/realms/$SOAM_KC_REALM_ID  --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for student-admin-backend-$SOAM_KC_REALM_ID application
oc set env --from=configmap/student-admin-backend-config-map dc/student-admin-backend-$SOAM_KC_REALM_ID

###########################################################
#Setup for student-admin-frontend-config-map
###########################################################

echo Creating config map student-admin-frontend-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap student-admin-frontend-config-map --from-literal=HOST_ROUTE=student-admin-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=BACKEND_ROOT=https://student-admin-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca  --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for student-admin-frontend-$SOAM_KC_REALM_ID application
oc set env --from=configmap/student-admin-frontend-config-map dc/student-admin-frontend-$SOAM_KC_REALM_ID

echo Complete.


