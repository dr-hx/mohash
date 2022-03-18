package edu.ustb.sei.mde.mohash;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiFunction;

import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.ImmutableSet;

public class SimpleIndex implements ObjectIndex {
	private Set<EObject> allObjects = new LinkedHashSet<>();

	@Override
	public Iterable<EObject> query(EObject target, EObject containerMatch, long hashCode, double minSim,
			double containerDiff) {
		return allObjects;
	}

	@Override
	public void index(EObject object, long hashCode) {
		allObjects.add(object);
	}

	@Override
	public Long remove(EObject object) {
		allObjects.remove(object);
		return 0L;
	}

	@Override
	public Long getHash(EObject object) {
		return 0L;
	}

	@Override
	public Iterable<EObject> getRemainingObjects() {
		return ImmutableSet.copyOf(allObjects);
	}

	@Override
	public void printHashCodes(BiFunction<EObject, Long, String> function) {
		allObjects.forEach(o->{
			System.out.println(function.apply(o, 0L)); 
		});
	}

}
