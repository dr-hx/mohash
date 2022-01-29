package edu.ustb.sei.mde.mohash;

import org.eclipse.emf.ecore.EStructuralFeature;

import edu.ustb.sei.mde.mohash.functions.Hash64;

public class FeatureHasherTuple {
	public FeatureHasherTuple(EStructuralFeature feature, Hash64<?> hasher, int postiveWeight, int negativeWeight) {
		super();
		this.feature = feature;
		this.hasher = hasher;
		this.postiveWeight = postiveWeight;
		this.negativeWeight = negativeWeight;
	}
	
	final public EStructuralFeature feature;
	@SuppressWarnings("rawtypes")
	final public Hash64 hasher;
	
	final public int postiveWeight;
	final public int negativeWeight;
}