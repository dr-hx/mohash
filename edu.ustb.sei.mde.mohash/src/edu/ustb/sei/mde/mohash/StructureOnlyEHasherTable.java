package edu.ustb.sei.mde.mohash;

import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

public class StructureOnlyEHasherTable extends EHasherTable {

	public StructureOnlyEHasherTable() {
		super();
	}
	
	@Override
	public int getPosWeight(EStructuralFeature feature) {
		if(feature instanceof EReference) {
			if(((EReference) feature).isContainment()) return 10;
			else return 5;
		}
		else return 0;
	}

	@Override
	public int getNegWeight(EStructuralFeature feature) {
		if(feature instanceof EReference) {
			if(((EReference) feature).isContainment()) return 2;
			else return 1;
		}
		else return 0;
	}
}
