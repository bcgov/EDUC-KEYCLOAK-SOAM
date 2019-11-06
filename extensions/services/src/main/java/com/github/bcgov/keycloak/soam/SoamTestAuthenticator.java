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

package com.github.bcgov.keycloak.authenticators;

import java.io.ByteArrayInputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.RoleModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SoamTestAuthenticator implements org.keycloak.authentication.Authenticator {
    private static Logger logger = Logger.getLogger(SoamTestAuthenticator.class);
    
    
    @Override
    public void authenticate(AuthenticationFlowContext context) {
    	logger.info("SOAM Test: Have entered authenticate");
    }
    

    
    
    private boolean hasAuthenticatorConfig(AuthenticationFlowContext context) {
        return context != null
                && context.getAuthenticatorConfig() != null
                && context.getAuthenticatorConfig().getConfig() != null
                && !context.getAuthenticatorConfig().getConfig().isEmpty();
    }




	@Override
	public void close() {
		//no-op
	}




	@Override
	public void action(AuthenticationFlowContext context) {
		logger.info("SOAM Test: Have entered action method");
		authenticate(context);
	}




	@Override
	public boolean requiresUser() {
		return false;
	}




	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return true;
	}




	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
		logger.info("SOAM Test: Have entered setRequiredActions");
	}


}
