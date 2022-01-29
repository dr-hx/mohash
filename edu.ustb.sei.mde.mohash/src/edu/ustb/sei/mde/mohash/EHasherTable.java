package edu.ustb.sei.mde.mohash;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;

import edu.ustb.sei.mde.mohash.functions.AccListHash64;
import edu.ustb.sei.mde.mohash.functions.EcoreEListHash64;
import edu.ustb.sei.mde.mohash.functions.Hash64;
import edu.ustb.sei.mde.mohash.functions.ListHash64;
import edu.ustb.sei.mde.mohash.functions.OnehotURIHash64;
import edu.ustb.sei.mde.mohash.functions.StringSimHash64;

public class EHasherTable {
	static final public EReference ECONTAINER_FEATURE = EcoreFactory.eINSTANCE.createEReference();
	
	protected Map<EStructuralFeature, Hash64<?>> hasherMap = new HashMap<>();

	static public Hash64<EObject> globalURIHasher = new OnehotURIHash64();
	static public EcoreEListHash64 globalEListHasher = new EcoreEListHash64(globalURIHasher);
	
	public Hash64<?> getHasher(EStructuralFeature feature) {
		return hasherMap.computeIfAbsent(feature, EHasherTable::hashBuilder);
	}
	
	static public Hash64<?> hashBuilder(EStructuralFeature feature) {
		if(feature instanceof EAttribute) {
			Hash64<?> valHasher;
			Class<?> valType = feature.getEType().getInstanceClass();
			if(valType==String.class) {
				valHasher = new StringSimHash64();
//			} else if(valType==Boolean.class || valType==boolean.class) {
//				valHasher = new BooleanHash64();
//			} else if(Number.class.isAssignableFrom(valType) || valType==int.class || valType==long.class || valType==double.class) {
//				valHasher = new NumberHash64();
			} else return null;
			
			if(feature.isMany()) {
				return new ListHash64<>(valHasher);
			} else {
				return valHasher;
			}
		} else {
			if(feature.isMany()) {
				return new AccListHash64<>(globalURIHasher);
			} else {
				return globalURIHasher;
			}
		}
	}
	
	public int getPosWeight(EStructuralFeature feature) {
		if("name".equals(feature.getName())) return 50;
		else if(feature==ECONTAINER_FEATURE) return 0;
		else if(feature instanceof EAttribute) return 10;
		else if(feature instanceof EReference && ((EReference)feature).isContainment()) return 1;
		else return 5;
	}
	
	public int getNegWeight(EStructuralFeature feature) {
		if("name".equals(feature.getName())) return 10;
		else if(feature==ECONTAINER_FEATURE) return 0;
		else if(feature instanceof EAttribute) return 2;
		else if(feature instanceof EReference && ((EReference)feature).isContainment()) return 1;
		else return 1;
	}
}
