package edu.ustb.sei.mde.mohash.jit;

import java.util.List;

import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.mohash.EObjectHasher;
import edu.ustb.sei.mde.mohash.FeatureHasherTuple;

public interface FeatureHasher {
	void doHash(EObject object, List<FeatureHasherTuple> tuples, int[] buffer);
	
	static public final FeatureHasher DEFAULT = new DefaultFeatureHasher();
}

class DefaultFeatureHasher implements FeatureHasher {

	@Override
	public void doHash(EObject data, List<FeatureHasherTuple> pairs, int[] buffer) {
		for(FeatureHasherTuple pair : pairs) {
			Object value = data.eGet(pair.feature);
			if(value!=null) {
				@SuppressWarnings("unchecked")
				long localHash = pair.hasher.hash(value);
				if(localHash!=0) EObjectHasher.mergeHash(buffer, localHash, pair);
			}
		}
	}
	
}