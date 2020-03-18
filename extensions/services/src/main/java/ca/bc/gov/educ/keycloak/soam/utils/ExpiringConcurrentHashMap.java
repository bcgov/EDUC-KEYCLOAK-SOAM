package ca.bc.gov.educ.keycloak.soam.utils;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An expiring concurrent hash map solution which stores the keys and values only for a specific amount of time, and then expires after that
 * time.
 * 
 */
public class ExpiringConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {

	private static final long serialVersionUID = 1L;

	private Map<K, Long> timeMap = new ConcurrentHashMap<K, Long>();
	private ExpiringConcurrentHashMapListener<K, V> listener;
	private long expiryInMillis;
	private boolean mapAlive = true;

	public ExpiringConcurrentHashMap() {
		this.expiryInMillis = 10000;
		initialize();
	}

	public ExpiringConcurrentHashMap(ExpiringConcurrentHashMapListener<K, V> listener) {
		this.listener = listener;
		this.expiryInMillis = 10000;
		initialize();
	}

	public ExpiringConcurrentHashMap(long expiryInMillis) {
		this.expiryInMillis = expiryInMillis;
		initialize();
	}

	public ExpiringConcurrentHashMap(long expiryInMillis, ExpiringConcurrentHashMapListener<K, V> listener) {
		this.expiryInMillis = expiryInMillis;
		this.listener = listener;
		initialize();
	}

	void initialize() {
		new CleanerThread().start();
	}

	public void registerRemovalListener(ExpiringConcurrentHashMapListener<K, V> listener) {
		this.listener = listener;
	}

	@Override
	public V put(K key, V value) {
		if (!mapAlive) {
			throw new IllegalStateException("ExpiringConcurrentHashMap is no longer alive.. Try creating a new one.");
		}
		Date date = new Date();
		timeMap.put(key, date.getTime());
		V returnVal = super.put(key, value);
		if (listener != null) {
			listener.notifyOnAdd(key, value);
		}
		return returnVal;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (!mapAlive) {
			throw new IllegalStateException("ExpiringConcurrentHashMap Hashmap is no longer alive.. Try creating a new one.");	
		}
		for (K key : m.keySet()) {
			put(key, m.get(key));
		}
	}

	@Override
	public V putIfAbsent(K key, V value) {
		if (!mapAlive) {
			throw new IllegalStateException("ExpiringConcurrentHashMap Hashmap is no longer alive.. Try creating a new one.");
		}
		if (!containsKey(key)) {
			return put(key, value);
		} else {
			return get(key);
		}
	}

	public void quitMap() {
		mapAlive = false;
	}

	public boolean isAlive() {
		return mapAlive;
	}

	class CleanerThread extends Thread {

		@Override
		public void run() {
			while (mapAlive) {
				cleanMap();
				try {
					Thread.sleep(expiryInMillis / 2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		private void cleanMap() {
			long currentTime = new Date().getTime();
			for (K key : timeMap.keySet()) {
				if (currentTime > (timeMap.get(key) + expiryInMillis)) {
					V value = remove(key);
					timeMap.remove(key);
					if (listener != null) {
						listener.notifyOnRemoval(key, value);
					}
				}
			}
		}
	}
}