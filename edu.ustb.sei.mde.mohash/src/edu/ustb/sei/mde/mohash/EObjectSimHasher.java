package edu.ustb.sei.mde.mohash;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.google.common.collect.Iterables;

import edu.ustb.sei.mde.mohash.functions.Hash64;
import edu.ustb.sei.mde.mohash.functions.PrimitiveValueHash64;

public class EObjectSimHasher implements Hash64<EObject> {
	static public boolean ENABLE_JIT = false;
	static public HasherTableKind TABLE_KIND = HasherTableKind.WEIGHTED;
	
	protected EHasherTable table = null;
	
	public EObjectSimHasher() {
		this(new EHasherTable());
	}

	public EObjectSimHasher(EHasherTable table) {
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
			mergeHash(hashBuffer, localHash, containerPosWeight, containerNegWeight);
		}
		
		doHash(data, clazz);
		
		long hash = 0;
		for(int i=0;i<64;i++) {
			if(hashBuffer[i]>0) hash |= bitmasks[i];
		}
		return hash;
	}
	
	

	protected void doHash(EObject data, EClass clazz) {
		Iterable<FeatureHasherTuple> pairs = getFeatureHasherTuples(clazz);
		for(FeatureHasherTuple pair : pairs) {
//			if(data.eIsSet(pair.feature)) {				
				Object value = data.eGet(pair.feature);
				if(value!=null) {
					@SuppressWarnings("unchecked")
					long localHash = pair.hasher instanceof PrimitiveValueHash64 ? ((PrimitiveValueHash64)pair.hasher).hash(pair.feature, value)  : pair.hasher.hash(value);
					if(localHash!=0) {
						localHash = strength(localHash, pair);
						mergeHash(hashBuffer, localHash, pair);
					}
//				}
			}
		}
	}
	
	protected long strength(long localHash, FeatureHasherTuple pair) {
		if(pair.feature instanceof EAttribute && pair.postiveWeight > 20) 
			return localHash | Hash64.rShift3(localHash);
		else 
			return localHash;
	}

	private EClass lastKey = null;
	private List<FeatureHasherTuple> lastTuples = null;
	private Map<EClass, List<FeatureHasherTuple>> classFeatureHasherMap = new HashMap<>();
	
	public int getFeatureCount(EClass clazz) {
		List<FeatureHasherTuple> it = getFeatureHasherTuples(clazz);
		return it.size();
	}
	
	public List<FeatureHasherTuple> getFeatureHasherTuples(EClass clazz) {
		if(lastKey==clazz) return lastTuples;
		
		List<FeatureHasherTuple> it = classFeatureHasherMap.get(clazz);
		if(it==null) {
			Iterable<EStructuralFeature> features = Iterables.filter(clazz.getEAllStructuralFeatures(), f->!shouldSkip(f));
			List<FeatureHasherTuple> list = new LinkedList<>();
			features.forEach(f->{
				Hash64<?> hasher = getFeatureHasher(f);
				if(hasher!=null) {
					FeatureHasherTuple pair = new FeatureHasherTuple(f, hasher, table.getPosWeight(f), table.getNegWeight(f));
					if(pair.postiveWeight!=0 || pair.negativeWeight!=0)
						list.add(pair);
				}
			});
			it = list;
			classFeatureHasherMap.put(clazz, it);
		}
		
		lastKey = clazz;
		lastTuples = it;
		
		return it;
	}
	
	
	protected int[] hashBuffer = new int[64];
	
	static public void mergeHash(int[] buffer, long localHash, int pi, int ni) {
		for(int i=0;i<64;i++) {
			if((localHash & bitmasks[i]) != 0) buffer[i] += pi;
			else buffer[i] -= ni;
		}
	}
	
	static public void mergeHash(int[] buffer, long localHash, FeatureHasherTuple pair) {
		mergeHash(buffer, localHash, pair.postiveWeight, pair.negativeWeight);
	}

	public boolean shouldSkip(EStructuralFeature feature) {
		if(feature.isDerived() || feature.isTransient()) return true;
		if(feature instanceof EReference) return ((EReference) feature).isContainer();
		return false;
	}

	protected Hash64<?> getFeatureHasher(EStructuralFeature feature) {
		return table.getHasher(feature);
	}
}
