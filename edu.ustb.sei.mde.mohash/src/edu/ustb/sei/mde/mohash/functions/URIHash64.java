package edu.ustb.sei.mde.mohash.functions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;

public class URIHash64 implements Hash64<EObject> {
	/*
	 * The combination of iterablehash+stringhash can be used to handle long reference lists. 
	 */
	private IterableHash64<String> hasher = new IterableHash64<String>(new StringHash64());
		
	private URIComputer uriEncoder = new URIComputer();
	private Map<Iterable<String>, Long> hashCache = new LinkedHashMap<>();

	@Override
	public long hash(EObject data) {
		Iterable<String> fragments = uriEncoder.getOrComputeLocation(data);
		long h = hashCache.computeIfAbsent(fragments, hasher::hash);
		return h;
	}
	
	
}
