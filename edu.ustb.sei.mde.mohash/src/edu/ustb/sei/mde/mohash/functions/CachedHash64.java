package edu.ustb.sei.mde.mohash.functions;

import java.util.Map;

public class CachedHash64<D> implements Hash64<D> {
	
	protected Map<D, Long> hashCache = null;
	protected Hash64<D> delegate;

	public CachedHash64(Hash64<D> d) {
		hashCache = new AccessBasedLRUCache<>(5000,2000,0.75f);
		delegate = d;
	}

	@Override
	public long hash(D data) {
		return hashCache.computeIfAbsent(data, delegate::hash);
	}

}
