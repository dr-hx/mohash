package edu.ustb.sei.mde.mohash.functions;

public class StringHash64 implements Hash64<String> {

	@Override
	public long hash(String data) {
		long h = data==null ? 0 : data.hashCode();
		return h;
	}

}
