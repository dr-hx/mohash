package edu.ustb.sei.mde.mohash.functions;

public class NumberHash64 implements Hash64<Number> {

	@Override
	public long hash(Number data) {
		return data.hashCode();
	}
}
