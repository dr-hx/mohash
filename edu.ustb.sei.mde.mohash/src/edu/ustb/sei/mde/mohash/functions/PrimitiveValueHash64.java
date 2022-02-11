package edu.ustb.sei.mde.mohash.functions;

import org.eclipse.emf.ecore.EStructuralFeature;

public class PrimitiveValueHash64 extends OnehotObjectHasher64<Object> {

	public PrimitiveValueHash64() {
	}
	
	public long hash(EStructuralFeature feature, Object value) {
		return hash(feature.getName()) | hash(value);
	}

}
