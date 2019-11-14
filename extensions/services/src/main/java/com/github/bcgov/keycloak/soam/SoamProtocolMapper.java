package com.github.bcgov.keycloak.soam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

public class SoamProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper{

	public static final String PROVIDER_ID = "oidc-customprotocolmapper";
	private static Logger logger = Logger.getLogger(SoamProtocolMapper.class);

	private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		logger.info("SOAM: inside getConfigProperties");
		return configProperties;
	}

	@Override
	public String getDisplayCategory() {
		logger.info("SOAM: inside getDisplayCategory");
		return TOKEN_MAPPER_CATEGORY;
	}

	@Override
	public String getDisplayType() {
		logger.info("SOAM: inside getDisplayType");
		return "Stackoverflow Custom Protocol Mapper";
	}

	@Override
	public String getId() {
		logger.info("SOAM: inside getId");
		return PROVIDER_ID;
	}

	@Override
	public String getHelpText() {
		logger.info("SOAM: inside getHelpText");
		return "some help text";
	}

	@Override
	public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
			UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
		logger.info("SOAM: inside transformAccessToken");
		token.getOtherClaims().put("stackoverflowCustomToken", "stackoverflow");

		setClaim(token, mappingModel, userSession, session);
		return token;
	}

	public static ProtocolMapperModel create(String name, boolean accessToken, boolean idToken, boolean userInfo) {
		logger.info("SOAM: inside create");
		ProtocolMapperModel mapper = new ProtocolMapperModel();
		mapper.setName(name);
		mapper.setProtocolMapper(PROVIDER_ID);
		mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
		Map<String, String> config = new HashMap<String, String>();
		mapper.setConfig(config);
		return mapper;
	}

	@Override
	protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
		logger.info("SOAM: inside setClaim");
		super.setClaim(token, mappingModel, userSession);
	}

	@Override
	protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
			KeycloakSession keycloakSession) {
		logger.info("SOAM: inside setClaim2");
		super.setClaim(token, mappingModel, userSession, keycloakSession);
	}
	
	
	
	
}