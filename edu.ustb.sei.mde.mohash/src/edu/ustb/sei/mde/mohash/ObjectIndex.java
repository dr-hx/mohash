package edu.ustb.sei.mde.mohash;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.eobject.internal.MatchAheadOfTime;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory;
import edu.ustb.sei.mde.mohash.emfcompare.HashBasedEObjectIndex;
import edu.ustb.sei.mde.mohash.emfcompare.SimHashProximityEObjectMatcher;
import edu.ustb.sei.mde.mohash.functions.Hash64;

@SuppressWarnings("restriction")
public interface ObjectIndex extends MatchAheadOfTime {

	int SEARCH_WINDOW = 1000;

	Iterable<EObject> query(EObject target, EObject containerMatch, long hashCode, double minSim, double containerDiff);

	void index(EObject object, long hashCode);

	Long remove(EObject object);
	
	Long getHash(EObject object);
	
	Iterable<EObject> getRemainingObjects();

	void printHashCodes(BiFunction<EObject, Long,String> function);
	
	static public HashBasedEObjectIndex getObjectIndex(MoHashMatchEngineFactory factory) {
		try {
			DefaultMatchEngine engine = (DefaultMatchEngine) factory.getMatchEngine();
			Method method = engine.getClass().getDeclaredMethod("getEObjectMatcher");
			method.setAccessible(true);
			SimHashProximityEObjectMatcher matcher = (SimHashProximityEObjectMatcher) method.invoke(engine);
			HashBasedEObjectIndex index = (HashBasedEObjectIndex) matcher.getIndex();
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