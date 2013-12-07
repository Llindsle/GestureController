package controller;

import java.io.Serializable;
import java.util.Collection;
import java.util.Scanner;

import controller.xmlGestureParser.xmlStatics;

/**
 * Data about points in 3D space. Each Euclidean is treated like a vector for
 * the purposes of comparison and many geometry functions.
 * <p> The {@link #unitSize} of a unit vector is variable.
 * <p> {@link #equals(Object)} comparison is done with {@link Double#equals(Object)}
 * for each of the three components but another function is available {@link #isAbout(Euclidean)}
 * to compare another Euclidean within a range determined by {@link #Epsilon}
 * 
 * @author Levi Lindsley
 *
 */
class Euclidean implements Serializable{
	/**Generated serialVersionUID for serialization*/
	private static final long serialVersionUID = 5920030636109587966L;
	/**
	 * This is the unitSize of a unit vector, by default it is set as 1.0
	 * but it may be useful when a different scale is required to change this
	 * value. There is no change when this value is changed but calls to
	 * {@link #unitVector()} will return scaled to unitSize.
	 * @see #changeUnitSize(Double)
	 * @see #Epsilon
	 */
	private static Double unitSize = 1.0;
	/**
	 * This is used to compare two Euclidean values using {@link #isAbout(Euclidean)}
	 * Epsilon is used as the bound for determining if the values are equivilent
	 * the value for Epsilon may vary per application so a function is avalible to 
	 * change it.
	 * <p> Epsilon will also auto-scale if unitSize is changed. 
	 * @see #changeEpsilon(Double)
	 * @see #unitSize
	 */
	private static Double Epsilon = 0.015*unitSize;
	/**Class tag for XML documents*/
	private static final String classTag = "Euclidean";
	
	/**The zero vector*/
	static final Euclidean ZERO = new Euclidean (0.0,0.0,0.0);
	
