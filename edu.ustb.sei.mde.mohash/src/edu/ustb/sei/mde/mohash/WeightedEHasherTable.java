package edu.ustb.sei.mde.mohash;

import org.eclipse.emf.compare.match.eobject.AbstractWeightProvider;
import org.eclipse.emf.compare.match.eobject.WeightProvider;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;

public class WeightedEHasherTable extends EHasherTable {
	private WeightProvider.Descriptor.Registry weightProviderRegistry;
	
	public WeightedEHasherTable(WeightProvider.Descriptor.Registry weightProviderRegistry) {
		this.weightProviderRegistry = weightProviderRegistry;
	}
	
	@Override
	public int getPosWeight(EStructuralFeature feature) {
		if(feature==ECONTAINER_FEATURE) return 0;
		EClass eType = feature.getEContainingClass();
		int weight = weightProviderRegistry.getHighestRankingWeightProvider(eType.getEPackage()).getWeight(feature) / AbstractWeightProvider.SMALL;
		return Math.min(100, weight);
	}
	
	@Override
	public int getNegWeight(EStructuralFeature feature) {
//		return 0;
		return 0;
//		if(feature==ECONTAINER_FEATURE) return 0;
//		EClass eType = feature.getEContainingClass();
//		int weight = weightProviderRegistry.getHighestRankingWeightProvider(eType.getEPackage()).getWeight(feature) / (AbstractWeightProvider.NORMAL << 1);
//		return Math.min(20, weight);
	}

}
