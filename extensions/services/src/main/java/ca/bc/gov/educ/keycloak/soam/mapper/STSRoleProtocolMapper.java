package ca.bc.gov.educ.keycloak.soam.mapper;

import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * STS Role Protocol Mapper Will be used to set STS specific roles
 *
 * @author Marco Villeneuve
 *
 */
public class STSRoleProtocolMapper extends AbstractOIDCProtocolMapper
		implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

	private static Logger logger = Logger.getLogger(STSRoleProtocolMapper.class);
	private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

	static {
		OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, STSRoleProtocolMapper.class);
	}

	public static final String PROVIDER_ID = "oidc-sts-role-mapper";

	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	public String getId() {
		return PROVIDER_ID;
	}

	public String getDisplayType() {
		return "STS Role Mapper";
	}

	public String getDisplayCategory() {
		return TOKEN_MAPPER_CATEGORY;
	}

	public String getHelpText() {
		return "Map STS Role claims";
	}

	@Override
  protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx){
		String accountType = userSession.getUser().getFirstAttribute("account_type");

		logger.debug("Protocol Mapper - User Account Type is: " + accountType);

		if(accountType == null) {
			//This is a client credential call
		}else {
			String userGUID = userSession.getUser().getFirstAttribute("user_guid");
			logger.debug("User GUID is: " + userGUID);

			logger.debug("Attribute Values");
			RealmModel realm = userSession.getRealm();
			userSession.getUser().getRoleMappings().add(realm.getRole("TESTMARCO"));

			for(RoleModel rm: userSession.getUser().getRoleMappings()) {
				logger.debug("Role Name: " + rm.getName());
			}
		}
	}

	public static ProtocolMapperModel create(String name, String tokenClaimName, boolean consentRequired,
			String consentText, boolean accessToken, boolean idToken) {
		ProtocolMapperModel mapper = new ProtocolMapperModel();
		mapper.setName(name);
		mapper.setProtocolMapper(PROVIDER_ID);
		mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
		Map<String, String> config = new HashMap<String, String>();
		if (accessToken)
			config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
		if (idToken)
			config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
		mapper.setConfig(config);

		return mapper;
	}

}
