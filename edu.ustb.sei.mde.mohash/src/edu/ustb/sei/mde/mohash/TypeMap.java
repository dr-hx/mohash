package edu.ustb.sei.mde.mohash;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EClass;

public class TypeMap<V> {
	public TypeMap(V defaultValue) {
		super();
		this.defaultValue = defaultValue;
	}

	private Map<EClass, V> map = new HashMap<>();
	private V defaultValue;
	
	public V get(EClass clazz) {
		V val = map.get(clazz);
		if(val==null) {
			return defaultValue;
		} else return val;
	}
	
	public void put(EClass clazz, V value) {
		map.put(clazz, value);
	}
}
