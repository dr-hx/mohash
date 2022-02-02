package edu.ustb.sei.mde.mohash;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.mohash.emfcompare.HammingEObjectIndex;
import edu.ustb.sei.mde.mohash.emfcompare.HammingProximityEObjectMatcher;
import edu.ustb.sei.mde.mohash.emfcompare.MoHashMatchEngineFactory;

public interface ObjectIndex {

	Iterable<EObject> query(EObject target, long hashCode, double minSim);

	void index(EObject object, long hashCode);

	Long remove(EObject object);
	
	Long getHash(EObject object);
	
	Iterable<EObject> getRemainingObjects();

	void printHashCodes(Function<EObject,String> function);
	
	static public HammingEObjectIndex getObjectIndex(MoHashMatchEngineFactory factory) {
		try {
			DefaultMatchEngine engine = (DefaultMatchEngine) factory.getMatchEngine();
			Method method = engine.getClass().getDeclaredMethod("getEObjectMatcher");
			method.setAccessible(true);
			HammingProximityEObjectMatcher matcher = (HammingProximityEObjectMatcher) method.invoke(engine);
			HammingEObjectIndex index = (HammingEObjectIndex) matcher.getIndex();
			return index;
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}
}