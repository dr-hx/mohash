package edu.ustb.sei.mde.mohash.emfcompare;

import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher.DistanceFunction;
import org.eclipse.emf.compare.match.eobject.WeightProvider.Descriptor.Registry;

abstract public class AdapterEnhancedSimHashProximityEObjectMatcher extends SimHashProximityEObjectMatcher {

	public AdapterEnhancedSimHashProximityEObjectMatcher(DistanceFunction meter) {
		this(meter, null);
	}

	public AdapterEnhancedSimHashProximityEObjectMatcher(DistanceFunction meter, Registry weightProviderRegistry) {
		this(meter, weightProviderRegistry, null);
	}

	public AdapterEnhancedSimHashProximityEObjectMatcher(DistanceFunction meter, Registry weightProviderRegistry,
			double[] thresholds) {
		this.index = new AdapterEnhancedSimHashEObjectIndex(meter, this, weightProviderRegistry, thresholds);
	}

}
