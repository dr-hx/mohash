package edu.ustb.sei.mde.mohash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.emf.compare.match.eobject.EObjectIndex.Side;
import org.eclipse.emf.compare.match.eobject.internal.MatchAheadOfTime;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@SuppressWarnings("restriction")
public class ByTypeIndex implements ObjectIndex {
	private Function<EClass, ObjectIndex> innerIndexCreator;
	private Map<EClass,ObjectIndex> typeIndex;
	
	public ByTypeIndex(Function<EClass, ObjectIndex> t) {
		typeIndex = new LinkedHashMap<>();
		innerIndexCreator = t;
	}

	@Override
	public Iterable<EObject> query(EObject target, EObject containerMatch, long hashCode, double minSim, double containerDiff) {
		EClass clazz = target.eClass();
		ObjectIndex index = typeIndex.get(clazz);
		if(index!=null) return index.query(target, containerMatch, hashCode, minSim, 0.0);
		else return Collections.emptyList();
	}

	@Override
	public void index(EObject object, long hashCode) {
		EClass clazz = object.eClass();
		ObjectIndex index = typeIndex.computeIfAbsent(clazz, innerIndexCreator);
		index.index(object, hashCode);
	}

	@Override
	public Long remove(EObject object) {
		EClass clazz = object.eClass();
		ObjectIndex index = typeIndex.get(clazz);
		if(index!=null) return index.remove(object);
		else return null;
	}
	
	@Override
	public Long getHash(EObject object) {
		EClass clazz = object.eClass();
		ObjectIndex index = typeIndex.get(clazz);
		if(index!=null) return index.getHash(object);
		else return null;
	}

	@Override
	public Iterable<EObject> getRemainingObjects() {
		List<Iterable<EObject>> iterators = new ArrayList<>(typeIndex.size());
		typeIndex.values().forEach(index->{
			iterators.add(index.getRemainingObjects());
		});
		return Iterables.concat(iterators);
	}
	
	@Override
	public void printHashCodes(BiFunction<EObject, Long, String> function) {
		typeIndex.forEach((c, i)->{
			System.out.println(c.getName()+":");
			i.printHashCodes(function);
			System.out.println("=================================");
		});
	}

	@Override
	public Iterable<EObject> getValuesToMatchAhead(Side side) {
		List<Iterable<EObject>> allLists = Lists.newArrayList();
		for (MatchAheadOfTime typeSpecificIndex : Iterables.filter(typeIndex.values(),
				MatchAheadOfTime.class)) {
			allLists.add(typeSpecificIndex.getValuesToMatchAhead(side));
		}
		return Iterables.concat(allLists);
	}
}
