package ca.bc.gov.educ.keycloak.soam.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.logging.Logger;

public class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {
	
	private static Logger logger = Logger.getLogger(MaxSizeHashMap.class);

	private static final long serialVersionUID = -6215491273927146252L;
	private final int maxSize;

    public MaxSizeHashMap(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    	logger.info("MaxSizeHashMap removing eldest entry, current map size: " + size());
        return size() > maxSize;
    }
}