echo This script will setup the target keycloak instance for SOAM configuration
echo Note a user will need to be created prior to running this script
echo  
echo Which keycloak environment would you like to update? [dev,test,prod]
read envValue

FILE=./properties/setup-$envValue.properties

SOAM_KC_LOAD_USER_ADMIN=$(grep -i 'SOAM_KC_LOAD_USER_ADMIN' $FILE  | cut -f2 -d'=')
KCADM_FILE_BIN_FOLDER=$(grep -i 'KCADM_FILE_BIN_FOLDER' $FILE  | cut -f2 -d'=')
SOAM_KC_REALM_ID=$(grep -i 'SOAM_KC_REALM_ID' $FILE  | cut -f2 -d'=')
OPENSHIFT_NAMESPACE=$(grep -i 'OPENSHIFT_NAMESPACE' $FILE  | cut -f2 -d'=')
DEVEXCHANGE_KC_REALM_ID=$(grep -i 'DEVEXCHANGE_KC_REALM_ID' $FILE  | cut -f2 -d'=')

echo Properties Defined
echo -----------------------------------------------------------
echo SOAM_KC_LOAD_USER_ADMIN: $SOAM_KC_LOAD_USER_ADMIN
echo KCADM_FILE_BIN_FOLDER: $KCADM_FILE_BIN_FOLDER
echo SOAM_KC_REALM_ID: $SOAM_KC_REALM_ID
echo OPENSHIFT_NAMESPACE: $OPENSHIFT_NAMESPACE
echo DEVEXCHANGE_KC_REALM_ID: $DEVEXCHANGE_KC_REALM_ID
echo -----------------------------------------------------------
#########################################################################################

echo Please enter client secret for soam user in your BCDevExchange Keycloak realm:
read -s soamClientSecret
echo Thank you.

echo Logging in
$KCADM_FILE_BIN_FOLDER/kcadm.sh config credentials --server https://$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/auth --realm $SOAM_KC_REALM_ID --user $SOAM_KC_LOAD_USER_ADMIN

echo Updating realm details
$KCADM_FILE_BIN_FOLDER/kcadm.sh update realms/$SOAM_KC_REALM_ID --body "{\"loginWithEmailAllowed\" : false, \"duplicateEmailsAllowed\" : true, \"accessTokenLifespan\" : 1800}"

echo Creating STUDENT_ADMIN role
$KCADM_FILE_BIN_FOLDER/kcadm.sh create roles -r $SOAM_KC_REALM_ID --body "{\"name\" : \"STUDENT_ADMIN\",\"description\" : \"Allows access to staff site\",\"composite\" : false,\"clientRole\" : false,\"containerId\" : \"$SOAM_KC_REALM_ID\"}"
 
echo Creating Client Scopes
#READ_CODETABLE_SET
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Read scope for code tables\",\"id\": \"READ_CODETABLE_SET\",\"name\": \"READ_CODETABLE_SET\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#READ_DIGITALID
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Read scope for digital ID\",\"id\": \"READ_DIGITALID\",\"name\": \"READ_DIGITALID\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#WRITE_DIGITALID
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Write scope for digital ID\",\"id\": \"WRITE_DIGITALID\",\"name\": \"WRITE_DIGITALID\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#READ_PEN_REQUEST
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Read scope for PEN request\",\"id\": \"READ_PEN_REQUEST\",\"name\": \"READ_PEN_REQUEST\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#WRITE_PEN_REQUEST
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Write scope for PEN request\",\"id\": \"WRITE_PEN_REQUEST\",\"name\": \"WRITE_PEN_REQUEST\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#READ_STUDENT
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Read scope for student\",\"id\": \"READ_STUDENT\",\"name\": \"READ_STUDENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#WRITE_STUDENT
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Write scope for student\",\"id\": \"WRITE_STUDENT\",\"name\": \"WRITE_STUDENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#SOAM_LOGIN
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"SOAM login scope\",\"id\": \"SOAM_LOGIN\",\"name\": \"SOAM_LOGIN\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#SEND_EMAIL
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"SOAM send email scope\",\"id\": \"SEND_EMAIL\",\"name\": \"SEND_EMAIL\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#DELETE_DOCUMENT
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"SOAM send email scope\",\"id\": \"DELETE_DOCUMENT\",\"name\": \"DELETE_DOCUMENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#READ_DOCUMENT
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"SOAM send email scope\",\"id\": \"READ_DOCUMENT\",\"name\": \"READ_DOCUMENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#READ_DOCUMENT_REQUIREMENTS
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"SOAM send email scope\",\"id\": \"READ_DOCUMENT_REQUIREMENTS\",\"name\": \"READ_DOCUMENT_REQUIREMENTS\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#WRITE_DOCUMENT
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"SOAM send email scope\",\"id\": \"WRITE_DOCUMENT\",\"name\": \"WRITE_DOCUMENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
#WRITE_DOCUMENT_OWNER
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"SOAM send email scope\",\"id\": \"WRITE_DOCUMENT_OWNER\",\"name\": \"WRITE_DOCUMENT_OWNER\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"



