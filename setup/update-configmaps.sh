echo Which keycloak environment would you like to setup/update? [dev,test,prod]
read -r envValue


FILE=./properties/setup-$envValue.properties

SOAM_KC_LOAD_USER_ADMIN=$(grep -i 'SOAM_KC_LOAD_USER_ADMIN' $FILE  | cut -f2 -d'=')
KCADM_FILE_BIN_FOLDER=$(grep -i 'KCADM_FILE_BIN_FOLDER' $FILE  | cut -f2 -d'=')
SOAM_KC_REALM_ID=$(grep -i 'SOAM_KC_REALM_ID' $FILE  | cut -f2 -d'=')
OPENSHIFT_NAMESPACE=$(grep -i 'OPENSHIFT_NAMESPACE' $FILE  | cut -f2 -d'=')
DEVEXCHANGE_KC_REALM_ID=$(grep -i 'DEVEXCHANGE_KC_REALM_ID' $FILE  | cut -f2 -d'=')
DB_JDBC_CONNECT_STRING=$(grep -i 'DB_JDBC_CONNECT_STRING' $FILE  | cut -f2 -d'=')

DB_USER_API_STUDENT=$(grep -i 'DB_USER_API_STUDENT' $FILE  | cut -f2 -d'=')
DB_PWD_API_STUDENT=$(grep -i 'DB_PWD_API_STUDENT' $FILE  | cut -f2 -d'=')
DB_USER_API_DIGITALID=$(grep -i 'DB_USER_API_DIGITALID' $FILE  | cut -f2 -d'=')
DB_PWD_API_DIGITALID=$(grep -i 'DB_PWD_API_DIGITALID' $FILE  | cut -f2 -d'=')
DB_USER_API_PEN_REQUEST=$(grep -i 'DB_USER_API_PEN_REQUEST' $FILE  | cut -f2 -d'=')
DB_PWD_API_PEN_REQUEST=$(grep -i 'DB_PWD_API_PEN_REQUEST' $FILE  | cut -f2 -d'=')
DB_USER_API_PEN_DEMOGRAPHICS=$(grep -i 'DB_USER_API_PEN_DEMOGRAPHICS' $FILE  | cut -f2 -d'=')
DB_PWD_API_PEN_DEMOGRAPHICS=$(grep -i 'DB_PWD_API_PEN_DEMOGRAPHICS' $FILE  | cut -f2 -d'=')
DB_USER_API_SERVICES_CARD=$(grep -i 'DB_USER_API_SERVICES_CARD' $FILE  | cut -f2 -d'=')
DB_PWD_API_SERVICES_CARD=$(grep -i 'DB_PWD_API_SERVICES_CARD' $FILE  | cut -f2 -d'=')

CHES_CLIENT_ID=$(grep -i 'CHES_CLIENT_ID' $FILE  | cut -f2 -d'=')
CHES_CLIENT_SECRET=$(grep -i 'CHES_CLIENT_SECRET' $FILE  | cut -f2 -d'=')
CHES_TOKEN_URL=$(grep -i 'CHES_TOKEN_URL' $FILE  | cut -f2 -d'=')
CHES_ENDPOINT_URL=$(grep -i 'CHES_ENDPOINT_URL' $FILE  | cut -f2 -d'=') 
SERVER_FRONTEND=$(grep -i 'SERVER_FRONTEND' $FILE  | cut -f2 -d'=')
URL_LOGIN_BASIC=$(grep -i 'URL_LOGIN_BASIC' $FILE  | cut -f2 -d'=')
URL_LOGIN_BCSC=$(grep -i 'URL_LOGIN_BCSC' $FILE  | cut -f2 -d'=')
SOAM_KC=$OPENSHIFT_NAMESPACE.pathfinder.gov.bc.ca

STUDENT_PROFILE_FRONTEND=$(grep -i 'STUDENT_PROFILE_FRONTEND' $FILE  | cut -f2 -d'=')
STUDENT_PROFILE_LOGIN_BASIC=$(grep -i 'URL_LOGIN_BASIC' $FILE  | cut -f2 -d'=')
STUDENT_PROFILE_LOGIN_BCSC=$(grep -i 'URL_LOGIN_BCSC' $FILE  | cut -f2 -d'=')

