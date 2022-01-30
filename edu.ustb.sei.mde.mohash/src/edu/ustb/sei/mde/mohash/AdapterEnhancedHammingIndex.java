package edu.ustb.sei.mde.mohash;

import org.eclipse.emf.ecore.EObject;

public class AdapterEnhancedHammingIndex extends HammingIndex {

	public AdapterEnhancedHammingIndex() {
		super();
	}
	
	@Override
	public void index(EObject object, long hashCode) {
		HashAdapter.make(object, hashCode);
		super.index(object, hashCode);
	}
	
	@Override
	public Long getHash(EObject object) {
		return HashAdapter.getHash(object);
	}

}
