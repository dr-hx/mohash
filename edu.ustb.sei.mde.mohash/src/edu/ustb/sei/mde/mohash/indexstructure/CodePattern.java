package edu.ustb.sei.mde.mohash.indexstructure;

public class CodePattern {
	/**
	 * A code pattern describes the hamming weight of each portion of the hashing.
	 * It must have a length of 2^depth. 
	 */
	protected short[] weight;
	
	
	protected int distance(CodePattern other) {
		if(other.weight.length!=weight.length) return -1;
		else {
			int diff = 0;
			for(int i=0;i<weight.length;i++) {
				diff += Math.abs(other.weight[i]-weight[i]);
			}
			return diff;
		}
	}
	
	public boolean match(CodePattern other, int lb, int ub) {
		int diff = distance(other);
		if(diff==-1) return false;
		else return diff >=lb && diff <= ub;
	}
	
	public boolean match(CodePattern other,int ub) {
		return match(other, 0, ub);
	}
	
	public boolean match(CodePattern other) {
		return distance(other) == 0;
	}
	
	public void doAbstract(CodePattern concrete) {
		weight = new short[concrete.weight.length / 2];
		for(int i=0;i<weight.length;i++) {
			weight[i] = (short) (concrete.weight[2*i] + concrete.weight[2 * i + 1]);
		}
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		for(short s : weight) {
			hash = hash * 31 + s;
		}
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof CodePattern) {
			return match((CodePattern) obj);
		} 
		return false;
	}
}