if [ "$envValue" != "prod" ]
then
    SERVER_FRONTEND=https://pen-request-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca
    URL_LOGIN_BASIC=https://pen-request-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/api/auth/login_bceid
    URL_LOGIN_BCSC=https://pen-request-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/api/auth/login_bcsc
    SOAM_KC=$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca

    STUDENT_PROFILE_FRONTEND=https://student-profile-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca
    STUDENT_PROFILE_LOGIN_BASIC=https://student-profile-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/api/auth/login_bceid
    STUDENT_PROFILE_LOGIN_BCSC=https://student-profile-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/api/auth/login_bcsc
fi
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
$KCADM_FILE_BIN_FOLDER/kcadm.sh config credentials --server https://$SOAM_KC/auth --realm $SOAM_KC_REALM_ID --user $SOAM_KC_LOAD_USER_ADMIN

###########################################################
#set siteminder logout url to be used by different UI config maps
###########################################################
siteMinderLogoutUrl=""
if [ "$envValue" = "dev" ] || [ "$envValue" = "test"  ]
then
    siteMinderLogoutUrl="https://logontest7.gov.bc.ca/clp-cgi/logoff.cgi?retnow=1&returl="
else
    siteMinderLogoutUrl="https://logon7.gov.bc.ca/clp-cgi/logoff.cgi?retnow=1&returl="
fi

TZVALUE="America/Vancouver"
###########################################################
#Fetch the public key
###########################################################
getPublicKey(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get keys -r $SOAM_KC_REALM_ID | grep -Po 'publicKey" : "\K([^"]*)'
}

echo Fetching public key from SOAM
soamFullPublicKey="-----BEGIN PUBLIC KEY----- $(getPublicKey) -----END PUBLIC KEY-----"
newline=$'\n'
formattedPublicKey="${soamFullPublicKey:0:26}${newline}${soamFullPublicKey:27:64}${newline}${soamFullPublicKey:91:64}${newline}${soamFullPublicKey:155:64}${newline}${soamFullPublicKey:219:64}${newline}${soamFullPublicKey:283:64}${newline}${soamFullPublicKey:347:64}${newline}${soamFullPublicKey:411:9}${newline}${soamFullPublicKey:420}"

###########################################################
#setup for jwt token to be used by PEN-REQUEST-EMAIL-API and PEN-REQUEST-BACKEND
###########################################################
getSecret(){
head /dev/urandom | tr -dc A-Za-z0-9 | head -c 5000 | base64
}
JWT_SECRET_KEY=$(getSecret)

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
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap soam-sso-config-map --from-literal=TZ=$TZVALUE --from-literal=clientID=soam-kc-service --from-literal=clientSecret=$soamKCServiceClientSecret --from-literal=soamApiURL=https://soam-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=tokenURL=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for sso-$envValue application
oc set env --from=configmap/soam-sso-config-map dc/sso-$envValue

###########################################################
#Setup for digitalid-api-config-map
###########################################################
echo
echo Creating config map digitalid-api-config-map 
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap digitalid-api-config-map --from-literal=TZ=$TZVALUE --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME="$DB_USER_API_DIGITALID" --from-literal=ORACLE_PASSWORD="$DB_PWD_API_DIGITALID" --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for digitalid-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/digitalid-api-config-map dc/digitalid-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for student-api-config-map
###########################################################

getStudentApiServiceClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | python3 -c "import sys, json; data = json.load(sys.stdin); output_dict = [x for x in data if x['clientId'] == 'student-api-service'];  print(output_dict)" | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

