/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.bcgov.keycloak.soam;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SoamFirstTimeLoginAuthenticator extends AbstractIdpAuthenticator {

    private static Logger logger = Logger.getLogger(SoamFirstTimeLoginAuthenticator.class);


    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    }

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    	logger.info("SOAM: inside authenticateImpl");
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        if (context.getAuthenticationSession().getAuthNote(EXISTING_USER_INFO) != null) {
            context.attempted();
            return;
        }
        
        logger.info("Context values: ");
        for(String s: brokerContext.getContextData().keySet()) {
        	logger.info("Context data key: " + s + " value: " + brokerContext.getContextData().get(s));	
        }
        
        JsonWebToken token = (JsonWebToken)brokerContext.getContextData().get("VALIDATED_ID_TOKEN");
        
        logger.info("JWT print: " + token.toString());
        logger.info("JWT print guid: " + token.getSubject());
        logger.info("Claims: ");
        for(String s: token.getOtherClaims().keySet()) {
        	logger.info("Claim key: " + s + " value: " + token.getOtherClaims().get(s));	
        }
        
        
        String userIdAttrName =brokerContext.getIdpConfig().getAlias()+ "_user_guid";
        String usernameAttrName =brokerContext.getIdpConfig().getAlias()+ "_username";
        String userIdAttrValue = brokerContext.getUserAttribute(userIdAttrName);
        logger.info("User GUID: " + userIdAttrValue);
        
        String username = getUsername(context, serializedCtx, brokerContext);
        if (username == null) {
            ServicesLogger.LOGGER.resetFlow(realm.isRegistrationEmailAsUsername() ? "Email" : "Username");
            context.getAuthenticationSession().setAuthNote(ENFORCE_UPDATE_PROFILE, "true");
            context.resetFlow();
            return;
        }

        ExistingUserInfo duplication = checkExistingUser(context, username, serializedCtx, brokerContext);

        if (duplication == null) {
            logger.debugf("No duplication detected. Creating account for user '%s' and linking with identity provider '%s' .",
                    username, brokerContext.getIdpConfig().getAlias());

            UserModel federatedUser = session.users().addUser(realm, username);
            federatedUser.setEnabled(true);
            federatedUser.setEmail(brokerContext.getEmail());
            federatedUser.setFirstName(brokerContext.getFirstName());
            federatedUser.setLastName(brokerContext.getLastName());

            for (Map.Entry<String, List<String>> attr : serializedCtx.getAttributes().entrySet()) {
                federatedUser.setAttribute(attr.getKey(), attr.getValue());
            }

            federatedUser.setSingleAttribute(userIdAttrName, userIdAttrValue);
            
            federatedUser.setSingleAttribute("PEN", "123456789");
            federatedUser.setSingleAttribute("AssuranceLvl", "2");
            federatedUser.setSingleAttribute("Address", "2033 Someplace, Somewhere, BC");
            federatedUser.setSingleAttribute("DOB", "1990-10-16");

            //AuthenticatorConfigModel config = context.getAuthenticatorConfig();
            //if (config != null && Boolean.parseBoolean(config.getConfig().get(IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION))) {
            //    logger.debugf("User '%s' required to update password", federatedUser.getUsername());
            //    federatedUser.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
            //}

            userRegisteredSuccess(context, federatedUser, serializedCtx, brokerContext);

            context.setUser(federatedUser);
            context.getAuthenticationSession().setAuthNote(BROKER_REGISTERED_NEW_USER, "true");
            context.success();
        } else if (userIdAttrName.equalsIgnoreCase(duplication.getDuplicateAttributeName()) || usernameAttrName.equalsIgnoreCase(duplication.getDuplicateAttributeName())){
        	UserModel existingUser = context.getSession().users().getUserById(duplication.getExistingUserId(), realm);
        	if (usernameAttrName.equalsIgnoreCase(duplication.getDuplicateAttributeName())) {
        		existingUser.removeAttribute(usernameAttrName);
        		existingUser.setSingleAttribute(userIdAttrName, userIdAttrValue);
        	}
        	
        	context.setUser(existingUser);
        	context.success();
        } else {
            logger.debugf("Duplication detected. There is already existing user with %s '%s' .",
                    duplication.getDuplicateAttributeName(), duplication.getDuplicateAttributeValue());

            // Set duplicated user, so next authenticators can deal with it
            context.getAuthenticationSession().setAuthNote(EXISTING_USER_INFO, duplication.serialize());

            Response challengeResponse = context.form()
                    .setError(Messages.FEDERATED_IDENTITY_EXISTS, duplication.getDuplicateAttributeName(), duplication.getDuplicateAttributeValue())
                    .createErrorPage(Response.Status.CONFLICT);
            context.challenge(challengeResponse);

            if (context.getExecution().isRequired()) {
                context.getEvent()
                        .user(duplication.getExistingUserId())
                        .detail("existing_" + duplication.getDuplicateAttributeName(), duplication.getDuplicateAttributeValue())
                        .removeDetail(Details.AUTH_METHOD)
                        .removeDetail(Details.AUTH_TYPE)
                        .error(Errors.FEDERATED_IDENTITY_EXISTS);
            }
        }
    }

    // Could be overriden to detect duplication based on other criterias (firstName, lastName, ...)
    protected ExistingUserInfo checkExistingUser(AuthenticationFlowContext context, String username, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    	//brokerContext.getBrokerUserId()
    	//context.getSession().users().searchForUserByUserAttribute(attrName, attrValue, realm)
    	logger.info("SOAM: inside checkExistingUser");
    	// check by IdP userid
//        String userIdAttrName =brokerContext.getIdpConfig().getAlias()+ "_user_guid";
//    	String userIdAttrValue = brokerContext.getUserAttribute(userIdAttrName);
//    	logger.info("User GUID: " + userIdAttrValue);
//    	List<UserModel> existingUserByAttr=context.getSession().users().searchForUserByUserAttribute(userIdAttrName, userIdAttrValue, context.getRealm());
//    	if (existingUserByAttr.size() == 1) {
//    		return new ExistingUserInfo(existingUserByAttr.get(0).getId(), userIdAttrName, userIdAttrValue);
//    	}
//    	
//    	//Check by IdP username
//    	String usernameAttrName =brokerContext.getIdpConfig().getAlias()+ "_username";
//    	existingUserByAttr=context.getSession().users().searchForUserByUserAttribute(usernameAttrName, username, context.getRealm());
//    	if (existingUserByAttr.size() == 1) {
//    		return new ExistingUserInfo(existingUserByAttr.get(0).getId(), usernameAttrName, username);
//    	}
//    	
//        if (brokerContext.getEmail() != null && !context.getRealm().isDuplicateEmailsAllowed()) {
//            UserModel existingUser = context.getSession().users().getUserByEmail(brokerContext.getEmail(), context.getRealm());
//            if (existingUser != null) {
//                return new ExistingUserInfo(existingUser.getId(), UserModel.EMAIL, existingUser.getEmail());
//            }
//        }
//
//        UserModel existingUser = context.getSession().users().getUserByUsername(username, context.getRealm());
//        if (existingUser != null) {
//            return new ExistingUserInfo(existingUser.getId(), UserModel.USERNAME, existingUser.getUsername());
//        }

        return null;
    }

    protected String getUsername(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    	logger.info("SOAM: inside getUsername");
        RealmModel realm = context.getRealm();
        String val = realm.isRegistrationEmailAsUsername() ? brokerContext.getEmail() : brokerContext.getModelUsername();
        logger.info("Username value: " + val);

        return realm.isRegistrationEmailAsUsername() ? brokerContext.getEmail() : brokerContext.getModelUsername();
    }


    // Empty method by default. This exists, so subclass can override and add callback after new user is registered through social
    protected void userRegisteredSuccess(AuthenticationFlowContext context, UserModel registeredUser, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    	logger.info("SOAM: inside userRegisteredSuccess");
    	logger.info("User Model: ");
    	logger.info(registeredUser.getEmail());
    	logger.info(registeredUser.getFirstName());
    	logger.info(registeredUser.getId());
    	logger.info(registeredUser.getServiceAccountClientLink());
		logger.info("Attributes for user: ");
		for(String s: registeredUser.getAttributes().keySet()) {
			logger.info("Key: " + s + " Value: " + registeredUser.getAttributes().get(s) + "\n");	
		}
    	logger.info(registeredUser.getUsername());
    	logger.info("Broker User " + brokerContext.getUsername());
    	logger.info("Broker ID " + brokerContext.getId());
    	logger.info("Broker Model User " + brokerContext.getModelUsername());
        String userIdAttrName =brokerContext.getIdpConfig().getAlias()+ "_user_guid";
    	String userIdAttrValue = brokerContext.getUserAttribute(userIdAttrName);
    	logger.info("User GUID: " + userIdAttrValue);
    }
    
    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

}
