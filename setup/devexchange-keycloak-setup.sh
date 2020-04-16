#########################################################################################
#Create Admin user first via the UI!!!!! This script will not function without this user. 
#########################################################################################

echo This script will setup the target keycloak instance for SOAM configuration
echo Note a user will need to be created in the UI prior to running this script [see properties file]
echo  
echo Which keycloak environment would you like to update? [dev,test,prod]
read envValue

FILE=./properties/setup-$envValue.properties

DEVEXCHANGE_KC_LOAD_USER_ADMIN=$(grep -i 'DEVEXCHANGE_KC_LOAD_USER_ADMIN' $FILE  | cut -f2 -d'=')
KCADM_FILE_BIN_FOLDER=$(grep -i 'KCADM_FILE_BIN_FOLDER' $FILE  | cut -f2 -d'=')
SOAM_KC_REALM_ID=$(grep -i 'SOAM_KC_REALM_ID' $FILE  | cut -f2 -d'=')
OPENSHIFT_NAMESPACE=$(grep -i 'OPENSHIFT_NAMESPACE' $FILE  | cut -f2 -d'=')
DEVEXCHANGE_KC_REALM_ID=$(grep -i 'DEVEXCHANGE_KC_REALM_ID' $FILE  | cut -f2 -d'=')

echo Properties Defined
echo -----------------------------------------------------------
echo DEVEXCHANGE_KC_LOAD_USER_ADMIN: $DEVEXCHANGE_KC_LOAD_USER_ADMIN
echo KCADM_FILE_BIN_FOLDER: $KCADM_FILE_BIN_FOLDER
echo SOAM_KC_REALM_ID: $SOAM_KC_REALM_ID
echo OPENSHIFT_NAMESPACE: $OPENSHIFT_NAMESPACE
echo DEVEXCHANGE_KC_REALM_ID: $DEVEXCHANGE_KC_REALM_ID
echo -----------------------------------------------------------
#########################################################################################
SERVICES_CARD_DNS=id.gov.bc.ca
SSO_ENV=sso.pathfinder.gov.bc.ca
SOAM_KC=$OPENSHIFT_NAMESPACE.pathfinder.gov.bc.ca

if [ "$envValue" != "prod" ]
then
    SERVICES_CARD_DNS=id$envValue.gov.bc.ca
    SSO_ENV=sso-$envValue.pathfinder.gov.bc.ca
    SOAM_KC=$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca
fi


echo Please enter BC Services Card client ID for SSO BCDevExchange:
read bcscClientID
echo Please enter BC Services Card client secret for SSO BCDevExchange:
read -s bcscClientSecret

echo Logging in
$KCADM_FILE_BIN_FOLDER/kcadm.sh config credentials --server https://$SSO_ENV/auth --realm $DEVEXCHANGE_KC_REALM_ID --user $DEVEXCHANGE_KC_LOAD_USER_ADMIN

echo Updating realm details
$KCADM_FILE_BIN_FOLDER/kcadm.sh update realms/$DEVEXCHANGE_KC_REALM_ID --body "{\"loginWithEmailAllowed\" : false, \"duplicateEmailsAllowed\" : true}"

echo Updating First Broker Login executers
getFirstBrokerLoginRegistrationExecuterID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get -r $DEVEXCHANGE_KC_REALM_ID authentication/flows/first%20broker%20login/executions | jq -r ".[] | select(.providerId == \"idp-review-profile\") | .id"
}

FIRST_BROKER_EXECUTER_ID=$(getFirstBrokerLoginRegistrationExecuterID)
$KCADM_FILE_BIN_FOLDER/kcadm.sh update authentication/flows/first%20broker%20login/executions -r $DEVEXCHANGE_KC_REALM_ID --body "{ \"id\" : \"$FIRST_BROKER_EXECUTER_ID\", \"requirement\" : \"DISABLED\", \"displayName\" : \"Review Profile\", \"alias\" : \"review profile config\", \"requirementChoices\" : [ \"REQUIRED\", \"DISABLED\" ], \"configurable\" : true, \"providerId\" : \"idp-review-profile\", \"authenticationConfig\" : \"0ee684cd-7ce1-4278-9477-d40d1a3486bf\", \"level\" : 0, \"index\" : 0}" 

echo Removing BCSC IDP if exists...
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete identity-provider/instances/bcsc -r $DEVEXCHANGE_KC_REALM_ID

