package edu.ustb.sei.mde.mohash;

public class HashValue64 {
	public final long code;
	public final int bitCount;
	
	public HashValue64(long code) {
		this.code = code;
		this.bitCount = Long.bitCount(code);
	}

}
