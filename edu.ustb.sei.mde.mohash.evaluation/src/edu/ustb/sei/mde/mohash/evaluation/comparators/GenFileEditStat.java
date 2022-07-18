package edu.ustb.sei.mde.mohash.evaluation.comparators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenFileEditStat {

	public static void main(String[] args) {
		GenFileEditStat gen = new GenFileEditStat();
		gen.generate(new File("C:\\JavaProjects\\git\\mohash\\edu.ustb.sei.mde.mohash.evaluation\\output\\ecore_small"));
	}
	
	private List<FileEditData> modelDataList = new ArrayList<>();
	private Pattern pattern1 = Pattern.compile("[a-zA-Z0-9_]+\\s\\[numOfEdits=([0-9]+),.+\\sdiffs=([0-9]+),\\stotal=([0-9]+),\\scriticalMiss=[0-9]+\\]");
	
	
	public void generate(File folder) {
		try {
			extractFromFolder(folder);
			
			printSummary(System.out);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	private void printSummary(PrintStream out) {
		modelDataList.forEach(d->{
			out.println(d.toString());
		});
	}

	protected void extractFromFolder(File folder) throws IOException {
		for(File file : folder.listFiles((p,n)->{
			return n.endsWith(".log");
		})) {
			extractFromLog(file);
		}
	}
	
	protected void extractFromLog(File logFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(logFile));
		String line = null;
		
		String modelName = logFile.getName().substring(0, logFile.getName().indexOf('.'));
		
		int maxEdits = 0;
		int minEdits = Integer.MAX_VALUE;
		double maxDA = 0;
		double minDA = Double.MAX_VALUE;
		int total = 0;
		double midDA = 0;
		int count = 0;
		
		while((line=in.readLine())!=null) {
			Matcher m1 = pattern1.matcher(line);
			if(m1.matches()) {
				count ++;
				int edits = Integer.parseInt(m1.group(1));
				int diffs = Integer.parseInt(m1.group(2));
				total = Integer.parseInt(m1.group(3));
				
				double diffRate = ((double)diffs) / total * 100.0;
				midDA += diffRate;
				
				if(edits > maxEdits) maxEdits = edits;
				if(edits < minEdits) minEdits = edits;
				if(diffRate > maxDA) maxDA = diffRate;
				if(diffRate < minDA) minDA = diffRate;
					
			}
		}
		
		midDA = midDA / count;
		
		modelDataList.add(new FileEditData(modelName, minEdits, maxEdits, minDA, maxDA, midDA, total));
		
		in.close();
	}
}

record FileEditData(String modelName, int minEdits, int maxEdits, double minDA, double maxDA, double midDA, int total) {
	public String toString() {
		return String.format("%s & %d--%d & %.2f%%--%.2f%% & %.2f%% & %d\\\\", modelName, minEdits, maxEdits, minDA, maxDA, midDA, total);
	}
}	
	