package edu.ustb.sei.mde.mumodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EcorePackage;

import com.google.common.collect.Iterables;

public class RandomUtils {
	private Random random;
	private BoundConfiguration bound = new BoundConfiguration();
	
	public RandomUtils(long seed) {
		System.out.println("random seed is "+seed);
		random = new Random(seed);
	}
	
	public RandomUtils() {
		this(System.currentTimeMillis());
	}
	
	public int nextInt(int bound) {
		return random.nextInt(bound);
	}
	
	public int nextInt() {
		int offset = bound.upperBound - bound.lowerBound;
		int data = random.nextInt(offset);
		return data + bound.lowerBound;
	}
	
	public String nextString() {
		int len = nextInt(bound.stringLength) + 1; // at least 1
		char[] buf = new char[len];
		for(int i=0;i<len;i++) {
			buf[i] = bound.characterScope[nextInt(bound.characterScope.length)];
		}
		return new String(buf);
	}
	
	public String nextString(String prefix) {
		return prefix + nextString();
	}
	
	public double nextDouble() {
		return random.nextDouble();
	}
	
	public double nextDouble(double bound) {
		return bound * random.nextDouble();
	}
	
	public double nextDouble(double lb, double ub) {
		return lb + nextDouble(ub - lb);
	}
	
	public int[] select(int upperBound, int numberOfSelection) {
		SortedSet<Integer> result = new TreeSet<>();
		for(int i=0;i<numberOfSelection;i++) {
			int sel = random.nextInt(upperBound);
			if(result.contains(sel)) {
				for(int j=0;j<upperBound;j++) {
					sel++;
					if(!result.contains(sel)) break;
				}
			}
			result.add(sel);
		}
		int[] iArr = new int[numberOfSelection];
		int i=0;
		for(Integer n : result) {
			iArr[i++] = n;
		}
		return iArr;
	}
	
	public <T> Set<T> select(Collection<T> scope, int number) {
		List<T> list = scope instanceof List<?> ? (List<T>)scope : new ArrayList<T>(scope);
		Collections.shuffle(list, random);
		return new HashSet<>(list.subList(0, number));
	}
	
	public <T> List<T> shuffle(Collection<T> scope) {
		List<T> list = new ArrayList<T>(scope);
		Collections.shuffle(list, random);
		return list;
	}
	
	public <T> T selectOne(List<T> temp) {
		int id = random.nextInt(temp.size());
		return temp.get(id);
	}
	
	public <T> T selectOne(Iterable<T> temp) {
		var size = Iterables.size(temp);
		if(size==0) return null;
		int id = random.nextInt(size);
		for(T t : temp) {
			if(id==0) return t;
			id--;
		}
		return null;
	}
	
	public boolean shouldHappen() {
		return nextDouble() <= 0.5;
	}
	
	public boolean shouldHappen(double poss) {
		return nextDouble() <= poss;
	}
	
	public int select(double... poss) {
		double sum = 0;
		for(double d : poss) sum+=d;
		
		double sel = nextDouble(sum);
		
		double acc = 0;
		for(int i=0;i<poss.length;i++) {
			acc += poss[i];
			if(acc > sel) {
				return i;
			}
		}
		return poss.length - 1;
	}
	
	public String randomEdit(String string) {
		if(string==null) return (String) randomValue(EcorePackage.eINSTANCE.getEString());
		double changeRate = nextDouble(bound.maxStringDistanceRatio);
		double[] actionPoss = new double[] {bound.possOfCollectionElementAlteration, bound.possOfCollectionElementInsertion, bound.possOfCollectionElementRemoval};
		
		int numberOfChanges = (int) Math.round(changeRate * string.length());
		StringBuilder builder = new StringBuilder();
		for(int i = 0;i<string.length();i++) {
			if(shouldHappen(changeRate)) {
				int action = select(actionPoss);
				switch(action) {
				case 0: {
					builder.append(nextChar());
					break;
				}
				case 1: {
					builder.append(nextChar());
					i--; // move back
					break;
				}
				case 2:{
					break; // skip
				}
				}
				numberOfChanges--;
			} else {
				builder.append(string.charAt(i));
			}
		}
		while(numberOfChanges > 0) {
			builder.append(nextChar());
			numberOfChanges--;
		}
		return builder.toString();
	}
	
	public <T> T nextElement(T[] arr) {
		return arr[nextInt(arr.length)];
	}

	public char nextChar() {
		return bound.characterScope[nextInt(bound.characterScope.length)];
	}
	
	public Object randomValue(EDataType type) {
		Class<?> instanceClass = type.getInstanceClass();
		if(instanceClass==int.class || instanceClass == Integer.class) return nextInt();
		else if(instanceClass==boolean.class || instanceClass == Boolean.class) return nextBoolean();
		else if(instanceClass==String.class) return nextString();
		else if(instanceClass==double.class || instanceClass == double.class) return nextDouble();
		else return null;
	}
	
	
	
	public List<Object> randomValueList(EDataType type, int lower, int upper) {
		int len = 0;
		if(upper==-1) upper = bound.featureListBound;
		len = nextInt(lower, upper);
		List<Object> values = new ArrayList<>();
		for(int i=0;i<len;i++) {			
			Object randomValue = randomValue(type);
			if(randomValue!=null)
				values.add(randomValue);
		}
		return values;
	}

	public int nextInt(int lower, int upper) {
		return lower + nextInt(upper - lower);
	}

	public boolean nextBoolean() {
		return random.nextBoolean();
	}
}
