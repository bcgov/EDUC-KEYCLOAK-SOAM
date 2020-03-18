package ca.bc.gov.educ.keycloak.soam.utils;

public interface ExpiringConcurrentHashMapListener<K, V> {
	public void notifyOnAdd(K key, V value);

	public void notifyOnRemoval(K key, V value);
}