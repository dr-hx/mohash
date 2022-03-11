package edu.ustb.sei.mde.mohash.jit;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import edu.ustb.sei.mde.mohash.FeatureHasherTuple;

public class EObjectSimHasherGenerator
{
  protected static String nl;
  public static synchronized EObjectSimHasherGenerator create(String lineSeparator)
  {
    nl = lineSeparator;
    EObjectSimHasherGenerator result = new EObjectSimHasherGenerator();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "";
  protected final String TEXT_2 = NL + "package edu.ustb.sei.mde.mohash.jit;" + NL + "" + NL + "import java.util.List;" + NL + "import org.eclipse.emf.ecore.EObject;" + NL + "import edu.ustb.sei.mde.mohash.FeatureHasherTuple;" + NL + "import edu.ustb.sei.mde.mohash.jit.FeatureHasher;" + NL + "import edu.ustb.sei.mde.mohash.EObjectSimHasher;" + NL + "" + NL + "public class ";
  protected final String TEXT_3 = "FeatureHasher implements FeatureHasher {" + NL + "" + NL + "\t@SuppressWarnings({ \"unchecked\", \"restriction\" })" + NL + "\tpublic void doHash(EObject object, List<FeatureHasherTuple> tuples, int[] buffer) {" + NL + "\t";
  protected final String TEXT_4 = NL + "\t\t";
  protected final String TEXT_5 = NL + "\t\t" + NL + "\t\tFeatureHasherTuple tuple";
  protected final String TEXT_6 = " = tuples.get(";
  protected final String TEXT_7 = ");" + NL + "\t\tObject value";
  protected final String TEXT_8 = " = ((";
  protected final String TEXT_9 = ")object).get";
  protected final String TEXT_10 = "();" + NL + "\t\tif(value";
  protected final String TEXT_11 = "!=null) {" + NL + "\t\t\tlong hashCode";
  protected final String TEXT_12 = " = tuple";
  protected final String TEXT_13 = ".hasher.hash(value";
  protected final String TEXT_14 = ");" + NL + "\t\t\tif(hashCode";
  protected final String TEXT_15 = "!=0) EObjectSimHasher.mergeHash(buffer, hashCode";
  protected final String TEXT_16 = ", tuple";
  protected final String TEXT_17 = ");" + NL + "\t\t}" + NL + "\t\t" + NL + "\t";
  protected final String TEXT_18 = NL + "\t}" + NL + "" + NL + "}";

  public String generate(Object argument)
  {
    final StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append(TEXT_1);
    
EObject eObject = (EObject) ((Object[]) argument)[0];
List<FeatureHasherTuple> tuples = (List<FeatureHasherTuple>) (((Object[]) argument)[1]);

Class<?> javaType = eObject.getClass();

    stringBuffer.append(TEXT_2);
    stringBuffer.append(javaType.getSimpleName());
    stringBuffer.append(TEXT_3);
    for(int index=0;index<tuples.size();index++){
    stringBuffer.append(TEXT_4);
    
		FeatureHasherTuple tuple = tuples.get(index);
		String featureName = tuple.feature.getName();
		
    stringBuffer.append(TEXT_5);
    stringBuffer.append(index);
    stringBuffer.append(TEXT_6);
    stringBuffer.append(index);
    stringBuffer.append(TEXT_7);
    stringBuffer.append(index);
    stringBuffer.append(TEXT_8);
    stringBuffer.append(javaType.getCanonicalName());
    stringBuffer.append(TEXT_9);
    stringBuffer.append(featureName.substring(0, 1).toUpperCase());
    stringBuffer.append(featureName.substring(1));
    stringBuffer.append(TEXT_10);
    stringBuffer.append(index);
    stringBuffer.append(TEXT_11);
    stringBuffer.append(index);
    stringBuffer.append(TEXT_12);
    stringBuffer.append(index);
    stringBuffer.append(TEXT_13);
    stringBuffer.append(index);
    stringBuffer.append(TEXT_14);
    stringBuffer.append(index);
    stringBuffer.append(TEXT_15);
    stringBuffer.append(index);
    stringBuffer.append(TEXT_16);
    stringBuffer.append(index);
    stringBuffer.append(TEXT_17);
    }
    stringBuffer.append(TEXT_18);
    return stringBuffer.toString();
  }
}
