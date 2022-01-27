package edu.ustb.sei.mde.mohash.functions;

import java.util.List;

public class AccListHash64<D> implements Hash64<List<D>> {
	final protected Hash64<D> hasher;

	public AccListHash64(Hash64<D> hasher) {
		super();
		this.hasher = hasher;
	}

	@Override
	public long hash(List<D> data) {
		long code = 0;
		
		for(D d : data) {
			long h = hasher.hash(d);
			code |= h;
		}
		
		return code;
	}

}
