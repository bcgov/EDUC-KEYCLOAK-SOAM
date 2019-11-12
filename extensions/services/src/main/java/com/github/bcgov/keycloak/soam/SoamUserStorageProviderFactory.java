package com.github.bcgov.keycloak.soam;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class SoamUserStorageProviderFactory implements UserStorageProviderFactory<SoamUserStorageProvider> {

	private static Logger logger = Logger.getLogger(SoamUserStorageProviderFactory.class);
	static SoamUserStorageProvider SINGLETON = new SoamUserStorageProvider();
	
	public SoamUserStorageProviderFactory() {
		logger.info("SOAM Fac: Inside SoamUserStorageProviderFactory");
	}

	@Override
	public SoamUserStorageProvider create(KeycloakSession session, ComponentModel model) {
		logger.info("SOAM Fac: Inside create");
		return SINGLETON;
	}

	@Override
	public String getId() {
		logger.info("SOAM Fac: Inside getId");
		return  "soam-user-storage";
	}

}
