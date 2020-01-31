package ca.bc.gov.educ.keycloak.soam.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import ca.bc.gov.educ.keycloak.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.keycloak.soam.model.SoamLoginEntity;
import ca.bc.gov.educ.keycloak.soam.rest.RestUtils;

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

	static {
		// OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
		OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, SoamProtocolMapper.class);
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
		String accountType = userSession.getUser().getFirstAttribute("account_type");
		
		logger.info("Protocol Mapper - User Account Type is: " + accountType);
		
		if(accountType == null) {
			throw new SoamRuntimeException("Account type is null; account type should always be available, check the IDP mappers for the hardcoded attribute");
		}

		String userGUID = userSession.getUser().getUsername();
		
		if(accountType.equals("bceid")){
			logger.info("SOAM Fetching BCEID Claims");
			
			SoamLoginEntity soamLoginEntity = RestUtils.getInstance().getSoamLoginEntity("BASIC", userGUID);
			token.getOtherClaims().put("accountType", "BCEID");
			
			setStandardSoamLoginClaims(token, soamLoginEntity, userSession);
		}else if(accountType.equals("bcsc")){
			logger.info("SOAM Fetching BCSC Claims");
			
			SoamLoginEntity soamLoginEntity = RestUtils.getInstance().getSoamLoginEntity("BCSC", userGUID);
			token.getOtherClaims().put("accountType", "BCSC");
			
			setStandardSoamLoginClaims(token, soamLoginEntity, userSession);	
		}
	}

	private void setStandardSoamLoginClaims(IDToken token, SoamLoginEntity soamLoginEntity, UserSessionModel userSession) {
		if(soamLoginEntity.getStudent() != null) {
			populateStudentClaims(token, soamLoginEntity);
		}
		//In this case we have a digital identity; someone that has logged in but does not have an associated student record
		else if(soamLoginEntity.getDigitalIdentityID() != null) {
			populateDigitalIDClaims(token, soamLoginEntity, userSession);
		}
		//This is an exception since we have no data at all
		else {
			throw new SoamRuntimeException("No student or digital ID data found in SoamLoginEntity");
		}
	}
	
	private void populateDigitalIDClaims(IDToken token, SoamLoginEntity soamLoginEntity, UserSessionModel userSession) {
		token.getOtherClaims().put("digitalIdentityID", soamLoginEntity.getDigitalIdentityID()); 
		token.getOtherClaims().put("displayName", userSession.getUser().getFirstAttribute("display_name"));
	}
	
	private void populateStudentClaims(IDToken token, SoamLoginEntity soamLoginEntity) {
		token.getOtherClaims().put("studentID", soamLoginEntity.getStudent().getStudentID());
		token.getOtherClaims().put("legalFirstName", soamLoginEntity.getStudent().getLegalFirstName());
		token.getOtherClaims().put("legalMiddleNames", soamLoginEntity.getStudent().getLegalMiddleNames());
		token.getOtherClaims().put("legalLastName", soamLoginEntity.getStudent().getLegalLastName());
		token.getOtherClaims().put("dob", soamLoginEntity.getStudent().getDob());
		token.getOtherClaims().put("pen", soamLoginEntity.getStudent().getPen());
		token.getOtherClaims().put("sexCode", soamLoginEntity.getStudent().getSexCode());
		token.getOtherClaims().put("dataSourceCode", soamLoginEntity.getStudent().getDataSourceCode());
		token.getOtherClaims().put("usualFirstName", soamLoginEntity.getStudent().getUsualFirstName());
		token.getOtherClaims().put("usualMiddleNames", soamLoginEntity.getStudent().getUsualMiddleNames());
		token.getOtherClaims().put("usualLastName", soamLoginEntity.getStudent().getUsualLastName());
		token.getOtherClaims().put("email", soamLoginEntity.getStudent().getEmail());
		token.getOtherClaims().put("deceasedDate", soamLoginEntity.getStudent().getDeceasedDate());
		token.getOtherClaims().put("createUser", soamLoginEntity.getStudent().getCreateUser());
		token.getOtherClaims().put("createDate", soamLoginEntity.getStudent().getCreateDate());
		token.getOtherClaims().put("updateUser", soamLoginEntity.getStudent().getUpdateUser());
		token.getOtherClaims().put("updateDate", soamLoginEntity.getStudent().getUpdateDate());
		token.getOtherClaims().put("displayName", soamLoginEntity.getStudent().getLegalFirstName() + " " + soamLoginEntity.getStudent().getLegalLastName());
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
