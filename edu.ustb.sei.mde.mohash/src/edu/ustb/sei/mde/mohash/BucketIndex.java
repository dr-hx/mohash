package edu.ustb.sei.mde.mohash;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.mohash.functions.Hash64;

/**
 * The index is based on the article Fast Search in Hamming Space with Multi-Index Hashing
 * @author hexiao
 *
 */
public class BucketIndex extends HammingIndex {
	static final private int N = 8;
	static final private long[] bucketmasks = new long[] {
			0x00000000000000FFL, 0x000000000000FF00L, 0x0000000000FF0000L, 0x00000000FF000000L, 
			0x000000FF00000000L, 0x0000FF0000000000L, 0x00FF000000000000L, 0xFF00000000000000L
	};
	
	
	private Map<Integer, Set<EObject>>[] buckets;
	
	@SuppressWarnings("unchecked")
	public BucketIndex() {
		buckets = new Map[N];
		for(int i=0;i<N;i++) {
			buckets[i] = new HashMap<>();
		}
	}
	
	@Override
	public Iterable<EObject> query(EObject target, long hashCode, double minSim) {
		/*
		 * First, we must compute the max distance from minSim.
		 * We assume that sim = |h1 AND h2| / |h1 OR h2|, where h2 is any hash code.
		 * The hamming distance K = |h1 OR h2| - |h1 AND h2|.
		 * 
		 * sim > minSim
		 * => |h1 AND h2| / |h1 OR h2| = |h1 AND h2| / (|h1 AND h2| + K) > minSim
		 * => K < (1-minSim)/minSim*|h1 AND h2|<(1-minSim)/minSim*|h1|
		 */
		int nOneBits = Long.bitCount(hashCode);

		int maxDis = (int) Math.round((1 - minSim) / minSim * nOneBits + 0.5);
		int maxBucketDis = maxDis / N;
		
		HashValue64 hv = new HashValue64(hashCode);
		
		Set<EObject> result = new HashSet<>();
		
		for(int i=0; i<N; i++) {
			Map<Integer, Set<EObject>> bucket = buckets[i];
			int bInd = getBucketIndex(hashCode, i);
			
			for(Entry<Integer, Set<EObject>> e : bucket.entrySet()) {
				int localKey = e.getKey();
				if(Hash64.hammingDistance(bInd, localKey) <= maxBucketDis) {
					result.addAll(e.getValue());
				}
			}
		}
		
		// filter
		result.removeIf(o->{
			HashValue64 ho = obj2codeMap.get(o);
			if(ObjectIndex.similarity(ho, hv) < minSim) return true;
			else return false;
		});
		
		return result;
	}
	
	@Override
	public void index(EObject object, long hashCode) {
		super.index(object, hashCode);
		for(int i = 0; i<N; i++) {
			int bInd = getBucketIndex(hashCode, i);
			buckets[i].computeIfAbsent(bInd, BucketIndex::setCreator).add(object);
		}
	}
	
	@Override
	public Long remove(EObject object) {
		Long c = super.remove(object);
		if(c!=null) {			
			for(int i = 0; i<N; i++) {
				int bInd = getBucketIndex(c, i);
				Set<EObject> set = buckets[i].get(bInd);
				if(set!=null) {
					set.remove(object);
				}
			}
		}
		return c;
	}
	
	static private Set<EObject> setCreator(long b) {
		return new HashSet<>();
	}
	
	static private int getBucketIndex(long code, int bucketID) {
		int bucketcode = (int) (code & bucketmasks[bucketID]) >>> (bucketID << 3);
		return bucketcode;
	}
}
