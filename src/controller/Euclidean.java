package controller;

import java.util.Collection;

import controller.xmlGestureParser.xmlStatics;

/**
 * Data about points in 3D space
 * 
 * @author Levi Lindsley
 *
 */
class Euclidean{
	private static Double Epsilon = 0.015;
	private static final String classTag = "Euclidean";
	
	static final Euclidean ZERO = new Euclidean (0.0,0.0,0.0);
	
	Double x;
	Double y;
	Double z;
	Euclidean(){
		x = 0.0;
		y = 0.0;
		z = 0.0;
	}
	Euclidean(Double i, Double j, Double k){
		x = i;
		y = j;
		z = k;
	}
	Euclidean(Euclidean o){
		this.x = o.x.doubleValue();
		this.y = o.y.doubleValue();
		this.z = o.z.doubleValue();
	}
	Euclidean(float i, float j, float k) {
		this.x = (double)i;
		this.y = (double)j;
		this.z = (double)k;
	}
	/**
	 * 
	 * @param o
	 * @return
	 * 		Angle of this as compared to o
	 */
	Double angle(Euclidean o){
		return Math.acos(this.dotProduct(o)/(this.length()*o.length()));
	}
	/**
	 * returns Euclidean that represents the angle of this compared to 
	 * o flattened into three planes, x-z, y-z, x-y.
	 * @param o
	 * @return
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
	 *
	 * @return
	 * 		Angle of this as compared to origin
	 */
	Euclidean angle(){
		return null;
	}
	/**
	 * return length of this 
	 * @return
	 */
	Double length(){
		return Math.sqrt((x*x)+(y*y)+(z*z));
	}
	Double distance(Euclidean o){
		return Math.sqrt(Math.pow(this.x-o.x,2)+Math.pow(this.y-o.y,2)
				+Math.pow(this.z-o.z,2));
	}
	/**
	 * scales this by o
	 * @param o
	 */
	void scale(Euclidean o){
		this.x *= o.x;
		this.y *= o.y;
		this.z *= o.z;
	}
	void scale(Double s){
		this.x *= s;
		this.y *= s;
		this.z *= s;
	}
	/**
	 * translates coordinate of this by the coordinates of o
	 * @param o
	 */
	void translate(Euclidean o){
		this.x += o.x;
		this.y += o.y;
		this.z += o.z;
	}
	/**
	 * returns the inverse of this such that 
	 * this.translate(this.inverse()) yields zero
	 */
	Euclidean inverse(){
		return new Euclidean(x*-1,y*-1,z*-1);
	}
	/**
	 * scales this to a unit vector
	 * @return
	 * 		Returns this scaled to a unit vector
	 */
	Euclidean unitVector(){
		Double l = length();
		return new Euclidean(x/l,y/l,z/l);
	}
	/**
	 * returns cross product between this and o
	 * @param o
	 * @return
	 * 		this X o
	 */
	Euclidean crossProcuct(Euclidean o){
		Euclidean s = new Euclidean(0.0, 0.0, 0.0);
		s.x = this.y*o.z - this.z*o.y;
		s.y = this.z*o.x - this.x*o.z;
		s.z = this.x*o.y - this.y*o.x;
		return s;
	}
	/**
	 * returns dot product between vectors
	 * @param o
	 * @return
	 */
	Double dotProduct(Euclidean o){
		return (this.x*o.x)+(this.y*o.y)+(this.z*o.z);
	}
	Euclidean unitize(){
		return new Euclidean(compare(x,0.0),compare(y,0.0),compare(z,0.0));
	}
	private int compare(Double a, Double b){
		int c = a.compareTo(b);
		if (c == 0)
			return 0;
		if (c < 0)
			return -1;
		return 1;
	}
	static Euclidean average(Collection<Euclidean> c){
		Euclidean a = new Euclidean(0.0,0.0,0.0);
		for (Euclidean e : c){
			a.translate(e);
		}
		Double inv = 1.0/c.size();
		a.scale(inv);
		return a;
	}
	boolean isAbout(Euclidean o){
		return (isAbout(this.x, o.x) && isAbout(this.y, o.y) && isAbout(this.z,o.z));
	}
	private boolean isAbout(Double a, Double b){
		Double ub = b+Epsilon;
		Double lb = b-Epsilon;
		if ((ub.compareTo(a)>=0) && ((lb.compareTo(a))<=0))
				return true;
		return false;
	}
	boolean isBoundedBy(Euclidean lb, Euclidean ub){
		return (isEpsilonBound(ub.x, this.x, lb.x) && isEpsilonBound(ub.y, this.y, lb.y)
				&& isEpsilonBound(ub.z, this.z, lb.z));
	}
	private boolean isEpsilonBound(Double ub, Double val, Double lb){
		if (ub.compareTo(lb) < 0){
			Double tmp = ub;
			ub = lb;
			lb = tmp;
		}
		return isBoundedBy(ub+(Epsilon*.5), val, lb-(Epsilon*.5));
	}
	boolean isTightBoundedBy(Euclidean lb, Euclidean ub){
		return (isBoundedBy(ub.x, this.x, lb.x) && isBoundedBy(ub.y, this.y, lb.y) 
				&& isBoundedBy(ub.z, this.z, lb.z));
	}
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
	public static Double getEpsilon(){
		return Epsilon;
	}
	public static void changeEpsilon(Double delta){
		Epsilon += delta;
	}
	public String toString(){
		return "<"+x+", "+y+", "+z+">";
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