getStudentApiServiceClientSecret(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$studentApiServiceClientID/client-secret -r $SOAM_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

echo Fetching client ID for student-api-service client
studentApiServiceClientID=$(getStudentApiServiceClientID)
echo Fetching client secret for student-api-service client
studentApierviceClientSecret=$(getStudentApiServiceClientSecret)

echo
echo Creating config map student-api-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap student-api-config-map --from-literal=TZ=$TZVALUE --from-literal=CLIENT_ID=student-api-service --from-literal=CLIENT_SECRET="$studentApierviceClientSecret" --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME="$DB_USER_API_STUDENT" --from-literal=ORACLE_PASSWORD="$DB_PWD_API_STUDENT" --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=TOKEN_URL=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for student-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/student-api-config-map dc/student-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for pen-request-api-config-map
###########################################################

echo
echo Creating config map pen-request-api-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap pen-request-api-config-map --from-literal=TZ=$TZVALUE --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME="$DB_USER_API_PEN_REQUEST" --from-literal=ORACLE_PASSWORD="$DB_PWD_API_PEN_REQUEST" --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=HIBERNATE_STATISTICS=false --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=FILE_EXTENSIONS="image/jpeg,image/png,application/pdf,.jpg,.jpeg,.jpe,.jfif,.jif,.jfi" --from-literal=FILE_MAXSIZE=10485760 --from-literal=BCSC_AUTO_MATCH_OUTCOMES="RIGHTPEN,WRONGPEN,ZEROMATCHES,MANYMATCHES,ONEMATCH" --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for pen-request-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/pen-request-api-config-map dc/pen-request-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for services-card-api-config-map
###########################################################

echo
echo Creating config map services-card-api-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap services-card-api-config-map --from-literal=TZ=$TZVALUE --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME="$DB_USER_API_SERVICES_CARD" --from-literal=ORACLE_PASSWORD="$DB_PWD_API_SERVICES_CARD" --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for services-card-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/services-card-api-config-map dc/services-card-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for pen-request-email-api-config-map
###########################################################

echo
echo Creating config map pen-request-email-api-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap pen-request-email-api-config-map --from-literal=TZ=$TZVALUE --from-literal=URL_LOGIN_BASIC="$URL_LOGIN_BASIC" --from-literal=URL_LOGIN_BCSC="$URL_LOGIN_BCSC" --from-literal=SOAM_PUBLIC_KEY="$soamFullPublicKey" --from-literal=CHES_CLIENT_ID=$CHES_CLIENT_ID --from-literal=CHES_CLIENT_SECRET=$CHES_CLIENT_SECRET --from-literal=CHES_TOKEN_URL=$CHES_TOKEN_URL --from-literal=JWT_SECRET_KEY="$JWT_SECRET_KEY"  --from-literal=JWT_TOKEN_TTL_IN_MINUTES=1440 --from-literal=CHES_ENDPOINT_URL=$CHES_ENDPOINT_URL --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=EMAIL_TEMPLATE_COMPLETED_REQUEST="<!DOCTYPE html><html><head><meta charset=\"ISO-8859-1\"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello {0},<br><br><b>We have located your PEN</b><br><br>Steps to access your PEN:<ol><li>Click this link <a href={1}>here</a></li><li>Log in using your BCeID (the same method you did when submitting the original request)</li></ol>Note: if your demographic information (name, gender or date of birth) has changed since you last attended a B.C. school, and if you are creating a Student Transcript Services (STS) account to order your transcript from the Ministry of Education, <strong>then please wait until tomorrow morning</strong> for the overnight update to finalize. When you register on STS, be sure you are using your current legal name format and <strong>NOT a maiden name</strong>.  Your transcript will be generated using your current legal name format listed below.<br><br>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href={2}>{3}</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --from-literal=EMAIL_TEMPLATE_COMPLETED_REQUEST_DEMOGRAPHIC_CHANGE="<!DOCTYPE html><html><head><meta charset=\"ISO-8859-1\"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello {0},<br><br><b>We have located your PEN</b><br><br>Steps to access your PEN:<ol><li>Click this link <a href={1}>here</a></li><li>Log in using your BCeID (the same method you did when submitting the original request)</li></ol>Note: Your demographic information (name, gender or date of birth) reported to PEN has been updated since you last attended a B.C. school.  If you are planning on creating a Student Transcript Services (STS) account to order your transcript from the Ministry of Education, <strong>then please wait until tomorrow morning</strong> for the overnight update to finalize before you proceed with STS. When you register on STS, be sure you are using your current legal name format reported to PEN and <strong>NOT a maiden name</strong>.  Your transcript will be generated using your current legal name format noted on your PEN account.<br><br>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href={2}>{3}</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --from-literal=EMAIL_TEMPLATE_REJECTED_REQUEST="<!DOCTYPE html><html><head><meta charset=\"ISO-8859-1\"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello,<br><br><b>Your Personal Education Number (PEN) request could not be fulfilled</b> for the following reason(s):<br><br><b><i>{0}</i></b><br><br>Please review the above reason(s) and the information you provided.<br>If any of the information above is incorrect, you can make another PEN request or contact the <a href="mailto:pens.coordinator@gov.bc.ca">pens.coordinator@gov.bc.ca</a>.<br>To login to GetMyPEN click <a href={1}>here</a> and log in using your BCeID.<br><br>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href={2}>{3}</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --from-literal=EMAIL_TEMPLATE_ADDITIONAL_INFO="<!DOCTYPE html><html><head><meta charset=\"ISO-8859-1\"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello,<br><br><b>Your Personal Education Number (PEN) request is in progress but, we do not have enough information to locate your PEN.</b><br><br>Steps to provide additional information:<ol><li>Click this link <a href={0}>here</a></li><li>Log in using the same method you did when submitting the original request and</li><li>Respond to the additional information request</li></ol>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href={1}>{2}</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --from-literal=EMAIL_TEMPLATE_VERIFY_EMAIL="<!DOCTYPE html><html><head><meta charset=\"ISO-8859-1\"><title>Activate your GetMyPEN request within 24 hours of receiving this email</title></head><body>Hello,<br><br>You have requested your Personal Education Number from the Ministry of Education.<br><br>To get started we need to verify your identity and link your {0} account to your GetMyPEN request.<br><br>You have <b>24 hours</b> after receiving this email to: <ol><li><a href=$SERVER_FRONTEND/api/pen/verification?verificationToken={1}>Activate your GetMyPEN</a> request</li><li>Then, login using the same {2} account</li></ol>If the activation link above doesn't work, please paste this link into your web browser's address field:<br><br><a href=$SERVER_FRONTEND/api/pen/verification?verificationToken={3}>$SERVER_FRONTEND/api/pen/verification?verificationToken={4}</a><br><br>If you are not able to activate your account, you will have to log into GetMyPEN.gov.bc.ca and resend the <b>Verification Email</b>.<br><br>If you have received this message in error, please contact <a href="mailto:pens.coordinator@gov.bc.ca">pens.coordinator@gov.bc.ca</a><br><br>Regards,<br>PEN Coordinator, B.C. Ministry of Education</body></html>" --dry-run -o yaml | oc apply -f -


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
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap soam-api-config-map --from-literal=TZ=$TZVALUE --from-literal=CLIENT_ID=soam-api-service --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=CLIENT_SECRET=$soamAPIServiceClientSecret --from-literal=DIGITALID_URL=https://digitalid-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=STUDENT_URL=https://student-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=SERVICESCARD_API_URL=https://services-card-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=TOKEN_URL=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for soam-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/soam-api-config-map dc/soam-api-$SOAM_KC_REALM_ID
###########################################################
#Setup for pen-request-backend-config-map , make sure the log level is all lower case.
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
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap pen-request-backend-config-map --from-literal=TZ=$TZVALUE --from-literal=UI_PRIVATE_KEY="$UI_PRIVATE_KEY_VAL" --from-literal=UI_PUBLIC_KEY="$UI_PUBLIC_KEY_VAL" --from-literal=SOAM_CLIENT_ID=pen-request-soam --from-literal=SOAM_CLIENT_SECRET=$penRequestServiceClientSecret --from-literal=SERVER_FRONTEND="$SERVER_FRONTEND" --from-literal=ISSUER=PEN_Retrieval_Application --from-literal=PEN_REQUEST_API_ENDPOINT=https://pen-request-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=SOAM_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SOAM_DISCOVERY=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/.well-known/openid-configuration --from-literal=SOAM_URL=https://$SOAM_KC --from-literal=STUDENT_API_ENDPOINT=https://student-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=DIGITALID_API_ENDPOINT=https://digitalid-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=PEN_REQUEST_CLIENT_ID=pen-request-soam --from-literal=PEN_REQUEST_CLIENT_SECRET=$penRequestServiceClientSecret --from-literal=PEN_REQUEST_EMAIL_API_ENDPOINT=https://pen-request-email-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=PEN_REQUEST_EMAIL_SECRET_KEY="$JWT_SECRET_KEY" --from-literal=SITEMINDER_LOGOUT_ENDPOINT="$siteMinderLogoutUrl" --from-literal=STUDENT_DEMOG_API_ENDPOINT=https://pen-demographics-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=LOG_LEVEL=info --from-literal=REDIS_HOST=redis --from-literal=REDIS_PORT=6379 --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for pen-request-backend-$SOAM_KC_REALM_ID application
oc set env --from=configmap/pen-request-backend-config-map dc/pen-request-backend-$SOAM_KC_REALM_ID
oc set env --from=secret/redis dc/pen-request-backend-$SOAM_KC_REALM_ID
###########################################################
#Setup for pen-request-frontend-config-map
###########################################################
bceid_reg_url=""
journey_builder_url=""
meta_data=""
if [ "$envValue" = "dev" ] || [ "$envValue" = "test"  ]
then
    bceid_reg_url="https://www.test.bceid.ca/os/?7081&SkipTo=Basic#action"
    journey_builder_url="https://www2.qa.gov.bc.ca/gov/content/education-training/k-12/support/pen"
    meta_data="[ { name: 'robots', content: 'noindex,nofollow' } ]"
else
    bceid_reg_url="https://www.bceid.ca/os/?7081&SkipTo=Basic#action"
    journey_builder_url="https://www2.gov.bc.ca/gov/content?id=74E29C67215B4988ABCD778F453A3129"
    meta_data="[]"
fi

snowplow="
// <!-- Snowplow starts plowing - Standalone vA.2.10.2 -->
;(function(p,l,o,w,i,n,g){if(!p[i]){p.GlobalSnowplowNamespace=p.GlobalSnowplowNamespace||[];
 p.GlobalSnowplowNamespace.push(i);p[i]=function(){(p[i].q=p[i].q||[]).push(arguments)
 };p[i].q=p[i].q||[];n=l.createElement(o);g=l.getElementsByTagName(o)[0];n.async=1;
 n.src=w;g.parentNode.insertBefore(n,g)}}(window,document,\"script\",\"https://sp-js.apps.gov.bc.ca/MDWay3UqFnIiGVLIo7aoMi4xMC4y.js\",\"snowplow\"));
var collector = 'spt.apps.gov.bc.ca';
 window.snowplow('newTracker','rt',collector, {
  appId: \"Snowplow_standalone\",
  platform: 'web',
  post: true,
  forceSecureTracker: true,
  contexts: {
   webPage: true,
   performanceTiming: true
  }
 });
 window.snowplow('enableActivityTracking', 30, 30); // Ping every 30 seconds after 30 seconds
 window.snowplow('enableLinkClickTracking');
 window.snowplow('trackPageView');
//  <!-- Snowplow stop plowing -->
"

regConfig="var config = (function() {
  return {
    \"VUE_APP_BCEID_REG_URL\" : \"$bceid_reg_url\",
    \"VUE_APP_JOURNEY_BUILDER\" : \"$journey_builder_url\",
    \"VUE_APP_IDLE_TIMEOUT_IN_MILLIS\" : \"1800000\"
    \"VUE_APP_META_DATA\" : \"$meta_data\"
  };
})();"

echo Creating config map pen-request-frontend-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap pen-request-frontend-config-map --from-literal=TZ=$TZVALUE --from-literal=HOST_ROUTE=pen-request-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=config.js="$regConfig" --from-literal=snowplow.js="$snowplow"  --dry-run -o yaml | oc apply -f -
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
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap student-admin-backend-config-map --from-literal=TZ=$TZVALUE --from-literal=UI_PRIVATE_KEY="$UI_PRIVATE_KEY_VAL"  --from-literal=SITEMINDER_LOGOUT_ENDPOINT="$siteMinderLogoutUrl" --from-literal=UI_PUBLIC_KEY="$UI_PUBLIC_KEY_VAL" --from-literal=ID=student-admin-soam --from-literal=SECRET=$studentAdminClientSecret --from-literal=SERVER_FRONTEND=https://student-admin-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=ISSUER=STUDENT_ADMIN_APPLICATION --from-literal=SOAM_PUBLIC_KEY="$formattedPublicKey" --from-literal=PEN_REQUEST_EMAIL_API_URL=https://pen-request-email-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=PEN_REQUEST_API_URL=https://pen-request-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=DISCOVERY=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/.well-known/openid-configuration --from-literal=KC_DOMAIN=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID --from-literal=PEN_DEMOGRAPHICS_URL=https://pen-demographics-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=DIGITAL_ID_URL=https://digitalid-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=STUDENT_API_URL=https://student-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=LOG_LEVEL=info --from-literal=REDIS_HOST=redis --from-literal=REDIS_PORT=6379 --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for student-admin-backend-$SOAM_KC_REALM_ID application
oc set env --from=configmap/student-admin-backend-config-map dc/student-admin-backend-$SOAM_KC_REALM_ID
oc set env --from=secret/redis dc/student-admin-backend-$SOAM_KC_REALM_ID
###########################################################
#Setup for student-admin-frontend-config-map
###########################################################
regConfigStaff="var config = (function() {
  return {
    \"VUE_APP_IDLE_TIMEOUT_IN_MILLIS\" : \"1800000\"
  };
})();"
echo Creating config map student-admin-frontend-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap student-admin-frontend-config-map --from-literal=TZ=$TZVALUE --from-literal=HOST_ROUTE=student-admin-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca  --from-literal=BACKEND_ROOT=https://student-admin-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=config.js="$regConfigStaff"  --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for student-admin-frontend-$SOAM_KC_REALM_ID application
oc set env --from=configmap/student-admin-frontend-config-map dc/student-admin-frontend-$SOAM_KC_REALM_ID
###########################################################
#Setup for pen-demog-api-config-map
###########################################################
echo
echo Creating config map pen-demog-api-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap pen-demog-api-config-map --from-literal=TZ=$TZVALUE --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME="$DB_USER_API_PEN_DEMOGRAPHICS" --from-literal=ORACLE_PASSWORD="$DB_PWD_API_PEN_DEMOGRAPHICS" --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=HIBERNATE_STATISTICS=false --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for pen-demog-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/pen-demog-api-config-map dc/pen-demographics-api-$SOAM_KC_REALM_ID

