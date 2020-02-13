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
import ca.bc.gov.educ.keycloak.soam.model.SoamServicesCard;
import ca.bc.gov.educ.keycloak.soam.model.SoamStudent;
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

	public String getId() {
		return PROVIDER_ID;
	}

	public String getDisplayType() {
		return "Soam Protocol Mapper";
	}

	public String getDisplayCategory() {
		return TOKEN_MAPPER_CATEGORY;
	}

	public String getHelpText() {
		return "Map SOAM claims";
	} 

	protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
		logger.info("Protocol Mapper Claims list: ");
		for(String s: token.getOtherClaims().keySet()) {
    		logger.info("Key: " + s + " Value: " + token.getOtherClaims().get(s));
		}
		
		String accountType = userSession.getUser().getFirstAttribute("account_type");
		
		logger.info("Protocol Mapper - User Account Type is: " + accountType);
		
		if(accountType == null) {
			//This is a client credential call
		}else {
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
	}

	private void setStandardSoamLoginClaims(IDToken token, SoamLoginEntity soamLoginEntity, UserSessionModel userSession) {
		if(soamLoginEntity.getStudent() != null) {
			populateStudentClaims(token, soamLoginEntity);
		}
		//In this case we have a services card
		else if(soamLoginEntity.getServiceCard() != null) {
			populateServicesCardClaims(token, soamLoginEntity);
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
		SoamStudent student = soamLoginEntity.getStudent();
		token.getOtherClaims().put("studentID", student.getStudentID());
		token.getOtherClaims().put("legalFirstName", student.getLegalFirstName());
		token.getOtherClaims().put("legalMiddleNames", student.getLegalMiddleNames());
		token.getOtherClaims().put("legalLastName", student.getLegalLastName());
		token.getOtherClaims().put("dob", student.getDob());
		token.getOtherClaims().put("pen", student.getPen()); 
		token.getOtherClaims().put("sexCode", student.getSexCode());
		token.getOtherClaims().put("dataSourceCode", student.getDataSourceCode());
		token.getOtherClaims().put("usualFirstName", student.getUsualFirstName());
		token.getOtherClaims().put("usualMiddleNames", student.getUsualMiddleNames());
		token.getOtherClaims().put("usualLastName", student.getUsualLastName());
		token.getOtherClaims().put("email", student.getEmail());
		token.getOtherClaims().put("deceasedDate", student.getDeceasedDate());
		token.getOtherClaims().put("createUser", student.getCreateUser());
		token.getOtherClaims().put("createDate", student.getCreateDate());
		token.getOtherClaims().put("updateUser", student.getUpdateUser());
		token.getOtherClaims().put("updateDate", student.getUpdateDate());
		token.getOtherClaims().put("displayName", student.getLegalFirstName() + " " + soamLoginEntity.getStudent().getLegalLastName());
	}

	private void populateServicesCardClaims(IDToken token, SoamLoginEntity soamLoginEntity) {
		SoamServicesCard servicesCard = soamLoginEntity.getServiceCard();
		token.getOtherClaims().put("birthDate", servicesCard.getBirthDate());
		token.getOtherClaims().put("city", servicesCard.getCity());
		token.getOtherClaims().put("country", servicesCard.getCountry());
		token.getOtherClaims().put("createDate", servicesCard.getCreateDate());
		token.getOtherClaims().put("createUser", servicesCard.getCreateUser());
		token.getOtherClaims().put("did", servicesCard.getDid()); 
		token.getOtherClaims().put("email", servicesCard.getEmail());
		token.getOtherClaims().put("gender", servicesCard.getGender());
		token.getOtherClaims().put("givenName", servicesCard.getGivenName());
		token.getOtherClaims().put("identityAssuranceLevel", servicesCard.getIdentityAssuranceLevel());
		token.getOtherClaims().put("givenNames", servicesCard.getGivenNames());
		token.getOtherClaims().put("postalCode", servicesCard.getPostalCode());
		token.getOtherClaims().put("province", servicesCard.getProvince());
		token.getOtherClaims().put("servicesCardInfoID", servicesCard.getServicesCardInfoID());
		token.getOtherClaims().put("streetAddress", servicesCard.getStreetAddress());
		token.getOtherClaims().put("surname", servicesCard.getSurname());
		token.getOtherClaims().put("updateDate", servicesCard.getUpdateDate());
		token.getOtherClaims().put("updateUser", servicesCard.getUpdateUser());
		token.getOtherClaims().put("displayName", servicesCard.getUserDisplayName());
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
