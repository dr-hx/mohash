package edu.ustb.sei.mde.mohash;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import edu.ustb.sei.mde.mohash.functions.Hash64;


public class HammingIndex implements ObjectIndex {
	protected Map<EObject, Long> obj2codeMap = new LinkedHashMap<>();
	protected Map<Long, List<EObject>> code2objMap = new LinkedHashMap<>();
	
	@Override
	public Iterable<EObject> query(EObject target, long hashCode, double minSim) {
		if(minSim==1) {
			List<EObject> o = code2objMap.getOrDefault(hashCode, Collections.emptyList());
			return Iterables.filter(o, eo->obj2codeMap.containsKey(eo));
		}
		
		// ===============================================================
		// FIXME: the following code ensures the order of comparison
		// If we want to be consistent with EMF Compare, we should execute it
		LinkedList<EObject> result = new LinkedList<>();
		for(Entry<EObject, Long> entry : obj2codeMap.entrySet()) {
			Long value = entry.getValue();
			if(value==hashCode || Hash64.jaccardSimilarity(value, hashCode)>=minSim) {
				result.add(entry.getKey());
			}
		}
		return result;
		// ===============================================================
		// FIXME: the following code may change the order of comparison
		// However, the following code may actually improve the result of the match
		// If we want to be consistent with EMF Compare, we should not execute it
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
	}
	
	@Override
	public void index(EObject object, long hashCode) {
		obj2codeMap.put(object, hashCode);
		code2objMap.computeIfAbsent(hashCode,HammingIndex::listCreator).add(object);
	}
	
	static private List<EObject> listCreator(Long key) {
		return new LinkedList<>();
	}
	
	@Override
	public Long remove(EObject object) {
		return obj2codeMap.remove(object);
	}
	
	@Override
	public Long getHash(EObject object) {
		Long code = obj2codeMap.get(object);
		
//		if(code!=null) {
//			List<EObject> o = code2objMap.get(code);
//			if(o!=null) {
//				o.remove(object);
//			}
//		}
		
		return code;
	}
	
	@Override
	public Iterable<EObject> getRemainingObjects() {
		return ImmutableSet.copyOf(obj2codeMap.keySet());
	}

	@Override
	public void printHashCodes(Function<EObject,String> function) {
		code2objMap.forEach((h,list)->{
			Object hashString = Hash64.toString(h);
			list.forEach(e->{				
				System.out.println(String.format("%s\t%s", hashString, function.apply(e)));
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
