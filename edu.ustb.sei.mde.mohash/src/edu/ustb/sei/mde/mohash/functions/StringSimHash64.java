package edu.ustb.sei.mde.mohash.functions;

/**
 * compute simhash based on N-gram model (N=2 by default)
 * @author hexiao
 *
 */
public class StringSimHash64 implements Hash64<String> {

	@Override
	public long hash(String data) {
		if(data==null) return 0;
		else {
			byte[] bytes = data.getBytes();
			
			if(bytes.length==0) return 0L;
			else if(bytes.length==1) return hashNGram(bytes, 0);
			
			long hash = 0L;
			for(int i=0;i<bytes.length-1;i++) {
				long lh = hashNGram(bytes, i);
				hash |= lh;
			}
			return hash;
		}
	}
	
	static final public int MAGIC_NUMBER = 23;
	protected int N = 2;
	
	
	public StringSimHash64() {
		this(2);
	}
	public StringSimHash64(int n) {
		super();
		N = n;
	}

	/**
	 * compute a local hash for [a,b]
	 * @param a
	 * @param b
	 * @return
	 */
	protected long hashNGram(byte[] bytes, int start) {
		int shift = 0;
		
		for(int i=0,j=start ; i<N && j<bytes.length; i++, j++) {
			shift = shift * MAGIC_NUMBER + toLowerCase(bytes[j]); // toLowerCase is a kind of normalization
		}
		
//		shift = shift % 32 + 32;
		shift = shift % 64;
		return 1L << shift;
	}
	
	// we do not use String.toLowerCase for the sake of performance
	private int toLowerCase(byte b) {
		if(b>='A' && b <='Z') return b +32;
		return b;
	}
}
