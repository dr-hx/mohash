package edu.ustb.sei.mde.mohash.emfcompare;

import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.match.eobject.WeightProvider;

public class MohashEMFCompareBuilder {
	static public EMFCompare build() {
		return build(null, null);
	}
	static public EMFCompare build(double[] thresholds) {
		return build(null, thresholds);
	}
	
	static public EMFCompare build(WeightProvider.Descriptor.Registry weightProviderRegistry, double[] thresholds) {
		return org.eclipse.emf.compare.EMFCompare.builder()
				.setMatchEngineFactoryRegistry(MoHashMatchEngineFactory.createFactoryRegistry(weightProviderRegistry, thresholds))
				.build();
	}
}
