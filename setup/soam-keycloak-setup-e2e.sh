echo This script will setup the target keycloak instance for SOAM configuration to be run in pipeline for E2E
echo Note a user will need to be created prior to running this script
echo
echo Which keycloak environment would you like to update? [dev, test]
read -r envValue

echo "Env Value: ${envValue}"

if [ -n "$1" ]; then
  envValue=$1
  echo "$envValue"
else
  echo "Environment value set by user"
fi

if [ "$envValue" == "dev" ] || [ "$envValue" == "test" ]; then
  FILE=./properties/setup-$envValue.properties

  SOAM_KC_LOAD_USER_ADMIN=$(grep -i 'SOAM_KC_LOAD_USER_ADMIN' "$FILE" | cut -f2 -d'=')
  SOAM_KC_LOAD_USER_PASS=$(grep -i 'SOAM_KC_LOAD_USER_PASS' "$FILE" | cut -f2 -d'=')
  KCADM_FILE_BIN_FOLDER=$(grep -i 'KCADM_FILE_BIN_FOLDER' "$FILE" | cut -f2 -d'=')
  SOAM_KC_REALM_ID=$(grep -i 'SOAM_KC_REALM_ID' "$FILE" | cut -f2 -d'=')
  OPENSHIFT_NAMESPACE=$(grep -i 'OPENSHIFT_NAMESPACE' "$FILE" | cut -f2 -d'=')
  echo Properties Defined
  echo -----------------------------------------------------------
  echo SOAM_KC_LOAD_USER_ADMIN: "$SOAM_KC_LOAD_USER_ADMIN"
  echo KCADM_FILE_BIN_FOLDER: "$KCADM_FILE_BIN_FOLDER"
  echo SOAM_KC_REALM_ID: "$SOAM_KC_REALM_ID"
  echo OPENSHIFT_NAMESPACE: "$OPENSHIFT_NAMESPACE"
  echo -----------------------------------------------------------
  #########################################################################################

  echo Logging in
  "$KCADM_FILE_BIN_FOLDER"/kcadm.sh config credentials --server https://"$OPENSHIFT_NAMESPACE"-"$envValue".pathfinder.gov.bc.ca/auth --realm "$SOAM_KC_REALM_ID" --user "$SOAM_KC_LOAD_USER_ADMIN" --password "$SOAM_KC_LOAD_USER_PASS"

  echo Creating Client Scopes

  #DELETE DIGITAL ID
  "$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r "$SOAM_KC_REALM_ID" --body "{\"description\": \"Delete scope for Digital ID\",\"id\": \"DELETE_DIGITALID\",\"name\": \"DELETE_DIGITALID\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
  #DELETE PEN REQUEST
  "$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r "$SOAM_KC_REALM_ID" --body "{\"description\": \"Delete scope for Pen Request\",\"id\": \"DELETE_PEN_REQUEST\",\"name\": \"DELETE_PEN_REQUEST\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
  #DELETE SERVICES CARD
  "$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r "$SOAM_KC_REALM_ID" --body "{\"description\": \"Delete scope for Services Card\",\"id\": \"DELETE_SERVICES_CARD\",\"name\": \"DELETE_SERVICES_CARD\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
  #DELETE STUDENT
  "$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r "$SOAM_KC_REALM_ID" --body "{\"description\": \"Delete scope for Student\",\"id\": \"DELETE_STUDENT\",\"name\": \"DELETE_STUDENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

  #Clients----------------------------------------------------------
  echo Creating clients
  echo Creating automated-e2e-soam Keycloak client
  "$KCADM_FILE_BIN_FOLDER"/kcadm.sh create clients -r "$SOAM_KC_REALM_ID" --body "{\"clientId\" : \"automated-e2e-soam\",  \"name\" : \"Automation E2E SOAM\",  \"description\" : \"Connect user from Automated E2E Testing  to SOAM to perform delete operation on the APIs\",  \"surrogateAuthRequired\" : false,  \"enabled\" : true,  \"clientAuthenticatorType\" : \"client-secret\",  \"webOrigins\" : [ ],  \"notBefore\" : 0,  \"bearerOnly\" : false,  \"consentRequired\" : false,  \"standardFlowEnabled\" : true,  \"implicitFlowEnabled\" : false,  \"directAccessGrantsEnabled\" : false,  \"serviceAccountsEnabled\" : true,  \"publicClient\" : false,  \"frontchannelLogout\" : false,  \"protocol\" : \"openid-connect\",  \"attributes\" : { \"saml.assertion.signature\" : \"false\", \"saml.multivalued.roles\" : \"false\", \"saml.force.post.binding\" : \"false\", \"saml.encrypt\" : \"false\", \"saml.server.signature\" : \"false\", \"saml.server.signature.keyinfo.ext\" : \"false\", \"exclude.session.state.from.auth.response\" : \"false\", \"saml_force_name_id_format\" : \"false\", \"saml.client.signature\" : \"false\", \"tls.client.certificate.bound.access.tokens\" : \"false\", \"saml.authnstatement\" : \"false\", \"display.on.consent.screen\" : \"false\", \"saml.onetimeuse.condition\" : \"false\"  },  \"authenticationFlowBindingOverrides\" : { },  \"fullScopeAllowed\" : true,  \"nodeReRegistrationTimeout\" : -1,  \"defaultClientScopes\" : [  \"DELETE_DIGITALID\",\"DELETE_SERVICES_CARD\" ,\"DELETE_PEN_REQUEST\" ,\"DELETE_STUDENT\"  ],  \"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],  \"access\" : { \"view\" : true, \"configure\" : true, \"manage\" : true  }}"
  echo Complete.

else
  echo Invalid environment, should be dev or test only.
fi
