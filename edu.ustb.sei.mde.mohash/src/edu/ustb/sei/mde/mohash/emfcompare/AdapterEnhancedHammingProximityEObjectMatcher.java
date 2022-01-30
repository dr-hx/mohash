package edu.ustb.sei.mde.mohash.emfcompare;

import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher.DistanceFunction;
import org.eclipse.emf.compare.match.eobject.WeightProvider.Descriptor.Registry;

abstract public class AdapterEnhancedHammingProximityEObjectMatcher extends HammingProximityEObjectMatcher {

	public AdapterEnhancedHammingProximityEObjectMatcher(DistanceFunction meter) {
		this(meter, null);
	}

	public AdapterEnhancedHammingProximityEObjectMatcher(DistanceFunction meter, Registry weightProviderRegistry) {
		this(meter, weightProviderRegistry, null);
	}

	public AdapterEnhancedHammingProximityEObjectMatcher(DistanceFunction meter, Registry weightProviderRegistry,
			double[] thresholds) {
		this.index = new AdapterEnhancedHammingEObjectIndex(meter, this, weightProviderRegistry, thresholds);
	}

}
