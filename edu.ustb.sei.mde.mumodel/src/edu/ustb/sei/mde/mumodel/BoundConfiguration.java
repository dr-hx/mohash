package edu.ustb.sei.mde.mumodel;

public class BoundConfiguration {
	public int upperBound = 100;
	public int featureListBound = 10;
	public int lowerBound = 0;
	public int stringLength = 10;
	public char[] characterScope = new char[] {' ', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
	
	public double maxStringDistanceRatio = 0.3;
	public double possOfCollectionElementRemoval = 0.2;
	public double possOfCollectionElementInsertion = 0.4;
	public double possOfCollectionElementAlteration = 0.4;
}
