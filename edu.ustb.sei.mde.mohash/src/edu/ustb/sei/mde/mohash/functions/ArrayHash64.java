package edu.ustb.sei.mde.mohash.functions;

import java.util.Arrays;

public class ArrayHash64<D> implements Hash64<D[]> {
	final protected Hash64<D> hasher;

	public ArrayHash64(Hash64<D> hasher) {
		super();
		this.hasher = hasher;
	}

	private byte[] hashBuffer = new byte[64];

	@Override
	public long hash(D[] data) {
		Arrays.fill(hashBuffer, (byte) 0);

		int threshold = data.length / 2;
		
		for(D d : data) {
			long h = hasher.hash(d);
			if(h!=0) {				
				for(int i=0;i<64;i++) {
					if((bitmasks[i] & h) != 0) {
						hashBuffer[i] ++;
					}//else hashBuffer[i] --;
				}
			}
		}
		
		long code = 0;
		for(int i=0;i<64;i++) {
			if(hashBuffer[i] > threshold) code |= bitmasks[i]; 
		}
		return code;
	}

}
