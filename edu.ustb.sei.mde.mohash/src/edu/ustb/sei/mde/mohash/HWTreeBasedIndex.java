package edu.ustb.sei.mde.mohash;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import edu.ustb.sei.mde.mohash.functions.Hash64;
import edu.ustb.sei.mde.mohash.indexstructure.HWTree;

public class HWTreeBasedIndex implements ObjectIndex {
	class EObjectData {
		public EObject object;
		public int order;
		public long hashCode;
	}
	
	protected Map<Long, List<EObject>> code2objMap = new LinkedHashMap<>();
	protected Map<EObject, EObjectData> obj2objDataMap = new LinkedHashMap<>();
	
	protected HWTree<Long, EObjectData> hwTree;
	protected int kThreshold;
	protected int maxDiff;
	
	public HWTreeBasedIndex() {
		this(13, 13);
	}

	public HWTreeBasedIndex(int k, int d) {
		hwTree = new HWTree<>(HWTree::longHashDistance, HWTree::longCodePattern);
		kThreshold = k;
		maxDiff = d;
	}

	@Override
	public Iterable<EObject> query(EObject target, EObject containerMatch, long hashCode, double minSim) {
		if(minSim==1) {
			List<EObject> o = code2objMap.getOrDefault(hashCode, Collections.emptyList());
			return Iterables.filter(o, eo->obj2objDataMap.containsKey(eo));
		}
		
		int A = Long.bitCount(hashCode);
		// if we have B < A/minSim
		// D < A(1+1/min-sqrt(min))/2
		int diff = Math.max(0, (int) Math.ceil(A*(1+1/minSim-Math.sqrt(minSim))/2));
		List<EObjectData> cand = hwTree.searchKNearest(hashCode, kThreshold, Math.min(diff,maxDiff));
		cand.sort((a,b)->a.order-b.order);
		return Iterables.transform(cand, (x)->x.object);
	}
	
	@Override
	public void index(EObject object, long hashCode) {
		EObjectData pair = new EObjectData();
		pair.object = object;
		pair.order = obj2objDataMap.size();
		pair.hashCode = hashCode;
		
		hwTree.insert(hashCode, pair);
		
		obj2objDataMap.put(object, pair);
		code2objMap.computeIfAbsent(hashCode,HWTreeBasedIndex::listCreator).add(object);
	}
	
	static private List<EObject> listCreator(Long key) {
		return new LinkedList<>();
	}

	@Override
	public Long remove(EObject object) {
		EObjectData data = obj2objDataMap.remove(object);
		if(data!=null) {
			hwTree.remove(data.hashCode, data);
			return data.hashCode;
		} 
		return null;
	}

	@Override
	public Long getHash(EObject object) {
		EObjectData data = obj2objDataMap.get(object);
		if(data!=null) return data.hashCode;
		else return null;
	}

	@Override
	public Iterable<EObject> getRemainingObjects() {
		return ImmutableSet.copyOf(obj2objDataMap.keySet());
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

}
