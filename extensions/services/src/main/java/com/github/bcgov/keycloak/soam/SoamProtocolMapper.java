package com.github.bcgov.keycloak.soam;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;

public class SoamProtocolMapper extends AbstractOIDCProtocolMapper {

	private static Logger logger = Logger.getLogger(SoamProtocolMapper.class);
	
	public SoamProtocolMapper() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getDisplayCategory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDisplayType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHelpText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel,
			KeycloakSession session, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
		logger.info("SOAM Protocol Mapper: inside transformAccessToken");
		token.getOtherClaims().put("MarcoTest", "This is a value");
		return super.transformAccessToken(token, mappingModel, session, userSession, clientSession);
	}
	
	

}
