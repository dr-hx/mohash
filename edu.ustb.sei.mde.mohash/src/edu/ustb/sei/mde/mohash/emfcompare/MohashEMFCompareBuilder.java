package edu.ustb.sei.mde.mohash.emfcompare;

import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.match.eobject.WeightProvider;

import edu.ustb.sei.mde.mohash.TypeMap;

public class MohashEMFCompareBuilder {
	static public EMFCompare build() {
		return build(null, null);
	}
	static public EMFCompare build(TypeMap<Double> thresholds) {
		return build(null, thresholds);
	}
	
	static public EMFCompare build(boolean convolutional) {
		return build(convolutional, null, null);
	}
	
	static public EMFCompare build(boolean convolutional, TypeMap<Double> thresholds) {
		return build(convolutional, null, thresholds);
	}
	
	static public EMFCompare build(WeightProvider.Descriptor.Registry weightProviderRegistry, TypeMap<Double> thresholds) {
		return build(false, weightProviderRegistry, thresholds);
	}
	
	static public EMFCompare build(boolean convolutional, WeightProvider.Descriptor.Registry weightProviderRegistry, TypeMap<Double> thresholds) {
		return org.eclipse.emf.compare.EMFCompare.builder()
				.setMatchEngineFactoryRegistry(MoHashMatchEngineFactory.createFactoryRegistry(convolutional, weightProviderRegistry, thresholds))
				.build();
	}
}
