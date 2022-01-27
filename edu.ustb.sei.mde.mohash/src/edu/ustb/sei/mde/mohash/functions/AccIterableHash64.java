package edu.ustb.sei.mde.mohash.functions;


public class AccIterableHash64<D> implements Hash64<Iterable<D>> {
	// prefer one-hot hasher
	final protected Hash64<D> hasher;

	public AccIterableHash64(Hash64<D> hasher) {
		super();
		this.hasher = hasher;
	}

	@Override
	public long hash(Iterable<D> data) {
		long code = 0;
		for(D d : data) {
			long h = hasher.hash(d);
			code |= h;
		}
		return code;
	}

}
