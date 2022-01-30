package edu.ustb.sei.mde.mohash;

import org.eclipse.emf.ecore.EObject;

public class AdapterEnhancedHammingIndex extends HammingIndex {

	public AdapterEnhancedHammingIndex() {
		super();
	}
	
//	public void indexAdapter(EObject object, long hashCode) {
//		HashAdapter.make(object, hashCode);
//	}
	
	
	@Override
	public Long getHash(EObject object) {
		return HashAdapter.getHash(object);
	}

}
