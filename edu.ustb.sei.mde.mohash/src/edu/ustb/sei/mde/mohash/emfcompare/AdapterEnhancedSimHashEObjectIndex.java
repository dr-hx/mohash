package edu.ustb.sei.mde.mohash.emfcompare;

import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher.DistanceFunction;
import org.eclipse.emf.compare.match.eobject.ScopeQuery;
import org.eclipse.emf.compare.match.eobject.WeightProvider.Descriptor.Registry;

import edu.ustb.sei.mde.mohash.AdapterEnhancedHammingIndex;
import edu.ustb.sei.mde.mohash.ByTypeIndex;

public class AdapterEnhancedSimHashEObjectIndex extends HashBasedEObjectIndex {

	public AdapterEnhancedSimHashEObjectIndex(DistanceFunction meter, ScopeQuery matcher) {
		super(meter, matcher);
	}

	public AdapterEnhancedSimHashEObjectIndex(DistanceFunction meter, ScopeQuery matcher,
			Registry weightProviderRegistry) {
		super(meter, matcher, weightProviderRegistry);
	}

	public AdapterEnhancedSimHashEObjectIndex(DistanceFunction meter, ScopeQuery matcher,
			Registry weightProviderRegistry, double[] thresholds) {
		super(meter, matcher, weightProviderRegistry, thresholds);
	}
	
	@Override
	protected void initIndex() {
		this.lefts = new ByTypeIndex(t->new AdapterEnhancedHammingIndex());
		this.rights = new ByTypeIndex(t->new AdapterEnhancedHammingIndex());
		this.origins = new ByTypeIndex(t->new AdapterEnhancedHammingIndex());
	}

}
