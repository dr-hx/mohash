package edu.ustb.sei.mde.mohash.functions;

public class OnehotObjectHasher64<D> implements Hash64<D> {

	@Override
	public long hash(D data) {
		int hash = data.hashCode();
		int pos = hash % 64;
		return 1L << pos;
	}

}
