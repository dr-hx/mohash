package edu.ustb.sei.mde.mohash.emfcompare;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.function.Function;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.match.eobject.EObjectIndex;
import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher;
import org.eclipse.emf.compare.match.eobject.ScopeQuery;
import org.eclipse.emf.compare.match.eobject.WeightProvider;
import org.eclipse.emf.compare.match.eobject.internal.MatchAheadOfTime;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.google.common.collect.Maps;

import edu.ustb.sei.mde.mohash.ByTypeIndex;
import edu.ustb.sei.mde.mohash.EHasherTable;
import edu.ustb.sei.mde.mohash.EObjectSimHasher;
import edu.ustb.sei.mde.mohash.EObjectSimHasherWithJIT;
import edu.ustb.sei.mde.mohash.HWTreeBasedIndex;
import edu.ustb.sei.mde.mohash.ObjectIndex;
import edu.ustb.sei.mde.mohash.StructureOnlyEHasherTable;
import edu.ustb.sei.mde.mohash.TypeMap;
import edu.ustb.sei.mde.mohash.WeightedEHasherTable;

@SuppressWarnings("restriction")
public class HashBasedEObjectIndex implements EObjectIndex, MatchAheadOfTime {
	/**
	 * The distance function used to compare the Objects.
	 */
	private ProximityEObjectMatcher.DistanceFunction meter;
//	public Map<EObject, Match> previousMatchMap = new HashMap<>();
	protected Set<EClass> ignoredClasses = Collections.emptySet();
	private boolean isReasonableCachingDistanceFunction;
	
	protected boolean needHashing(EClass cls) {
		return ignoredClasses.contains(cls)==false;
	}

	public void setIgnoredClasses(Set<EClass> ignoredClasses) {
		this.ignoredClasses = ignoredClasses;
	}
	/**
	 * An object able to tell us whether an object is in the scope or not.
	 */
	private ScopeQuery scope;
	
	protected ObjectIndex lefts;
	protected ObjectIndex rights;
	protected ObjectIndex origins;
	
	private EObjectSimHasher hasher;
	
	private Function<EClass, ObjectIndex> objectIndexBuilder = (t)->new HWTreeBasedIndex();
	
	public EObjectSimHasher getEObjectHasher() {
		return hasher;
	}
	
	public Iterable<ObjectIndex> getIndices() {
		return Arrays.asList(lefts, rights, origins);
	}
	
	public HashBasedEObjectIndex(ProximityEObjectMatcher.DistanceFunction meter, ScopeQuery matcher) {
		this(meter, matcher, null);
	}
	
	public HashBasedEObjectIndex(ProximityEObjectMatcher.DistanceFunction meter, ScopeQuery matcher, WeightProvider.Descriptor.Registry weightProviderRegistry) {
		this(meter, matcher, weightProviderRegistry, null);
	}
	
	public HashBasedEObjectIndex(ProximityEObjectMatcher.DistanceFunction meter, ScopeQuery matcher, WeightProvider.Descriptor.Registry weightProviderRegistry, Double threshold) {
		this(meter, matcher, weightProviderRegistry, new TypeMap<Double>(threshold), null);
	}
	private WeightProvider.Descriptor.Registry weightProviderRegistry;
	private Map<EClass, Double> containerSimilarityRatioMap;
	
	public double getContainerSimilarityRatio(EObject object) {
		EClass clazz = object.eClass();
		Double ratio = containerSimilarityRatioMap.get(clazz);
		if(ratio==null) {
			int max = 0;
			WeightProvider highestRankingWeightProvider = weightProviderRegistry.getHighestRankingWeightProvider(clazz.getEPackage());
			for (EStructuralFeature feat : clazz.getEAllStructuralFeatures()) {
				EClassifier eType = feat.getEType();
				if (eType != null) { // Do not update amount in case of untyped feature
					int featureWeight = highestRankingWeightProvider.getWeight(feat);
					if (featureWeight != 0) {
						max += featureWeight;
					}
				}
			}
			max = max + highestRankingWeightProvider.getContainingFeatureWeight(object);
			int containerWeight = highestRankingWeightProvider.getParentWeight(object);
			ratio = ((double) containerWeight) / max;
			containerSimilarityRatioMap.put(clazz, ratio);
		}
		
		return ratio;
	}
	
	public HashBasedEObjectIndex(ProximityEObjectMatcher.DistanceFunction meter, ScopeQuery matcher, WeightProvider.Descriptor.Registry weightProviderRegistry, TypeMap<Double> threshold, Function<EClass, ObjectIndex> objectIndexBuilder) {
		this.meter = meter;
		isReasonableCachingDistanceFunction = meter instanceof ReasonableCachingDistance;
		this.scope = matcher;
		if(objectIndexBuilder!=null)
			this.objectIndexBuilder = objectIndexBuilder;
		
		if(threshold==null) this.thresholdMap = new TypeMap<Double>(0.5);
		else this.thresholdMap = threshold;

		// we do not use bucket index by default
		initIndex();
		
		this.weightProviderRegistry = weightProviderRegistry;
		this.containerSimilarityRatioMap = new HashMap<>();
		
		if(weightProviderRegistry==null) this.hasher = new EObjectSimHasher();
		else {
			EHasherTable table;
			
			switch(EObjectSimHasher.TABLE_KIND) {
			case DEFAULT: table = new EHasherTable(); break;
			case WEIGHTED: table = new WeightedEHasherTable(weightProviderRegistry); break;
			case STRUCTURAL: table = new StructureOnlyEHasherTable(); break;
			default: table = new EHasherTable();
			}
			
			if(EObjectSimHasher.ENABLE_JIT)
				this.hasher = new EObjectSimHasherWithJIT(table);
			else this.hasher = new EObjectSimHasher(table);
		}
	}