#Authenticators-----------------------------------------------------------
echo Creating authenticators
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows -r $SOAM_KC_REALM_ID  --body "{\"alias\" : \"SOAMFirstLogin\",\"providerId\" : \"basic-flow\",\"topLevel\" : true,\"builtIn\" : false}"
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows -r $SOAM_KC_REALM_ID  --body "{\"alias\" : \"SOAMPostLogin\",\"providerId\" : \"basic-flow\",\"topLevel\" : true,\"builtIn\" : false}"
echo Creating executors
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows/SOAMPostLogin/executions/execution -r $SOAM_KC_REALM_ID -s provider=bcgov-soam-post-authenticator
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows/SOAMFirstLogin/executions/execution -r $SOAM_KC_REALM_ID -s provider=bcgov-soam-authenticator

getSoamFirstLoginExecutorID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMFirstLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)'
}

soamFirstLoginExecutorID=$(getSoamFirstLoginExecutorID)
echo Updating first login executor to required
$KCADM_FILE_BIN_FOLDER/kcadm.sh update authentication/flows/SOAMFirstLogin/executions -r $SOAM_KC_REALM_ID  --body "{$soamFirstLoginExecutorID \"configurable\": false,\"displayName\": \"SOAM Authenticator\",\"index\": 0,\"level\": 0,\"providerId\": \"bcgov-soam-authenticator\",\"requirement\": \"REQUIRED\",\"requirementChoices\": [\"ALTERNATIVE\", \"REQUIRED\", \"DISABLED\"]}"

getSoamPostLoginExecutorID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMPostLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)'
}

soamPostLoginExecutorID=$(getSoamPostLoginExecutorID)
echo Updating post login executor to required
$KCADM_FILE_BIN_FOLDER/kcadm.sh update authentication/flows/SOAMPostLogin/executions -r $SOAM_KC_REALM_ID  --body "{$soamPostLoginExecutorID \"configurable\": false,\"displayName\": \"SOAM Authenticator\",\"index\": 0,\"level\": 0,\"providerId\": \"bcgov-soam-authenticator\",\"requirement\": \"REQUIRED\",\"requirementChoices\": [\"ALTERNATIVE\", \"REQUIRED\", \"DISABLED\"]}"

#Identity Providers------------------------------------------------
echo Creating DevExchange IDP
echo Building IDP instance...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances -r $SOAM_KC_REALM_ID --body "{\"alias\" : \"keycloak_bcdevexchange\",\"displayName\" : \"BCDevExchange Keycloak\",\"providerId\" : \"keycloak-oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : true,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"firstBrokerLoginFlowAlias\" : \"SOAMFirstLogin\",\"postBrokerLoginFlowAlias\" : \"SOAMPostLogin\",\"config\" : { \"hideOnLoginPage\" : \"true\",\"userInfoUrl\" : \"https://sso-$envValue.pathfinder.gov.bc.ca/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/userinfo\",\"validateSignature\" : \"false\",\"clientId\" : \"soam\",\"tokenUrl\" : \"https://sso-$envValue.pathfinder.gov.bc.ca/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/token\",\"uiLocales\" : \"\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://sso-$envValue.pathfinder.gov.bc.ca/auth/realms/$DEVEXCHANGE_KC_REALM_ID\",\"useJwksUrl\" : \"true\",\"loginHint\": \"\",\"authorizationUrl\" : \"https://sso-$envValue.pathfinder.gov.bc.ca/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/auth\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"https://sso-$envValue.pathfinder.gov.bc.ca/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/logout\",\"clientSecret\" : \"$soamClientSecret\",\"prompt\": \"\",\"defaultScope\" : \"openid profile email address\"}}"
echo Creating mappers for IDP...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"account_type\",\"user.attribute\" : \"account_type\"}}"
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"BCeID GUID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"bceid_userid\",\"user.attribute\" : \"bceid_guid\"}}"
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"IDIR GUID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"idir_userid\",\"user.attribute\" : \"idir_guid\"}}"
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"BCSC DID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"did\",\"user.attribute\" : \"bcsc_did\"}}"

#Clients----------------------------------------------------------
echo Creating clients

echo Creating soam-kc-service client
$KCADM_FILE_BIN_FOLDER/kcadm.sh create clients -r $SOAM_KC_REALM_ID --body "{\"clientId\" : \"soam-kc-service\",\"name\" : \"SOAM Keycloak Service Account\",\"description\" : \"Client to call from SOAM KC to SOAM API\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {  \"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",  \"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { }, \"fullScopeAllowed\" : true, \"nodeReRegistrationTimeout\" : -1, \"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\", \"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\", \"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,   \"config\" : {\"user.session.note\" : \"clientHost\", \"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\", \"role_list\", \"profile\", \"roles\", \"SOAM_LOGIN\", \"email\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

