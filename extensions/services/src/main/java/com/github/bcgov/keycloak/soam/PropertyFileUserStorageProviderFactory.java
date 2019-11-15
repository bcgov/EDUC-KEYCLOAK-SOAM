package com.github.bcgov.keycloak.soam;

import java.util.HashMap;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class PropertyFileUserStorageProviderFactory
                 implements UserStorageProviderFactory<PropertyFileUserStorageProvider> {

    public static final String PROVIDER_NAME = "readonly-property-file";

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }
    
    private static final Logger logger = Logger.getLogger(PropertyFileUserStorageProviderFactory.class);
    protected HashMap<String, String> properties  = new HashMap<String, String>();

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public PropertyFileUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new PropertyFileUserStorageProvider(session, model, properties);
    }
}