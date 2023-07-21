package ca.bc.gov.educ.keycloak.common.utils;

public interface ExpiringConcurrentHashMapListener<K, V> {
	public void notifyOnAdd(K key, V value);

	public void notifyOnRemoval(K key, V value);
}
