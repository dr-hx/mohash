package edu.ustb.sei.mde.mohash.emfcompare;

import org.eclipse.emf.compare.EMFCompare;

public class MohashEMFCompareBuilder {

	static public EMFCompare build() {
		return org.eclipse.emf.compare.EMFCompare.builder()
				.setMatchEngineFactoryRegistry(MoHashMatchEngineFactory.createFactoryRegistry())
				.build();
	}
}
