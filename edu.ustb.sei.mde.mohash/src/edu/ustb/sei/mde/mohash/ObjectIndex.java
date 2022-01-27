package edu.ustb.sei.mde.mohash;

import org.eclipse.emf.ecore.EObject;

public interface ObjectIndex {

	Iterable<EObject> query(EObject target, long hashCode, double minSim);

	void index(EObject object, long hashCode);

	Long remove(EObject object);
	
	Long getHash(EObject object);
	
	Iterable<EObject> getRemainingObjects();

}