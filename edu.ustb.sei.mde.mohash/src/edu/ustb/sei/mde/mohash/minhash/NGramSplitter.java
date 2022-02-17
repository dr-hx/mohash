package edu.ustb.sei.mde.mohash.minhash;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;

public class NGramSplitter {

	final private int N;
	public NGramSplitter(int N) {
		this.N = N;
	}
	public NGramSplitter() {
		this(2);
	}
	
	public List<CharSequence> split(String string) {
		ArrayList<CharSequence> frags = new ArrayList<>(string.length());
		if(Strings.isNullOrEmpty(string)) return frags;
		if(string.length()<=N) {
			frags.add(string);
		} else {
			for(int i=0;i<=string.length() - N;i++) {
				frags.add(string.subSequence(i, i + N));
			}
		}
		
		return frags;
	}

}
