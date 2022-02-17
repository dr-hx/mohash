package edu.ustb.sei.mde.mohash;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import edu.ustb.sei.mde.mohash.jit.EObjectHasherGenerator;
import edu.ustb.sei.mde.mohash.jit.FeatureHasher;
import edu.ustb.sei.mde.mohash.jit.JavaStringCompiler;

public class EObjectSimHasherWithJIT extends EObjectSimHasher {
	private EObjectHasherGenerator generator = new EObjectHasherGenerator();
	static public Map<Class<?>, FeatureHasher> jitHaserMap = new HashMap<>();
	
	static public void clear() {
		jitHaserMap.clear();
	}
	
	public EObjectSimHasherWithJIT() {
		super();
	}

	public EObjectSimHasherWithJIT(EHasherTable table) {
		super(table);
	}
	
	@Override
	protected void doHash(EObject data, EClass clazz) {
		List<FeatureHasherTuple> pairs = getFeatureHasherTuples(clazz);
		FeatureHasher jit = jitHaserMap.get(data.getClass());
		
		if(jit==null) {
			jit = generateJIT(data, pairs);
			jitHaserMap.put(data.getClass(), jit);
		}
		
		jit.doHash(data, pairs, hashBuffer);
	}

	protected FeatureHasher generateJIT(EObject data, List<FeatureHasherTuple> tuples) {
		FeatureHasher jit;
		Class<?> javaType = data.getClass();
		String fileName = javaType.getSimpleName()+"FeatureHasher";
		String fullName = "edu.ustb.sei.mde.mohash.jit."+fileName;
		String code = generator.generate(new Object[] {data, tuples});
		try {
			JavaStringCompiler compiler = new JavaStringCompiler();
		    Map<String, byte[]> results = compiler.compile(fileName + ".java", code);
		    Class<?> instanceClass = compiler.loadClass(fullName, results);
		    FeatureHasher instance = (FeatureHasher) instanceClass.getConstructor().newInstance();
		    jit = instance;
		} catch (Exception e) {
			e.printStackTrace();
			jit = FeatureHasher.DEFAULT;
		}
		return jit;
	}

}