	protected void initIndex() {
		this.lefts = new ByTypeIndex(objectIndexBuilder);
		this.rights = new ByTypeIndex(objectIndexBuilder);
		this.origins = new ByTypeIndex(objectIndexBuilder);
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
	
	/**
	 * copied from  <a>org.eclipse.emf.compare.match.eobject.EditionDistance</a>
	 */
//	private double[] thresholds = {0d, 0.6d, 0.6d, 0.55d, 0.465d };
	private TypeMap<Double> thresholdMap;
	
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
		
		
		Iterable<EObject> cand = storageToSearchFor.query(eObj, null, hash, 1.0, 0.0);
		for(EObject fastCheck : cand) {
			if (!readyForThisTest(inProgress, fastCheck)) {
			} else {
				identicCount ++;
				if (meter.areIdentic(inProgress, eObj, fastCheck)) {
					return fastCheck;
				}
			}
		}

		Match containerMatch = getPreviousMatch(eObj.eContainer(), inProgress);
		SortedMap<Double, EObject> candidates = Maps.newTreeMap();
		double minSim = getMinSim(eObj);
		double containerDiff = getContainerSimilarityRatio(eObj);
		
		boolean canCache = true;
		
		EObject matchedContainer = null;
		if(containerMatch!=null) {			
			switch (sideToFind) {
			case RIGHT:
				matchedContainer = containerMatch.getRight();
				break;
			case LEFT:
				matchedContainer = containerMatch.getLeft();
				break;
			case ORIGIN:
				matchedContainer = containerMatch.getOrigin();
				break;
			default:
				break;
			}
		} else {
			canCache = false;
		}
		
		
		/*
		 * We could not find an EObject which is identical, let's search again and find the closest EObject.
		 */
		Iterable<EObject> cand2 = storageToSearchFor.query(eObj, matchedContainer, hash, minSim, containerDiff);
		double bestDistance = Double.MAX_VALUE;
		EObject bestObject = null;
		
		if(shouldDoubleCheck) {
			for(EObject potentialClosest : cand2) {
				distanceCount ++;
				double dist = meter.distance(inProgress, eObj, potentialClosest);
				if(dist < bestDistance) {
					candidates.put(Double.valueOf(dist), potentialClosest);
				} 
				// FIXME: the following code should not be executed if we want to be consistent with EMF Compare
				// However, the following code may actually improve the result of the match
//				else if(dist<Double.MAX_VALUE && dist != bestDistance && candidates.size() < 3) {
//					candidates.put(Double.valueOf(dist), potentialClosest);
//				}
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
				double dist;
				if(isReasonableCachingDistanceFunction) {
					dist = ((ReasonableCachingDistance)meter).distance(inProgress, eObj, potentialClosest, canCache);
				} else {
					dist = meter.distance(inProgress, eObj, potentialClosest);
				}
				
				if (dist < bestDistance) {
					bestDistance = dist;
					bestObject = potentialClosest;
				}
			}
		} 
		
		return bestObject;
	}

	public Match getPreviousMatch(final EObject eObj, Comparison inProgress) {
		if(eObj==null) return null;
		else return inProgress.getMatch(eObj);
//		return previousMatchMap.get(eObj.eContainer());
	}

	protected double getMinSim(final EObject eObj) {
		EClass clazz = eObj.eClass();
		return thresholdMap.get(clazz);
//		int count = hasher.getFeatureCount(clazz);
//		if(count>=thresholds.length) return 1.0-thresholds[thresholds.length-1];
//		else return 1.0 - thresholds[count];
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
	static public long identicCount = 0;
	@Override
	public void index(EObject eObj, Side side) {
		long hashcode = needHashing(eObj.eClass()) ? hasher.hash(eObj) : 0L;
		switch(side) {
		case LEFT: lefts.index(eObj, hashcode); break;
		case RIGHT: rights.index(eObj, hashcode); break;
		case ORIGIN: origins.index(eObj, hashcode); break;
		}
	}
	
	public void index(EObject eObj, long hashcode, Side side) {
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

	@Override
	public Iterable<EObject> getValuesToMatchAhead(Side side) {
		switch(side) {
		case LEFT: return lefts.getValuesToMatchAhead(side);
		case RIGHT: return rights.getValuesToMatchAhead(side);
		case ORIGIN: origins.getValuesToMatchAhead(side);
		}
		return Collections.emptyList();
	}

}
