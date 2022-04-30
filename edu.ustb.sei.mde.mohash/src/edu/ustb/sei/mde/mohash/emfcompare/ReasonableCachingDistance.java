package edu.ustb.sei.mde.mohash.emfcompare;

import java.util.Map;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher.DistanceFunction;
import org.eclipse.emf.compare.match.eobject.internal.AccessBasedLRUCache;
import org.eclipse.emf.ecore.EObject;

@SuppressWarnings("restriction")
public class ReasonableCachingDistance implements DistanceFunction {
	/**
	 * The wrapped function.
	 */
	private DistanceFunction wrapped;

	/**
	 * The cache keeping the previous results.
	 */
	private Map<Pair, Double> distanceCache;

	/**
	 * Create a new caching distance.
	 * 
	 * @param wrapped
	 *            actual distance function to cache results from.
	 */
	public ReasonableCachingDistance(DistanceFunction wrapped) {
		this.wrapped = wrapped;
		distanceCache = new AccessBasedLRUCache<Pair, Double>(10000, 1000, .75F);
	}

	/**
	 * {@inheritDoc}
	 */
	public double distance(Comparison inProgress, EObject a, EObject b) {
		Pair key = new Pair(a, b);
		Double previousResult = distanceCache.get(key);
		if (previousResult == null) {
			double dist = wrapped.distance(inProgress, a, b);
			distanceCache.put(key, Double.valueOf(dist));
			// cache it
			return dist;
		}
		return previousResult.doubleValue();
	}
	
	public double distance(Comparison inProgress, EObject a, EObject b, boolean canCache) {
		if(canCache) return distance(inProgress, a, b);
		else return wrapped.distance(inProgress, a, b);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean areIdentic(Comparison inProgress, EObject a, EObject b) {
		return wrapped.areIdentic(inProgress, a, b);
	}

	/**
	 * A class used as a key for two EObjects. Pair(a,b) and Pair(b,a) should be equals and have the same
	 * hashcodes
	 */
	class Pair {
		// CHECKSTYLE:OFF
		EObject a;

		EObject b;

		public Pair(EObject a, EObject b) {
			super();
			this.a = a;
			this.b = b;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			int first = a.hashCode();
			int second = b.hashCode();
			if (first > second) {
				int tmp = first;
				first = second;
				second = tmp;
			}
			result = prime * result + first;
			result = prime * result + second;
			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Pair other = (Pair)obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			return (a == other.a && b == other.b) || (b == other.a && a == other.b);

		}

		private ReasonableCachingDistance getOuterType() {
			return ReasonableCachingDistance.this;
		}

	}
	// CHECKSTYLE:ON
}
