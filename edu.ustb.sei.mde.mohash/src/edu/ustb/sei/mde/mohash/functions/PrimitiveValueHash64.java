package edu.ustb.sei.mde.mohash.functions;

import org.eclipse.emf.ecore.EStructuralFeature;

public class PrimitiveValueHash64 extends OnehotObjectHasher64<Object> {

	public PrimitiveValueHash64() {
	}
	
	public long hash(Object data) {
		int hash = data.hashCode();
		int pos = hash % 64;
		return 1L << pos;
	}
	
	public long hash(EStructuralFeature feature, Object value) {
		int hash = (feature.getName().hashCode() * 31 + value.hashCode()) % 64;
		return 1L << hash;
	}

}
