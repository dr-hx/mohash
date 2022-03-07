package edu.ustb.sei.mde.mohash.functions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;

public class OnehotURIHash64 implements Hash64<EObject> {	
	/*
	 * The combination of acciterablehash+onehothash can be used to handle small reference lists. 
	 */
	private AccIterableHash64<String> hasher = new AccIterableHash64<String>(new OnehotObjectHasher64<>());
	
	private URIComputer uriEncoder = new URIComputer();
	private Map<Iterable<String>, Long> hashCache = new LinkedHashMap<>();

	@Override
	public long hash(EObject data) {
		Iterable<String> fragments = uriEncoder.getOrComputeLocation(data);
		long h = hashCache.computeIfAbsent(fragments, hasher::hash);
		return h;
	}
	
	public void resetURIEncoder() {
		uriEncoder = new URIComputer();
	}
}