echo Creating BC Services Card IDP...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances -r $DEVEXCHANGE_KC_REALM_ID --body "{\"alias\" : \"bcsc\",\"displayName\" : \"BC Services Card\",\"providerId\" : \"oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"firstBrokerLoginFlowAlias\" : \"first broker login\",\"config\" : {\"hideOnLoginPage\" : \"\",\"userInfoUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"$bcscClientID\",\"tokenUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/token\",\"uiLocales\" : \"\",\"jwksUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/jwk.json\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://$SERVICES_CARD_DNS/oauth2/\",\"useJwksUrl\" : \"true\",\"loginHint\" : \"\",\"authorizationUrl\" : \"https://$SERVICES_CARD_DNS/login/oidc/authorize\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"\",\"clientSecret\" : \"$bcscClientSecret\",\"prompt\" : \"\",\"defaultScope\" : \"openid profile email address\"}}"

echo Creating mappers for BC Services Card DevExchange IDP...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"First Name\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_name\",\"user.attribute\" : \"firstName\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Email\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"email\",\"user.attribute\" : \"email\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Gender\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"gender\",\"user.attribute\" : \"gender\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"User Type\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"user_type\",\"user.attribute\" : \"user_type\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"hardcoded-attribute-idp-mapper\",\"config\" : {\"attribute.value\" : \"bcsc\",\"attribute\" : \"account_type\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Display Name\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"display_name\",\"user.attribute\" : \"display_name\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Region\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.region\",\"user.attribute\" : \"region\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Given Names\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_names\",\"user.attribute\" : \"given_names\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Given Name\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_name\",\"user.attribute\" : \"given_name\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Street Address\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.street_address\",\"user.attribute\" : \"street_address\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Postal Code\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.postal_code\",\"user.attribute\" : \"postal_code\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Country\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.country\",\"user.attribute\" : \"country\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Birthdate\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"birthdate\",\"user.attribute\" : \"birthdate\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Locality\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.locality\",\"user.attribute\" : \"locality\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Directed Identifier\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"sub\",\"user.attribute\" : \"did\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Age\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"age\",\"user.attribute\" : \"age\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Identity Assurance Level\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"identity_assurance_level\",\"user.attribute\" : \"identity_assurance_level\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Last Name\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"family_name\",\"user.attribute\" : \"lastName\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Username DID\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-username-idp-mapper\",\"config\" : {\"template\" : \"\${CLAIM.sub}@\${ALIAS}\"}}"

echo Creating mappers for IDIR DevExchange IDP if not exist... 

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/idir/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"idir\",\"identityProviderMapper\" : \"hardcoded-attribute-idp-mapper\",\"config\" : {\"attribute.value\" : \"idir\",\"attribute\" : \"account_type\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/idir/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"idir_userid\",\"identityProviderAlias\" : \"idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"idir_userid\",\"user.attribute\" : \"idir_userid\"}}"

echo Creating mappers for BCeID DevExchange IDP if not exist...

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bceid/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"bceid\",\"identityProviderMapper\" : \"hardcoded-attribute-idp-mapper\",\"config\" : {\"attribute.value\" : \"bceid\",\"attribute\" : \"account_type\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bceid/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"bceid_userid\",\"identityProviderAlias\" : \"bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"bceid_userid\",\"user.attribute\" : \"bceid_userid\"}}"

echo Creating SOAM client
$KCADM_FILE_BIN_FOLDER/kcadm.sh create clients -r $DEVEXCHANGE_KC_REALM_ID --body "{\"clientId\" : \"soam\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_bceid/endpoint/logout_response\", \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_bceid/endpoint\", \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_idir/endpoint\", \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_idir/endpoint/logout_response\", \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_bcsc/endpoint\", \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_bcsc/endpoint/logout_response\" ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : true,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : false,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"IDIR GUID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"idir_userid\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"idir_guid\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"display_name\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"displayName\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"display_name\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"BCSC DID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"did\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"bcsc_did\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"BCeID GUID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"bceid_userid\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"bceid_guid\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Identity Assurance Level\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"identity_assurance_level\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"identity_assurance_level\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Region\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"region\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"region\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Locality\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"locality\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"locality\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Street Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"street_address\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"street_address\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Country\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"country\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"country\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Postal Code\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"postal_code\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"postal_code\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"username\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-property-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"username\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"preferred_username\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Account Type\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"account_type\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"account_type\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Given Names\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"given_names\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"given_names\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\", \"role_list\", \"profile\", \"roles\", \"email\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"
echo Complete.