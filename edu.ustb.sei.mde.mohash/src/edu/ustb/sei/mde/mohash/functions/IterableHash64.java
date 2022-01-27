package edu.ustb.sei.mde.mohash.functions;


public class IterableHash64<D> implements Hash64<Iterable<D>> {
	final protected Hash64<D> hasher;

	public IterableHash64(Hash64<D> hasher) {
		super();
		this.hasher = hasher;
	}

	@Override
	public long hash(Iterable<D> data) {
		int[] hashBuffer = new int[64];
		int size = 0;
		for(D d : data) {
			size ++;
			long h = hasher.hash(d);
			if(h!=0) {				
				for(int i=0;i<64;i++) {
					if((bitmasks[i] & h) != 0) {
						hashBuffer[i] ++;
					} //else  hashBuffer[i] --;
				}
			}
		}
		
		int threshold = size / 2;
		
		long code = 0;
		for(int i=0;i<64;i++) {
			if(hashBuffer[i] > threshold) code |= bitmasks[i]; 
		}
		return code;
	}

}
