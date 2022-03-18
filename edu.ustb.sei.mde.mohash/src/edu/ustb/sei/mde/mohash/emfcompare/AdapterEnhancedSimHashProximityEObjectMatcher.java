package edu.ustb.sei.mde.mohash.emfcompare;

import java.util.function.Function;

import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher.DistanceFunction;
import org.eclipse.emf.compare.match.eobject.WeightProvider.Descriptor.Registry;
import org.eclipse.emf.ecore.EClass;

import edu.ustb.sei.mde.mohash.ObjectIndex;
import edu.ustb.sei.mde.mohash.TypeMap;

abstract public class AdapterEnhancedSimHashProximityEObjectMatcher extends SimHashProximityEObjectMatcher {

//	public AdapterEnhancedSimHashProximityEObjectMatcher() {
//		super();
//		// TODO Auto-generated constructor stub
//	}

	public AdapterEnhancedSimHashProximityEObjectMatcher(DistanceFunction meter, Registry weightProviderRegistry,
			TypeMap<Double> thresholds, Function<EClass, ObjectIndex> objectIndexBuilder) {
		super(meter, weightProviderRegistry, thresholds, objectIndexBuilder);
	}

	public AdapterEnhancedSimHashProximityEObjectMatcher(DistanceFunction meter, Registry weightProviderRegistry,
			TypeMap<Double> thresholds) {
		this.index = new AdapterEnhancedSimHashEObjectIndex(meter, this, weightProviderRegistry, thresholds);
	}

	public AdapterEnhancedSimHashProximityEObjectMatcher(DistanceFunction meter) {
		this(meter, null);
	}

	public AdapterEnhancedSimHashProximityEObjectMatcher(DistanceFunction meter, Registry weightProviderRegistry) {
		this(meter, weightProviderRegistry, (TypeMap<Double>)null);
	}

	public AdapterEnhancedSimHashProximityEObjectMatcher(DistanceFunction meter, Registry weightProviderRegistry,
			Double threshold) {
		this.index = new AdapterEnhancedSimHashEObjectIndex(meter, this, weightProviderRegistry, threshold);
	}

	
}
