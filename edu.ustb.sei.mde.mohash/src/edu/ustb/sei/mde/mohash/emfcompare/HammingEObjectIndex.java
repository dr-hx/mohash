package edu.ustb.sei.mde.mohash.emfcompare;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.match.eobject.EObjectIndex;
import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher;
import org.eclipse.emf.compare.match.eobject.ScopeQuery;
import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.Maps;

import edu.ustb.sei.mde.mohash.ByTypeIndex;
import edu.ustb.sei.mde.mohash.EObjectHasher;
import edu.ustb.sei.mde.mohash.HammingIndex;
import edu.ustb.sei.mde.mohash.ObjectIndex;

public class HammingEObjectIndex implements EObjectIndex {
	/**
	 * The distance function used to compare the Objects.
	 */
	private ProximityEObjectMatcher.DistanceFunction meter;
	
	/**
	 * An object able to tell us whether an object is in the scope or not.
	 */
	private ScopeQuery scope;
	
	private ObjectIndex lefts;
	private ObjectIndex rights;
	private ObjectIndex origins;
	
	private EObjectHasher hasher;
	
	public HammingEObjectIndex(ProximityEObjectMatcher.DistanceFunction meter, ScopeQuery matcher) {
		this.meter = meter;
		this.scope = matcher;

		// we do not use bucket index by default
		this.lefts = new ByTypeIndex(t->new HammingIndex());
		this.rights = new ByTypeIndex(t->new HammingIndex());
		this.origins = new ByTypeIndex(t->new HammingIndex());
		
		this.hasher = new EObjectHasher();
	}

	@Override
	public Iterable<EObject> getValuesStillThere(Side side) {
		switch(side) {
		case LEFT: return lefts.getRemainingObjects();
		case RIGHT: return rights.getRemainingObjects();
		case ORIGIN: return origins.getRemainingObjects();
		}
		return Collections.emptySet();
	}

	public Map<Side, EObject> findClosests(Comparison inProgress, EObject eObj, Side passedObjectSide) {
		if (!readyForThisTest(inProgress, eObj)) {
			return null;
		}
		Map<Side, EObject> result = new HashMap<EObjectIndex.Side, EObject>(3);
		result.put(passedObjectSide, eObj);
		if (passedObjectSide == Side.LEFT) {
			EObject closestRight = findTheClosest(inProgress, eObj, Side.LEFT, Side.RIGHT, true);
			EObject closestOrigin = findTheClosest(inProgress, eObj, Side.LEFT, Side.ORIGIN, true);
			result.put(Side.RIGHT, closestRight);
			result.put(Side.ORIGIN, closestOrigin);
		} else if (passedObjectSide == Side.RIGHT) {
			EObject closestLeft = findTheClosest(inProgress, eObj, Side.RIGHT, Side.LEFT, true);
			EObject closestOrigin = findTheClosest(inProgress, eObj, Side.RIGHT, Side.ORIGIN, true);
			result.put(Side.LEFT, closestLeft);
			result.put(Side.ORIGIN, closestOrigin);

		} else if (passedObjectSide == Side.ORIGIN) {
			EObject closestLeft = findTheClosest(inProgress, eObj, Side.ORIGIN, Side.LEFT, true);
			EObject closestRight = findTheClosest(inProgress, eObj, Side.ORIGIN, Side.RIGHT, true);
			result.put(Side.LEFT, closestLeft);
			result.put(Side.RIGHT, closestRight);
		}

		return result;

	}
	
	private EObject findTheClosest(Comparison inProgress, final EObject eObj, final Side originalSide,
			final Side sideToFind, boolean shouldDoubleCheck) {
		ObjectIndex originStorage = lefts;
		switch (originalSide) {
			case RIGHT:
				originStorage = rights;
				break;
			case LEFT:
				originStorage = lefts;
				break;
			case ORIGIN:
				originStorage = origins;
				break;

			default:
				break;
		}
		
		ObjectIndex storageToSearchFor = lefts;
		switch (sideToFind) {
			case RIGHT:
				storageToSearchFor = rights;
				break;
			case LEFT:
				storageToSearchFor = lefts;
				break;
			case ORIGIN:
				storageToSearchFor = origins;
				break;

			default:
				break;
		}
		
		// find identical by hash code
		Long hash = originStorage.getHash(eObj);
		
		Iterable<EObject> cand = storageToSearchFor.query(eObj, hash, 1.0);
		for(EObject fastCheck : cand) {
			if (!readyForThisTest(inProgress, fastCheck)) {
			} else {
				distanceCount ++;
				if (meter.areIdentic(inProgress, eObj, fastCheck)) {
					return fastCheck;
				}
			}
		}

		SortedMap<Double, EObject> candidates = Maps.newTreeMap();
		/*
		 * We could not find an EObject which is identical, let's search again and find the closest EObject.
		 */
		Iterable<EObject> cand2 = storageToSearchFor.query(eObj, hash, 0.6);
		double bestDistance = Double.MAX_VALUE;
		EObject bestObject = null;
		
		if(shouldDoubleCheck) {
			for(EObject potentialClosest : cand2) {
				distanceCount ++;
				double dist = meter.distance(inProgress, eObj, potentialClosest);				
				if (dist < bestDistance || (dist != bestDistance && candidates.size() < 3)) { // 5 is a magic number
					candidates.put(Double.valueOf(dist), potentialClosest);
				}
			}
			// double check
			for (Entry<Double, EObject> entry : candidates.entrySet()) {
				EObject doubleCheck = findTheClosest(inProgress, entry.getValue(), sideToFind, originalSide, false);
				if (doubleCheck == eObj) {
					return entry.getValue();
				}
			}
		} else {
			for(EObject potentialClosest : cand2) {
				distanceCount ++;
				double dist = meter.distance(inProgress, eObj, potentialClosest);
				if (dist < bestDistance) {
					bestDistance = dist;
					bestObject = potentialClosest;
				}
			}
		}
		
		return bestObject;
	}

	@Override
	public void remove(EObject eObj, Side side) {
		switch(side) {
		case LEFT: lefts.remove(eObj); break;
		case RIGHT: rights.remove(eObj); break;
		case ORIGIN: origins.remove(eObj); break;
		}
	}
	
	static public long distanceCount = 0;

	@Override
	public void index(EObject eObj, Side side) {
		long hashcode = hasher.hash(eObj);
		switch(side) {
		case LEFT: lefts.index(eObj, hashcode); break;
		case RIGHT: rights.index(eObj, hashcode); break;
		case ORIGIN: origins.index(eObj, hashcode); break;
		}
	}
	
	private boolean readyForThisTest(Comparison inProgress, EObject fastCheck) {
		EObject eContainer = fastCheck.eContainer();
		if (eContainer != null && scope.isInScope(eContainer)) {
			return inProgress.getMatch(eContainer) != null;
		}
		return true;
	}

}