###########################################################
#Setup for student-profile-email-api-config-map
###########################################################

echo
echo Creating config map student-profile-email-api-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap student-profile-email-api-config-map --from-literal=TZ=$TZVALUE --from-literal=URL_LOGIN_BASIC="$STUDENT_PROFILE_LOGIN_BASIC" --from-literal=URL_LOGIN_BCSC="$STUDENT_PROFILE_LOGIN_BCSC" --from-literal=SOAM_PUBLIC_KEY="$soamFullPublicKey" --from-literal=CHES_CLIENT_ID=$CHES_CLIENT_ID --from-literal=CHES_CLIENT_SECRET=$CHES_CLIENT_SECRET --from-literal=CHES_TOKEN_URL=$CHES_TOKEN_URL --from-literal=JWT_SECRET_KEY="$JWT_SECRET_KEY"  --from-literal=JWT_TOKEN_TTL_IN_MINUTES=1440 --from-literal=CHES_ENDPOINT_URL=$CHES_ENDPOINT_URL --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=EMAIL_TEMPLATE_COMPLETED_REQUEST="<!DOCTYPE html><html><head><meta charset="ISO-8859-1"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello {0},<br><br><b>We have located your PEN</b><br><br>Steps to access your PEN:<ol><li>Click this link <a href={1}>here</a></li><li>Log in using your BCeID (the same method you did when submitting the original request)</li></ol>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href={2}>{3}</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --from-literal=EMAIL_TEMPLATE_REJECTED_REQUEST="<!DOCTYPE html><html><head><meta charset="ISO-8859-1"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello,<br><br><b>Your Personal Education Number (PEN) request could not be fulfilled</b> for the following reason(s):<br><br><b><i>{0}</i></b><br><br>Please review the above reason(s) and the information you provided.<br>If any of the information above is incorrect, you can make another PEN request or contact the <a href="mailto:pens.coordinator@gov.bc.ca">pens.coordinator@gov.bc.ca</a>.<br>To login to GetMyPEN click <a href={1}>here</a> and log in using your BCeID.<br><br>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href={2}>{3}</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --from-literal=EMAIL_TEMPLATE_ADDITIONAL_INFO="<!DOCTYPE html><html><head><meta charset="ISO-8859-1"><title>Your Personal Education Number(PEN) Request</title></head><body>Hello,<br><br><b>Your Personal Education Number (PEN) request is in progress but, we do not have enough information to locate your PEN.</b><br><br>Steps to provide additional information:<ol><li>Click this link <a href={0}>here</a></li><li>Log in using the same method you did when submitting the original request and</li><li>Respond to the additional information request</li></ol>If the above link doesn't work, please paste this link into your web browser's address field:<br><br><a href={1}>{2}</a><br><br>Regards,<br>PEN Team, B.C. Ministry of Education</body></html>" --from-literal=EMAIL_TEMPLATE_VERIFY_EMAIL="<!DOCTYPE html><html><head><meta charset="ISO-8859-1"><title>Activate your GetMyPEN request within 24 hours of receiving this email</title></head><body>Hello,<br><br>You have requested your Personal Education Number from the Ministry of Education.<br><br>To get started we need to verify your identity and link your {0} account to your GetMyPEN request.<br><br>You have <b>24 hours</b> after receiving this email to: <ol><li><a href=$STUDENT_PROFILE_FRONTEND/api/student/verification?verificationToken={1}>Activate your GetMyPEN</a> request</li><li>Then, login using the same {2} account</li></ol>If the activation link above doesn't work, please paste this link into your web browser's address field:<br><br><a href=$STUDENT_PROFILE_FRONTEND/api/student/verification?verificationToken={3}>$STUDENT_PROFILE_FRONTEND/api/student/verification?verificationToken={4}</a><br><br>If you are not able to activate your account, you will have to log into GetMyPEN.gov.bc.ca and resend the <b>Verification Email</b>.<br><br>If you have received this message in error, please contact <a href="mailto:pens.coordinator@gov.bc.ca">pens.coordinator@gov.bc.ca</a><br><br>Regards,<br>PEN Coordinator, B.C. Ministry of Education</body></html>" --dry-run -o yaml | oc apply -f -


