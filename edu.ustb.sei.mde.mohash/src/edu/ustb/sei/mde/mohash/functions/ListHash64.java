package edu.ustb.sei.mde.mohash.functions;

import java.util.Arrays;
import java.util.List;

public class ListHash64<D> implements Hash64<List<D>> {
	final protected Hash64<D> hasher;

	public ListHash64(Hash64<D> hasher) {
		super();
		this.hasher = hasher;
	}
	
	static int[] hashBuffer = new int[64];

	@Override
	public long hash(List<D> data) {
		switch(data.size()) {
		case 0: return 0L;
		case 1: return hasher.hash(data.get(0));
		case 2: return hasher.hash(data.get(0)) | hasher.hash(data.get(1));
		}
		
		Arrays.fill(hashBuffer, 0);
		
		int threshold = data.size() / 2;
		
		for(D d : data) {
			long h = hasher.hash(d);
			if(h!=0) {				
				for(int i=0;i<64;i++) {
					if((bitmasks[i] & h) != 0) {
						hashBuffer[i] ++;
					} //else hashBuffer[i] --;
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
