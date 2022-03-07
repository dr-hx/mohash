package edu.ustb.sei.mde.mohash.functions;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

public class EcoreEListHash64 extends ListHash64<EObject> {
	
	public EcoreEListHash64(Hash64<EObject> hasher) {
		super(hasher);
		cache = new AccessBasedLRUCache<EObjectFeaturePair, Long>(5000, 5000, 0.75f);
	}
	
	public void reset() {
		cache.clear();
	}
	
	private Map<EObjectFeaturePair, Long> cache;
	
	@Override
	public long hash(List<EObject> data) {
		if(data instanceof EStructuralFeature.Setting) {
			EStructuralFeature.Setting setting = (EStructuralFeature.Setting) data;
			EObject owner = setting.getEObject();
			EStructuralFeature feature = setting.getEStructuralFeature();
			return cache.computeIfAbsent(new EObjectFeaturePair(owner, feature), (p)->super.hash(data));
		} else {
			return super.hash(data);
		}
		
	}
	
	class EObjectFeaturePair {
		public EObjectFeaturePair(EObject owner, EStructuralFeature feature) {
			super();
			this.owner = owner;
			this.feature = feature;
		}
		public final EObject owner;
		public final EStructuralFeature feature;
		
		@Override
		public int hashCode() {
			return owner.hashCode() * 31 + feature.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj==null || !(obj instanceof EObjectFeaturePair)) return false;
			return owner == ((EObjectFeaturePair)obj).owner && feature == ((EObjectFeaturePair)obj).feature;
		}
	}
}
