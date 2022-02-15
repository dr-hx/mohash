package edu.ustb.sei.mde.mohash;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import edu.ustb.sei.mde.mohash.functions.Hash64;


public class HammingIndex implements ObjectIndex {
	protected Map<EObject, HashValue64> obj2codeMap = new LinkedHashMap<>();
	protected Map<Long, List<EObject>> code2objMap = new LinkedHashMap<>();
	
//	private Map<EObject, HashValue64>[] bitCountIndex = build();
//	
//	private static Map<EObject, HashValue64>[] build() {
//		@SuppressWarnings("unchecked")
//		Map<EObject, HashValue64>[] sets = new Map[65];
//		for(int i=0;i<sets.length;i++) {
//			sets[i] = new LinkedHashMap<>();
//		}
//		return sets;
//	}
//	
//	private void addToBitCountIndex(HashValue64 hv, EObject o) {
//		Map<EObject, HashValue64> set = bitCountIndex[hv.bitCount];
//		set.put(o, hv);
//	}
//	
//	private void removeToBitCountIndex(HashValue64 hv, EObject o) {
//		Map<EObject, HashValue64> set = bitCountIndex[hv.bitCount];
//		set.remove(o);
//	}
	
	@Override
	public Iterable<EObject> query(EObject target, long hashCode, double minSim) {
		if(minSim==1) {
			List<EObject> o = code2objMap.getOrDefault(hashCode, Collections.emptyList());
			return Iterables.filter(o, eo->obj2codeMap.containsKey(eo));
		}
		
		HashValue64 hv = new HashValue64(hashCode);
		
		// ===============================================================
		// FIXME: the following code ensures the order of comparison
		// If we want to be consistent with EMF Compare, we should execute it
		int minBits = (int) Math.round(hv.bitCount * minSim - 0.5);
		int maxBits = (int) Math.round(hv.bitCount / minSim + 0.5);
		
		LinkedList<EObject> result = new LinkedList<>();
		for(Entry<EObject, HashValue64> entry : obj2codeMap.entrySet()) {
			HashValue64 value = entry.getValue();
			if(value.code==hashCode || (hv.bitCount >=minBits && hv.bitCount<=maxBits && ObjectIndex.similarity(value, hv)>=minSim)) {
				result.add(entry.getKey());
			}
		}
		return result;
		// ===============================================================
		// FIXME: the following code may change the order of comparison
		// However, the following code may actually improve the result of the match
		// If we want to be consistent with EMF Compare, we should not execute it
		// Variant 1
//		SortedSet<DiffPair> result = new TreeSet<>(DiffPair::compare);
//		for(Entry<EObject, Long> entry : obj2codeMap.entrySet()) {
//			Long value = entry.getValue();
//			if(value==hashCode) {
//				result.add(new DiffPair(1.0, entry.getKey()));
//			} else {
//				double jaccardSimilarity = Hash64.jaccardSimilarity(value, hashCode);
//				if(jaccardSimilarity>=minSim) {
//					result.add(new DiffPair(jaccardSimilarity, entry.getKey()));
//				}
//			}
//		}
//		return Iterables.transform(result, DiffPair::getEObject);
		// ===============================================================
//		int minBits = Math.max(0, (int) Math.round(hv.bitCount * minSim - 0.5));
//		int maxBits = Math.min(64, (int) Math.round(hv.bitCount / minSim + 0.5));
//		LinkedList<EObject> result = new LinkedList<>();
//		for(int i = minBits ; i<=maxBits; i++) {
//			Map<EObject, HashValue64> map = bitCountIndex[i];
//			for(Entry<EObject, HashValue64> entry : map.entrySet()) {
//				HashValue64 value = entry.getValue();
//				if(value.code==hashCode || ObjectIndex.similarity(value, hv)>=minSim) {
//					result.add(entry.getKey());
//				}
//			}
//		}
//		return result;
//			
		
		// ===============================================================
	}
	
	@Override
	public void index(EObject object, long hashCode) {
		HashValue64 hv = new HashValue64(hashCode);
		obj2codeMap.put(object, hv);
//			addToBitCountIndex(hv, object);
		code2objMap.computeIfAbsent(hashCode,HammingIndex::listCreator).add(object);
	}
	
	static private List<EObject> listCreator(Long key) {
		return new LinkedList<>();
	}
	
	@Override
	public Long remove(EObject object) {
		HashValue64 hv = obj2codeMap.remove(object);
//			removeToBitCountIndex(hv, object);
		if(hv!=null) return hv.code;
		else return null;
	}
	
	@Override
	public Long getHash(EObject object) {
		HashValue64 hv = obj2codeMap.get(object);
		
//		if(code!=null) {
//			List<EObject> o = code2objMap.get(code);
//			if(o!=null) {
//				o.remove(object);
//			}
//		}
		
		if(hv!=null) return hv.code;
		else return null;
	}
	
	@Override
	public Iterable<EObject> getRemainingObjects() {
		return ImmutableSet.copyOf(obj2codeMap.keySet());
	}

	@Override
	public void printHashCodes(BiFunction<EObject, Long, String> function) {
		code2objMap.forEach((h,list)->{
			Object hashString = Hash64.toString(h)+":"+Long.bitCount(h);
			list.forEach(e->{				
				System.out.println(String.format("%s\t%s", hashString, function.apply(e, h)));
			});
		});
	}
	
	static class DiffPair {
		public DiffPair(double sim, EObject eObj) {
			super();
			this.diff = 1.0 - sim;
			this.eObj = eObj;
		}

		public double diff;
		public EObject eObj;
		
		public EObject getEObject() {
			return eObj;
		}
		
		static public int compare(DiffPair a, DiffPair b) {
			double delta = a.diff - b.diff;
			if(delta<0) return -1;
			else if(delta==0) return a.hashCode() - b.hashCode();
			else return 1;
		}
	}
}
