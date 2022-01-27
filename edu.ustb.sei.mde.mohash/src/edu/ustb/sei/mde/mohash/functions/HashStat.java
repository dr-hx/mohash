package edu.ustb.sei.mde.mohash.functions;

import java.util.HashMap;
import java.util.Map;

public class HashStat {
	static public long uri_hash_cost = 0;
	static private long uri_hash_start = 0;
	static public long string_sim_hash_cost = 0;
	static private long string_sim_hash_start = 0;
	
	static public long list_hash_cost = 0;
	static private long list_hash_start = 0;
	static private long list_hash_depth = 0;
	
	static private Map<Class<?>, Long> startMap = new HashMap<>();
	static private Map<Class<?>, Long> costMap = new HashMap<>();
	static private Map<Class<?>, Integer> nestedMap = new HashMap<>();
	
	static public void begin(Class<?> type) {
		int nest = nestedMap.compute(type, (t,n)->{
			if(n==null) n = 0;
			return n+1;
		});
		if(nest==1) {
			startMap.put(type, System.nanoTime());
		}
	}
	
	static public void end(Class<?> type) {
		int nest = nestedMap.compute(type, (t,n)->{
			return n-1;
		});
		if(nest==0) {
			long start = startMap.get(type);
			costMap.compute(type, (t,c)->{
				if(c==null) c = 0L;
				return c + (System.nanoTime() - start);
			});
		}
	}
	
	static public void reset() {
		uri_hash_cost = 0;
		string_sim_hash_cost = 0;
		list_hash_cost = 0;
		list_hash_depth = 0;
		
		startMap.clear();
		costMap.clear();
		nestedMap.clear();
	}
	
	static public void print() {
		System.out.println("===============");
		System.out.println("uri:"+uri_hash_cost);
		System.out.println("str_sim:"+string_sim_hash_cost);
		System.out.println("list:"+list_hash_cost);
		
		costMap.forEach((t,c)->{
			System.out.println(t.getName()+" : "+c);
		});
	}
	
	static public void beginURI() {
		uri_hash_start = System.nanoTime();
	}
	static public void endURI() {
		uri_hash_cost += (System.nanoTime() - uri_hash_start);
	}
	static public void beginStrSim() {
		string_sim_hash_start = System.nanoTime();
	}
	static public void endStrSim() {
		string_sim_hash_cost += (System.nanoTime() - string_sim_hash_start);
	}
	static public void beginList() {
		if((list_hash_depth++) == 0)
			list_hash_start = System.nanoTime();
	}
	static public void endList() {
		if((--list_hash_depth) == 0)
			list_hash_cost += (System.nanoTime() - list_hash_start);
	}
}