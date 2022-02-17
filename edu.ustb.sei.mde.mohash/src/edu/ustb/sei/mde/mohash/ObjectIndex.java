package edu.ustb.sei.mde.mohash;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory;
import edu.ustb.sei.mde.mohash.emfcompare.SimHashEObjectIndex;
import edu.ustb.sei.mde.mohash.emfcompare.SimHashProximityEObjectMatcher;
import edu.ustb.sei.mde.mohash.functions.Hash64;

public interface ObjectIndex {

	Iterable<EObject> query(EObject target, long hashCode, double minSim);

	void index(EObject object, long hashCode);

	Long remove(EObject object);
	
	Long getHash(EObject object);
	
	Iterable<EObject> getRemainingObjects();

	void printHashCodes(BiFunction<EObject, Long,String> function);
	
	static public SimHashEObjectIndex getObjectIndex(MoHashMatchEngineFactory factory) {
		try {
			DefaultMatchEngine engine = (DefaultMatchEngine) factory.getMatchEngine();
			Method method = engine.getClass().getDeclaredMethod("getEObjectMatcher");
			method.setAccessible(true);
			SimHashProximityEObjectMatcher matcher = (SimHashProximityEObjectMatcher) method.invoke(engine);
			SimHashEObjectIndex index = (SimHashEObjectIndex) matcher.getIndex();
			return index;
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static public double similarity(HashValue64 left, HashValue64 right) {
		return Hash64.cosineSimilarity(left, right);
	}
}