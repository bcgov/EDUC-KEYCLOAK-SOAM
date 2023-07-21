package ca.bc.gov.educ.keycloak.common.utils;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.BrokeredIdentityContext;

import java.util.List;

public class CommonUtils {

  private static Logger logger = Logger.getLogger(CommonUtils.class);

  private CommonUtils() {
  }

  public static String getValueForAttribute(String attributeName, BrokeredIdentityContext brokerContext){
    if(attributeName == null){
      return null;
    }
    try{
      return ((List<String>) brokerContext.getContextData().get(attributeName)).get(0);
    }catch(Exception e){
      logger.debug("SOAM: attribute value is null for attributeName: " + attributeName);
      return null;
    }
  }
}
