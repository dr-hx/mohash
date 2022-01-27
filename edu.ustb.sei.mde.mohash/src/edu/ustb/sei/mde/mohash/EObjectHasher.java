package edu.ustb.sei.mde.mohash;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.google.common.collect.Iterables;

import edu.ustb.sei.mde.mohash.functions.Hash64;

public class EObjectHasher implements Hash64<EObject> {
	
	protected EHasherTable table = null;
	
	public EObjectHasher() {
		this(new EHasherTable());
	}

	public EObjectHasher(EHasherTable table) {
		super();
		this.table = table;
		containerPosWeight = table.getPosWeight(EHasherTable.ECONTAINER_FEATURE);
		containerNegWeight = table.getNegWeight(EHasherTable.ECONTAINER_FEATURE);
	}
	
	private int containerPosWeight;
	private int containerNegWeight;

	@Override
	public long hash(EObject data) {
		Arrays.fill(hashBuffer, 0);
		
		EClass clazz = data.eClass();
		
		@SuppressWarnings("rawtypes")
		Hash64 containerHasher = getFeatureHasher(EHasherTable.ECONTAINER_FEATURE);
		Object container = data.eContainer();
		if(container!=null) {
			@SuppressWarnings("unchecked")
			long localHash = containerHasher.hash(container);
			mergeHash(hashBuffer, localHash, EHasherTable.ECONTAINER_FEATURE,containerPosWeight, containerNegWeight);
		}
		
		doHash(data, clazz);
		
		long hash = 0;
		for(int i=0;i<64;i++) {
			if(hashBuffer[i]>0) hash |= bitmasks[i];
		}
		
		return hash;
	}
	
	

	protected void doHash(EObject data, EClass clazz) {
		Iterable<FeatureHasherPair> pairs = getFeatureHasherPairs(clazz);
		for(FeatureHasherPair pair : pairs) {
			Object value = data.eGet(pair.feature);
			if(value!=null) {
				@SuppressWarnings("unchecked")
				long localHash = pair.hasher.hash(value);
				if(localHash!=0) mergeHash(hashBuffer, localHash, pair);
			}
		}
	}
	
	protected class FeatureHasherPair {
		public FeatureHasherPair(EStructuralFeature feature, Hash64<?> hasher, int postiveWeight, int negativeWeight) {
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
	
	private Map<EClass, Iterable<FeatureHasherPair>> classFeatureHasherMap = new HashMap<>();
	
	protected Iterable<FeatureHasherPair> getFeatureHasherPairs(EClass clazz) {
		Iterable<FeatureHasherPair> it = classFeatureHasherMap.get(clazz);
		if(it==null) {
			Iterable<EStructuralFeature> features = Iterables.filter(clazz.getEAllStructuralFeatures(), f->!f.isDerived()&&!f.isTransient()&&!shouldSkip(f));
			List<FeatureHasherPair> list = new LinkedList<>();
			features.forEach(f->{
				Hash64<?> hasher = getFeatureHasher(f);
				if(hasher!=null) {
					FeatureHasherPair pair = new FeatureHasherPair(f, hasher, table.getPosWeight(f), table.getNegWeight(f));
					list.add(pair);
				}
			});
			it = list;
			classFeatureHasherMap.put(clazz, it);
		}
		
		return it;
	}
	
	
	protected int[] hashBuffer = new int[64];
	
	static protected void mergeHash(int[] buffer, long localHash, EStructuralFeature feature, int pi, int ni) {
		for(int i=0;i<64;i++) {
			if((localHash & bitmasks[i]) != 0) buffer[i] += pi;
			else buffer[i] -= ni;
		}
	}
	
	static protected void mergeHash(int[] buffer, long localHash, FeatureHasherPair pair) {
		mergeHash(buffer, localHash, pair.feature, pair.postiveWeight, pair.negativeWeight);
	}

	protected boolean shouldSkip(EStructuralFeature feature) {
		return false;
	}

	protected Hash64<?> getFeatureHasher(EStructuralFeature feature) {
		return table.getHasher(feature);
	}
	
	

}
