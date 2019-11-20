package ca.bc.gov.educ.keycloak.soam.mapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.jboss.logging.Logger;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import com.github.javafaker.Faker;
import com.github.javafaker.Name;

import ca.bc.gov.educ.keycloak.soam.properties.ApplicationProperties;

/**
 * SOAM Protocol Mapper Will be used to set Education specific claims for our
 * client applications
 * 
 * @author Marco Villeneuve
 *
 */
public class SoamProtocolMapper extends AbstractOIDCProtocolMapper
		implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

	private static Logger logger = Logger.getLogger(SoamProtocolMapper.class);
	private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
	private static ApplicationProperties props;

	static {
		// OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
		OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, SoamProtocolMapper.class);
		props = new ApplicationProperties();
	}

	public static final String PROVIDER_ID = "oidc-soam-mapper";

	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayType() {
		return "Soam Protocol Mapper";
	}

	@Override
	public String getDisplayCategory() {
		return TOKEN_MAPPER_CATEGORY;
	}

	@Override
	public String getHelpText() {
		return "Map SOAM claims";
	}

	protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
		// Inject callout from here, using the GUID as our key
		// logger.info("Protocol Mapper - User GUID is: " +
		// userSession.getUser().getUsername());
		// logger.info("Protocol Mapper - Attribute GUID is: " +
		// userSession.getUser().getFirstAttribute("GUID"));
		logger.info("SOAM Injecting claims");
		String pen = getPen();
		token.getOtherClaims().put("pen", pen);
		
	    Faker faker1 = new Faker();
	    Name name = faker1.name();
	    token.getOtherClaims().put("firstName", name.firstName());
	    token.getOtherClaims().put("lastName", name.lastName());
	    token.getOtherClaims().put("acccountType", "BCEID");
		token.getOtherClaims().put("displayName", name.firstName() + " " + name.lastName());
	}

	private String getToken() {
		try {
			OAuthClient client = new OAuthClient(new URLConnectionClient());

			OAuthClientRequest request = OAuthClientRequest.tokenLocation(props.getTokenURL())
					.setGrantType(GrantType.CLIENT_CREDENTIALS).setClientId(props.getClientID())
					.setScope("GET_PEN").setClientSecret(props.getClientSecret()).buildBodyMessage();

			return client.accessToken(request, OAuth.HttpMethod.POST, OAuthJSONAccessTokenResponse.class)
					.getAccessToken();
		} catch (Exception exn) {
			throw new RuntimeException("Could not get token: " + exn);
		}
	}

	public String getPen() {
		try {
			// Sending get request
			URL url = new URL(props.getSoamURL());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setRequestProperty("Authorization", "Bearer " + getToken());
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output;

			StringBuffer response = new StringBuffer();
			while ((output = in.readLine()) != null) {
				response.append(output);
			}

			in.close();
			return response.toString();
		} catch (Exception e) {
			throw new RuntimeException("Could not call SOAM API: " + e);
		}

	} 

	public static ProtocolMapperModel create(String name, String tokenClaimName, boolean consentRequired,
			String consentText, boolean accessToken, boolean idToken) {
		ProtocolMapperModel mapper = new ProtocolMapperModel();
		mapper.setName(name);
		mapper.setProtocolMapper(PROVIDER_ID);
		mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
		mapper.setConsentRequired(consentRequired);
		mapper.setConsentText(consentText);
		Map<String, String> config = new HashMap<String, String>();
		if (accessToken)
			config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
		if (idToken)
			config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
		mapper.setConfig(config);

		return mapper;
	}

}
