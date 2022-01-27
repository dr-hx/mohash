package edu.ustb.sei.mde.mohash.functions;

public class BooleanHash64 implements Hash64<Boolean> {
	
	@Override
	public long hash(Boolean data) {
		if(data==null) return 0;
		return data.hashCode();
	}

}
