package ca.bc.gov.educ.keycloak.soam.utils;

import org.keycloak.broker.provider.BrokeredIdentityContext;

import java.util.List;

public class SoamUtils {

  private SoamUtils() {
  }

  public static String getValueForAttribute(String attributeName, BrokeredIdentityContext brokerContext){
    if(attributeName == null){
      return null;
    }
    try{
      return ((List<String>) brokerContext.getContextData().get(attributeName)).get(0);
    }catch(Exception e){
      return null;
    }
  }
}