	Double x; 
	Double y;
	Double z;
	/**
	 * Default constructor, x,y, and z are set to 0.0
	 */
	Euclidean(){
		x = 0.0;
		y = 0.0;
		z = 0.0;
	}
	/**
	 * Sets x to i, y to j, and z to k
	 * @param i - value for x
	 * @param j - value for y
	 * @param k - value for z
	 */
	Euclidean(Double i, Double j, Double k){
		x = i;
		y = j;
		z = k;
	}
	/**
	 * Sets this to the same values as o
	 * @param o - Euclideans values to use
	 */
	Euclidean(Euclidean o){
		this.x = o.x.doubleValue();
		this.y = o.y.doubleValue();
		this.z = o.z.doubleValue();
	}
	/**
	 * Floating point constructor, converts i,j,and k into double and sets 
	 * x,y and z to the corresponding value.
	 * @param i - Value for x
	 * @param j - Value for y
	 * @param k - Value for z
	 */
	Euclidean(float i, float j, float k) {
		this.x = (double)i;
		this.y = (double)j;
		this.z = (double)k;
	}
	/**
	 * @param o - Euclidean to compare this with
	 * @return
	 * 		Angle of this as compared to o
	 */
	Double angle(Euclidean o){
		return Math.acos(this.dotProduct(o)/(this.length()*o.length()));
	}
	/**
	 * returns Euclidean that represents the angle of this compared to 
	 * o flattened into three planes, x-z, y-z, x-y.
	 * @param o - Euclidean to compare this with
	 * @return
	 * 		Euclidean of flat angles with x = x-z angle, y = y-z angle and 
	 * z = x-y angle.
	 */
	Euclidean planarAngle(Euclidean o){
		Euclidean planear = new Euclidean();
		Euclidean tmp1 = new Euclidean(this.x, 0.0,this.z);
		Euclidean tmp2 = new Euclidean(o.x,0.0,o.z);
		planear.x = Math.acos((tmp1.dotProduct(tmp2))/((tmp1.length())*(tmp2.length())));
		tmp1 = new Euclidean(0.0,this.y,this.y);
		tmp2 = new Euclidean(0.0,o.y,o.z);
		planear.y = Math.acos((tmp1.dotProduct(tmp2))/((tmp1.length())*(tmp2.length())));
		tmp1 = new Euclidean(this.x,this.y,0.0);
		tmp2 = new Euclidean(o.x,o.y,0.0);
		planear.z = Math.acos((tmp1.dotProduct(tmp2))/((tmp1.length())*(tmp2.length())));
//		System.out.println(Math.acos((tmp1.dotProduct(tmp2))/((tmp1.length())*(tmp2.length()))));
		return planear;
	}
	/**
	 * @return
	 * 		Angle of this as compared to origin
	 * @see #angle()
	 */
	Double angle(){
		return angle(ZERO);
	}
	/**
	 * Calculates and returns the length of the vector represented by this.
	 * The length calculation is done in the standard fashion of 
	 * sqrt(x*x+y*y+z*z).
	 * @return
	 * 		length of this.
	 */
	Double length(){
		return Math.sqrt((x*x)+(y*y)+(z*z));
	}
	/**
	 * Calculates the linear distance between this and o. The calculation
	 * is the length of the vector created by subtracting o from this.
	 * @param o - Euclidean to compare this with
	 * @return
	 * 		Distance between this and o as a Double.
	 */
	Double distance(Euclidean o){
		return new Euclidean(this.x-o.x,this.y-o.y, this.z-o.z).length();
//		return Math.sqrt(Math.pow(this.x-o.x,2)+Math.pow(this.y-o.y,2)
//				+Math.pow(this.z-o.z,2));
	}
	/**
	 * Scales this by o, each value of this is scaled by the corresponding 
	 * value of o. 
	 * @param o - Euclidean to scale by
	 */
	void scale(Euclidean o){
		this.x *= o.x;
		this.y *= o.y;
		this.z *= o.z;
	}
	/**
	 * Scales this by a constant value
	 * @param s - Scale factor
	 */
	void scale(Double s){
		this.x *= s;
		this.y *= s;
		this.z *= s;
	}
	/**
	 * Scales a 1.0 unit vector to a unitSize unit vector. Technically 
	 * scales anything by unitSize.
	 */
	private void scale(){
		this.x *= unitSize;
		this.y *= unitSize;
		this.z *= unitSize;
	}
	/**
	 * translates coordinate of this by the coordinates of o
	 * @param o - Euclidean to use as transform
	 */
	void translate(Euclidean o){
		this.x += o.x;
		this.y += o.y;
		this.z += o.z;
	}
	/**
	 * Creates a new Euclidean from the coordinates of this multiplied by -1.
	 * <p> The coordinates of this are unchanged.
	 * @return
	 *  Inverse of this such that 
	 * this.translate(this.inverse()) yields zero
	 */
	Euclidean inverse(){
		return new Euclidean(x*-1,y*-1,z*-1);
	}
	/**
	 * Creates a unitVector of this, the unitVectors scale is determined by
	 * {@link #unitSize}
	 * @return
	 * 		Returns this scaled to a unit vector
	 */
	Euclidean unitVector(){
		Double l = length();
		Euclidean e = new Euclidean(x/l,y/l,z/l);
		e.scale();
		return e;
	}
	/**
	 * Creates and returns the cross product between this and o.
	 * <p> The coordinates of this and o are not changed.
	 * @param o Euclidean to cross this with
	 * @return
	 * 		new Euclidean of this cross o
	 */
	Euclidean crossProcuct(Euclidean o){
		Euclidean s = new Euclidean(0.0, 0.0, 0.0);
		s.x = this.y*o.z - this.z*o.y;
		s.y = this.z*o.x - this.x*o.z;
		s.z = this.x*o.y - this.y*o.x;
		return s;
	}
	/**
	 * Returns dot product between this and o.
	 * @param o Euclidean to take dot produce with
	 * @return
	 * 		Dot produce as a {@link Double}
	 */
	Double dotProduct(Euclidean o){
		return (this.x*o.x)+(this.y*o.y)+(this.z*o.z);
	}
	/**
	 * Categorizes each of the coordinates of this as positive, negative
	 * or zero.
	 * <p> This is NOT {@link #unitVector()} and performs a completely 
	 * different operation.
	 * @return
	 * 	Euclidean representing the sign value of this
	 */
	Euclidean unitize(){
		return new Euclidean(compare(x,0.0),compare(y,0.0),compare(z,0.0));
	}
	/**
	 * checks if a is greater than, less than or equivalent to b.
	 * @param a - Double value
	 * @param b - Value to compare a to
	 * @return
	 * 		-1 if a < b
	 * <p> 0 a == b
	 * <p> 1 a > b
	 */
	private int compare(Double a, Double b){
		int c = a.compareTo(b);
		if (c == 0)
			return 0;
		if (c < 0)
			return -1;
		return 1;
	}
	/**
	 * Averages a collection of Euclidean values and returns that average.
	 * <p> The collection c is not modified in any manner.
	 * @param c - Collecion&ltEuclidean&gt of values to average
	 * @return
	 * 		Average of all Euclideans in c
	 */
	static Euclidean average(Collection<Euclidean> c){
		//new empty Euclidean
		Euclidean a = new Euclidean();
		
		//for all elements in c translate e by that value, in other words
		//add all the coordinates together.
		for (Euclidean e : c){
			a.translate(e);
		}
		//the scale factor is 1/c.size() so that when a is scaled it is
		//divided by the number of elements that are in the collection
		//as scale is a multiply operation the value must be the inverse to 
		//divide
		Double inv = 1.0/c.size();
		
		//divide a by the size of the collection
		a.scale(inv);
		return a;
	}
	/**
	 * Compares the coordinates of this with the coordinates of o. The comparison
	 * is +- {@link #Epsilon} thus to return true all of the coordinates  of 
	 * this must be within Epsilon of the corresponding coordinate of o.
	 * @param o - Euclidean to compare this with
	 * @return
	 * 		True if the coordinates of this = the coordinates of o +- Epsilon.
	 * 	<p> False otherwise
	 */
	boolean isAbout(Euclidean o){
		return (isAbout(this.x, o.x) && isAbout(this.y, o.y) && isAbout(this.z,o.z));
	}
	/**
	 * Determines if a is within {@link #Epsilon} of b.
	 * @param a - Double value
	 * @param b - Double value to compare to a
	 * @return
	 * 		True if a == b +- Epsilon
	 * <p> False else
	 * @see #isAbout(Euclidean)
	 */
	private boolean isAbout(Double a, Double b){
		Double ub = b+Epsilon;
		Double lb = b-Epsilon;
		if ((ub.compareTo(a)>=0) && ((lb.compareTo(a))<=0))
				return true;
		return false;
	}
	/**
	 * Determines if this is bounded above and below by lb and ub. The bounds
	 * are not strict so lb could be the upper bound as long as ub is then the
	 * lower bound so if this returns true then lb and ub are guaranteed to bound
	 * this but which is the uppper and which is the lower bound is not known.
	 * 
	 * <p> The bounds are within {@link #Epsilon} error. 
	 * 
	 * @param lb - Euclidean forming lower bound
	 * @param ub - Euclidean forming upper bound
	 * @return
	 * 		True if this is bounded by lb and ub
	 * <p> False otherwise
	 * 
	 * @see #isTightBoundedBy(Euclidean, Euclidean)
	 */
	boolean isBoundedBy(Euclidean lb, Euclidean ub){
		return (isEpsilonBound(ub.x, this.x, lb.x) && isEpsilonBound(ub.y, this.y, lb.y)
				&& isEpsilonBound(ub.z, this.z, lb.z));
	}
	/**
	 * Determines if val is bounded by ub and lb. The bounds are not strictly
	 * in that order so ub may be the lower bound but if that is so then lb 
	 * must be the upper bound so if this returns true then val is bounded by
	 * ub and lb but which is the upper bound and which is the lower bound is
	 * unknown.
	 * 
	 * <p> The bounds are within {@link #Epsilon} precision.
	 * 
	 * @param ub - Upper bound
	 * @param val - value to check if bounded
	 * @param lb - Lower bound
	 * @return
	 * 		True if val is bounded by ub and lb.
	 * <p> False otherwise
	 * @see #isBoundedBy(Euclidean, Euclidean)
	 */
	private boolean isEpsilonBound(Double ub, Double val, Double lb){
		if (ub.compareTo(lb) < 0){
			Double tmp = ub;
			ub = lb;
			lb = tmp;
		}
		return isBoundedBy(ub+(Epsilon*.5), val, lb-(Epsilon*.5));
	}
	/**
	 * Determines if val is bounded by ub and lb. The bounds are not strictly
	 * in that order so ub may be the lower bound but if that is so then lb 
	 * must be the upper bound so if this returns true then val is bounded by
	 * ub and lb but which is the upper bound and which is the lower bound is
	 * unknown.
	 * 
	 * <p> The bounds are exact and thus not influenced by {@link #Epsilon}.
	 * 
	 * @param ub - Upper bound
	 * @param lb - Lower bound
	 * @return
	 * 		True if val is bounded by ub and lb.
	 * <p> False otherwise
	 * @see #isBoundedBy(Euclidean, Euclidean)
	 */
	boolean isTightBoundedBy(Euclidean lb, Euclidean ub){
		return (isBoundedBy(ub.x, this.x, lb.x) && isBoundedBy(ub.y, this.y, lb.y) 
				&& isBoundedBy(ub.z, this.z, lb.z));
	}
	/**
	 * Determines if val is bounded by ub and lb. The bounds are not strictly
	 * in that order so ub may be the lower bound but if that is so then lb 
	 * must be the upper bound so if this returns true then val is bounded by
	 * ub and lb but which is the upper bound and which is the lower bound is
	 * unknown.
	 * 
	 * <p> The bounds are exact and thus not influenced by {@link #Epsilon}.
	 * 
	 * @param ub - Upper bound
	 * @param val - value to check if bounded
	 * @param lb - Lower bound
	 * @return
	 * 		True if val is bounded by ub and lb.
	 * <p> False otherwise
	 * 
	 * @see #isTightBoundedBy(Euclidean, Euclidean)
	 */
	private boolean isBoundedBy(Double lb, Double val, Double ub){
		if (ub.compareTo(lb)< 0 ){
			Double tmp = ub;
			ub = lb;
			lb = tmp;
		}
		return (lb.compareTo(val) <= 0 && val.compareTo(ub) <= 0 );
	}
	public boolean equals(Object o){
		if (o == null)
			return false;
		if (! (o instanceof Euclidean))
			return false;
		Euclidean other = (Euclidean)o;
		return (x.equals(other.x)&&y.equals(other.y)&&z.equals(other.z));
	} 
	public int hashCode(){
		Long bits = Double.doubleToLongBits(x);
		bits  = bits ^ Double.doubleToLongBits(y);
		bits = bits ^ Double.doubleToLongBits(z);
		return (int)(bits ^ (bits >>>32));
	}
	/**
	 * @return
	 * 	Double value of {@link #Epsilon}
	 */
	public static Double getEpsilon(){
		return Epsilon.doubleValue();
	}
	/**
	 * Changes {@link #Epsilon} by the value of delta.
	 * @param delta - Amount to change Epsilon by
	 */
	public static void changeEpsilon(Double delta){
		Epsilon += delta;
	}
	/**@return Double value of unitSize*/
	public static Double getUnitSize(){
		return unitSize.doubleValue();
	}
	/**
	 * Changes unitSize by delta (unitSize += delta).
	 * @param delta value to change unitSize by
	 */
	public static void changeUnitSize(Double delta){
		Epsilon /= unitSize;
		unitSize += delta;
		Epsilon *= unitSize;
	}
	/**
	 * Sets unitSize to 1.0
	 */
	public static void resetUnitSize(){
		unitSize = 1.0;
	}
	public String toString(){
		return "<"+x+", "+y+", "+z+">";
	}
	public static Euclidean load(Scanner xmlInput){
		Euclidean e = new Euclidean();
		String next = xmlInput.next();
		while (next.compareTo("<"+Euclidean.classTag+">")!=0){
			if (!xmlInput.hasNext())
				return null;
			next = xmlInput.next();
		}
		e.x = Double.parseDouble(xmlStatics.parseElement(xmlInput));
		e.y = Double.parseDouble(xmlStatics.parseElement(xmlInput));
		e.z = Double.parseDouble(xmlStatics.parseElement(xmlInput));
		xmlInput.next();//</classTag>
		return e;
	}
	public String toXML(){
		String content = new String();
		content += "<"+classTag+">"+'\n';
		content += xmlStatics.createElement("x", x.toString());
		content += xmlStatics.createElement("y", y.toString());
		content += xmlStatics.createElement("z", z.toString());
		content += "</"+classTag+">"+'\n';
		return content;
	}
}