package edu.ustb.sei.mde.mohash.evaluation.roc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.ecore.EClass;

/*
 * for each log file in the given folder
 * 	extract the summary part, put the best configuration for each type into a multimap
 * for each type in the map
 * 	sort the configurations by the threshold, and compute the five values
 *  
 *
 */
public class GenBoxplot {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	private Pattern dataPattern = Pattern.compile("\\[([^\\]]+)\\]    \\[([\\w]+)\\_([\\w]+)\\]    threshold=(\\d+\\.\\d+)    TPR=(\\d+\\.\\d+)    FPR=(\\d+\\.\\d+)    Prec=(\\d+\\.\\d+)    Recall=(\\d+\\.\\d+)    Acc=(\\d+\\.\\d+)    F2=(\\d+\\.\\d+)");
	
	public void extractFromFolder(File folder) {
		File[] logs = folder.listFiles((d,n)->n.endsWith("log"));
		for(File file : logs) {
			try {
				extractFromFile(file);
			} catch (IOException e) {
				System.out.println("error at "+file.getName());
			}
		}
		
		data.
	}
	
	public void extractFromFile(File file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;
		boolean inSummary = false;
		while((line=in.readLine())!=null) {
			if(line.contains("=====================[SUMMARY]======================")) {
				inSummary = true;
			} else if(inSummary) {
				if(line.contains("=====================[CONFIG]======================")) {
					break;
				} else if(line.contains("====================================================")) {
					continue;
				} else {
					Matcher matcher = dataPattern.matcher(line);
					if(matcher.matches()) {
						String type = matcher.group(1);
						String hashMet = matcher.group(2);
						String simMet = matcher.group(3);
						String th = matcher.group(4);
						String Acc = matcher.group(9);
						String F2 = matcher.group(10);
						
						data.put(hashMet, simMet, type, Double.parseDouble(th));
						typenames.add(type);
					}
				}
			}
		}
		in.close();
	}
	
	private ConfigurationMap data = new ConfigurationMap();
	private Set<String> typenames = new HashSet<>();
}

class ConfigurationMap {
	private Map<String, Map<String, Map<String, java.util.List<Double>>>> map = new HashMap<>();
	
	public void put(String hashMethod, String simMethod, String type, Double best) {
		Map<String, Map<String, java.util.List<Double>>> simMap = map.computeIfAbsent(hashMethod, (x)->new HashMap<>());
		Map<String, java.util.List<Double>> typeMap = simMap.computeIfAbsent(simMethod, (x)->new HashMap<>());
		List<Double> list = typeMap.computeIfAbsent(type, (t)->new ArrayList<>());
		list.add(best);
	}
	
	public void printPlots(PrintStream out, List<String> typenames) {
		map.forEach((hashMet, simMap)->{
			simMap.forEach((simMet, typeMap)->{
				out.println("% "+hashMet+", "+simMet);
				for(int i=0;i<typenames.size();i++) {
					List<Double> rawResult = typeMap.getOrDefault(typenames.get(i), Collections.emptyList());
					BoxplotData data = BoxplotData.create(rawResult);
					out.print(data.toString());
					out.println("% "+typenames.get(i));
				}
			});
		});
	}
}

class BoxplotData {
	private double lowerWhisker;
	private double lowerQuartile;
	private double median;
	private double upperQuartile;
	private double upperWhisker;
	
	private BoxplotData() {}
	
	static public BoxplotData create(List<Double> list) {
		BoxplotData data = new BoxplotData();
		
		Collections.sort(list);
		
		double Q1 = (list.size()+1)/4.0;
		double Q2 = (list.size()+1)/2.0;
		double Q3 = 3*(list.size()+1)/4.0;
		double IQR = (Q3-Q1)*1.5;
		double LB = Q1 - 1.5*IQR;
		double UB = Q3 + 1.5*IQR;
		
		data.lowerWhisker = compute(list, LB);
		data.lowerQuartile = compute(list, Q1);
		data.median = compute(list, Q2);
		data.upperQuartile = compute(list, Q3);
		data.upperWhisker = compute(list, UB);
		
		return data;
	}
	
	static public double compute(List<Double> list, double position) {
		if(position < 0) return list.get(0);
		if(position >= list.size()) return list.get(list.size() - 1);
		
		double lb = Math.floor(position);
		double ub = Math.ceil(position);
		
		
		double ra = 1.0 - (position - lb);
		double rb = (position - lb);
		
		
		double rawL = list.get((int) lb);
		double rawU = list.get((int) ub);
		
		return rawL * ra + rawU * rb;
	}
	
	public String toString() {
		return String.format("\\thplot{%.2f}{%.2f}{%.2f}{%.2f}{%.2f}", lowerWhisker, lowerQuartile, median, upperQuartile, upperWhisker);
	}
}