echo Creating pen-request-soam Keycloak client
$KCADM_FILE_BIN_FOLDER/kcadm.sh create clients -r $SOAM_KC_REALM_ID --body "{\"clientId\" : \"pen-request-soam\",  \"name\" : \"PEN Request SOAM\",  \"description\" : \"Connect user from PEN request backend to the SOAM\",  \"surrogateAuthRequired\" : false,  \"enabled\" : true,  \"clientAuthenticatorType\" : \"client-secret\",  \"redirectUris\" : [ \"https://pen-request-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca\", \"https://pen-request-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/api/auth/callback\" ],  \"webOrigins\" : [ ],  \"notBefore\" : 0,  \"bearerOnly\" : false,  \"consentRequired\" : false,  \"standardFlowEnabled\" : true,  \"implicitFlowEnabled\" : false,  \"directAccessGrantsEnabled\" : false,  \"serviceAccountsEnabled\" : false,  \"publicClient\" : false,  \"frontchannelLogout\" : false,  \"protocol\" : \"openid-connect\",  \"attributes\" : { \"saml.assertion.signature\" : \"false\", \"saml.multivalued.roles\" : \"false\", \"saml.force.post.binding\" : \"false\", \"saml.encrypt\" : \"false\", \"saml.server.signature\" : \"false\", \"saml.server.signature.keyinfo.ext\" : \"false\", \"exclude.session.state.from.auth.response\" : \"false\", \"saml_force_name_id_format\" : \"false\", \"saml.client.signature\" : \"false\", \"tls.client.certificate.bound.access.tokens\" : \"false\", \"saml.authnstatement\" : \"false\", \"display.on.consent.screen\" : \"false\", \"saml.onetimeuse.condition\" : \"false\"  },  \"authenticationFlowBindingOverrides\" : { },  \"fullScopeAllowed\" : true,  \"nodeReRegistrationTimeout\" : -1,  \"protocolMappers\" : [ { \"name\" : \"last_name\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-usermodel-attribute-mapper\", \"consentRequired\" : false, \"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"last_name\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"last_name\",\"jsonType.label\" : \"String\" }  }, { \"name\" : \"first_name\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-usermodel-attribute-mapper\", \"consentRequired\" : false, \"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"first_name\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"first_name\",\"jsonType.label\" : \"String\" }  }, { \"name\" : \"middle_names\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-usermodel-attribute-mapper\", \"consentRequired\" : false, \"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"middle_names\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"middle_names\",\"jsonType.label\" : \"String\" }  }, { \"name\" : \"SOAM Mapper\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-soam-mapper\", \"consentRequired\" : false, \"config\" : {\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"userinfo.token.claim\" : \"true\" }  }, { \"name\" : \"idir_guid\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-usermodel-attribute-mapper\", \"consentRequired\" : false, \"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"idir_guid\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"idir_guid\",\"jsonType.label\" : \"String\" }  }, { \"name\" : \"bceid_guid\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-usermodel-attribute-mapper\", \"consentRequired\" : false, \"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"bceid_guid\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"bceid_guid\",\"jsonType.label\" : \"String\" }  }, { \"name\" : \"email_address\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-usermodel-attribute-mapper\", \"consentRequired\" : false, \"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"email_address\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"email_address\",\"jsonType.label\" : \"String\" }  } ],  \"defaultClientScopes\" : [ \"web-origins\", \"role_list\", \"READ_CODETABLE_SET\", \"WRITE_PEN_REQUEST\", \"profile\", \"roles\", \"email\", \"READ_PEN_REQUEST\" ],  \"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],  \"access\" : { \"view\" : true, \"configure\" : true, \"manage\" : true  }}"

echo Creating soam-api-service Keycloak client
$KCADM_FILE_BIN_FOLDER/kcadm.sh create clients -r $SOAM_KC_REALM_ID --body "{\"clientId\" : \"soam-api-service\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\", \"WRITE_STUDENT\", \"role_list\", \"READ_CODETABLE_SET\", \"WRITE_DIGITALID\", \"profile\", \"roles\", \"READ_STUDENT\", \"email\", \"READ_DIGITALID\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

echo Creating student-admin-soam Keycloak client
$KCADM_FILE_BIN_FOLDER/kcadm.sh create clients -r $SOAM_KC_REALM_ID --body "{\"clientId\" : \"student-admin-soam\",\"name\" : \"Student Admin SOAM\",\"description\" : \"Student admin user which logs into SOAM\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ \"https://student-admin-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca/api/auth/callback\" ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : true,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : false,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"defaultClientScopes\" : [ \"web-origins\", \"role_list\", \"READ_CODETABLE_SET\", \"WRITE_PEN_REQUEST\", \"profile\", \"roles\", \"email\", \"READ_PEN_REQUEST\", \"SEND_EMAIL\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

echo Complete. 