echo
echo Setting environment variables for student-profile-email-api-$SOAM_KC_REALM_ID application
oc set env --from=configmap/student-profile-email-api-config-map dc/student-profile-email-api-$SOAM_KC_REALM_ID
###########################################################
#Setup for student-profile-backend-config-map , make sure the log level is all lower case.
###########################################################
getStudentProfileServiceClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | python3 -c "import sys, json; data = json.load(sys.stdin); output_dict = [x for x in data if x['clientId'] == 'student-profile-soam'];  print(output_dict)" | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
getStudentProfileServiceClientSecret(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$studentProfileServiceClientID/client-secret -r $SOAM_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
echo
echo Fetching client ID for student-profile-soam client
studentProfileServiceClientID=$(getStudentProfileServiceClientID)
echo Fetching client secret for student-profile-soam client
studentProfileServiceClientSecret=$(getStudentProfileServiceClientSecret)
echo
echo Generating private and public keys
ssh-keygen -b 4096 -t rsa -f tempStudentProfileBackendkey -q -N ""
STUDENT_PROFILE_UI_PRIVATE_KEY_VAL="$(cat tempStudentProfileBackendkey)"
STUDENT_PROFILE_UI_PUBLIC_KEY_VAL="$(ssh-keygen -f tempStudentProfileBackendkey -e -m pem)"
echo Removing key files
rm tempStudentProfileBackendkey
rm tempStudentProfileBackendkey.pub
echo Creating config map student-profile-backend-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap student-profile-backend-config-map --from-literal=TZ=$TZVALUE --from-literal=UI_PRIVATE_KEY="$STUDENT_PROFILE_UI_PRIVATE_KEY_VAL" --from-literal=UI_PUBLIC_KEY="$STUDENT_PROFILE_UI_PUBLIC_KEY_VAL" --from-literal=SOAM_CLIENT_ID=student-profile-soam --from-literal=SOAM_CLIENT_SECRET=$studentProfileServiceClientSecret --from-literal=SERVER_FRONTEND="$STUDENT_PROFILE_FRONTEND" --from-literal=ISSUER=Student_Profile_Application --from-literal=STUDENT_PROFILE_API_ENDPOINT=https://student-profile-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=SOAM_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SOAM_DISCOVERY=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/.well-known/openid-configuration --from-literal=SOAM_URL=https://$SOAM_KC --from-literal=STUDENT_API_ENDPOINT=https://student-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=DIGITALID_API_ENDPOINT=https://digitalid-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=STUDENT_PROFILE_CLIENT_ID=student-profile-soam --from-literal=STUDENT_PROFILE_CLIENT_SECRET=$studentProfileServiceClientSecret --from-literal=STUDENT_PROFILE_EMAIL_API_ENDPOINT=https://student-profile-email-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=STUDENT_PROFILE_EMAIL_SECRET_KEY="$JWT_SECRET_KEY" --from-literal=SITEMINDER_LOGOUT_ENDPOINT="$siteMinderLogoutUrl" --from-literal=STUDENT_DEMOG_API_ENDPOINT=https://pen-demographics-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=LOG_LEVEL=info --from-literal=REDIS_HOST=redis --from-literal=REDIS_PORT=6379 --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for student-profile-backend-$SOAM_KC_REALM_ID application
oc set env --from=configmap/student-profile-backend-config-map dc/student-profile-backend-$SOAM_KC_REALM_ID
oc set env --from=secret/redis dc/student-profile-backend-$SOAM_KC_REALM_ID
###########################################################
#Setup for student-profile-frontend-config-map
###########################################################
student_profile_bceid_reg_url=""
student_profile_journey_builder_url=""
if [ "$envValue" = "dev" ] || [ "$envValue" = "test"  ]
then
    student_profile_bceid_reg_url="https://www.test.bceid.ca/os/?7081&SkipTo=Basic#action"
    student_profile_journey_builder_url="https://www2.qa.gov.bc.ca/gov/content/education-training/k-12/support/pen"
else
    student_profile_bceid_reg_url="https://www.bceid.ca/os/?7081&SkipTo=Basic#action"
    student_profile_journey_builder_url="https://www2.gov.bc.ca/gov/content?id=74E29C67215B4988ABCD778F453A3129"
fi

student_profile_snowplow="
// <!-- Snowplow starts plowing - Standalone vA.2.10.2 -->
;(function(p,l,o,w,i,n,g){if(!p[i]){p.GlobalSnowplowNamespace=p.GlobalSnowplowNamespace||[];
 p.GlobalSnowplowNamespace.push(i);p[i]=function(){(p[i].q=p[i].q||[]).push(arguments)
 };p[i].q=p[i].q||[];n=l.createElement(o);g=l.getElementsByTagName(o)[0];n.async=1;
 n.src=w;g.parentNode.insertBefore(n,g)}}(window,document,\"script\",\"https://sp-js.apps.gov.bc.ca/MDWay3UqFnIiGVLIo7aoMi4xMC4y.js\",\"snowplow\"));
var collector = 'spt.apps.gov.bc.ca';
 window.snowplow('newTracker','rt',collector, {
  appId: \"Snowplow_standalone\",
  platform: 'web',
  post: true,
  forceSecureTracker: true,
  contexts: {
   webPage: true,
   performanceTiming: true
  }
 });
 window.snowplow('enableActivityTracking', 30, 30); // Ping every 30 seconds after 30 seconds
 window.snowplow('enableLinkClickTracking');
 window.snowplow('trackPageView');
//  <!-- Snowplow stop plowing -->
"

student_profile_regConfig="var config = (function() {
  return {
    \"VUE_APP_BCEID_REG_URL\" : \"$student_profile_bceid_reg_url\",
    \"VUE_APP_JOURNEY_BUILDER\" : \"$student_profile_journey_builder_url\",
    \"VUE_APP_IDLE_TIMEOUT_IN_MILLIS\" : \"1800000\"
  };
})();"

echo Creating config map student-profile-frontend-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap student-profile-frontend-config-map --from-literal=TZ=$TZVALUE --from-literal=HOST_ROUTE=student-profile-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=config.js="$student_profile_regConfig" --from-literal=snowplow.js="$student_profile_snowplow"  --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for student-profile-frontend-$SOAM_KC_REALM_ID application
oc set env --from=configmap/student-profile-frontend-config-map dc/student-profile-frontend-$SOAM_KC_REALM_ID

echo Complete.
