package edu.ustb.sei.mde.mohash;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
		
		LinkedList<EObject> result = new LinkedList<>();
		for(Entry<EObject, Long> entry : obj2codeMap.entrySet()) {
			Long value = entry.getValue();
			if(value==hashCode || Hash64.hammingSimilarity(value, hashCode)>=minSim) {
				result.add(entry.getKey());
			}
		}
		return result;
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
}
