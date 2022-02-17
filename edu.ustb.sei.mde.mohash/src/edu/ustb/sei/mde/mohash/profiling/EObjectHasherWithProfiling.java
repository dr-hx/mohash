package edu.ustb.sei.mde.mohash.profiling;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.mohash.EHasherTable;
import edu.ustb.sei.mde.mohash.EObjectSimHasher;
import edu.ustb.sei.mde.mohash.FeatureHasherTuple;

public class EObjectHasherWithProfiling extends EObjectSimHasher {
	
	public EObjectHasherWithProfiling(FeatureHasherProfiler profiler, EHasherTable table) {
		super(table);
		this.profiler = profiler;
	}

	protected FeatureHasherProfiler profiler;
	
	protected void doHash(EObject data, EClass clazz) {
		profiler.addObjectCount(clazz);
		
		Iterable<FeatureHasherTuple> pairs = getFeatureHasherTuples(clazz);
		for(FeatureHasherTuple pair : pairs) {
			Object value = data.eGet(pair.feature);
			if(value!=null) {
				@SuppressWarnings("unchecked")
				long localHash = pair.hasher.hash(value);
				if(localHash!=0) {
					mergeHash(hashBuffer, localHash, pair);
					profiler.addOccurrence(clazz, pair.feature, localHash);
					profiler.addWidth(clazz, pair.feature, localHash);
				}
			}
		}
	}
}
