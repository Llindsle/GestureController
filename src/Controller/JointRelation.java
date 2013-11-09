package controller;

import controller.xmlGestureParser.xmlStatics;

/**
 * Class to hold various information about joins and relation to each other
 * and the other actions in sequence.
 * 
 * @author Levi Lindsley
 *
 */
class JointRelation{
	final private String classTag = "p";
	
	/**J Pair of SimpleOpenNI joints */
	JointPair J;
	
	/**X relationship between JointTwo and JointOne */
	Integer X;
	
	/**Y relationship between JointTwo and JointOne */
	Integer Y;
	
	/**Z relationship between JointTwo and JointOne */
	Integer Z;
	
	/**Determines if this action is concurrent with the action directly after it */
	Boolean C;
	
	/**Set to the previous appearance of ( JointOne, JointTwo ) */
	Integer prev;
	
	JointRelation(){
		J = null;
		X = 0;
		Y = 0;
		Z = 0;
		C = false;
		prev = -1;
	}
	/**
	 * The constructor called by Gesturecontroller.addPoint();
	 * @param J1 : SimpleOpenNI constant for a joint 
	 * @param J2 : SimpleOpenNI constant for a joint
	 * @param x : x relationship between J2 and J1
	 * @param y : y relationship between J2 and J1
	 * @param z : z relationship between J2 and J1
	 * @param conn : 
	 * Is this concurrent with the next action in sequence, should not be true for last action
	 */
	JointRelation(int J1, int J2,Integer x, Integer y, Integer z, boolean conn){
		J = new JointPair(J1,J2);
		X = x; 
		Y = y;
		Z = z;
		C = conn;
		prev = null;
	}
	/**
	 * Sets the previous value this is found after the JointOne and JointTwo are know
	 * so it is called after the constructor but could be included in the constructor 
	 * at a later time.
	 * 
	 * @param p : The value to set previous to
	 */
	void setPrev(int p){
		prev = p;
	}
	/**
	 * Determines if two instances have the same Joint locations used for finding the 
	 * previous value
	 * 
	 * @param o : Another P class to compare to
	 * @return
	 * True if BOTH joints in this and o are equivalent
	 */
	public boolean equalJoints(JointRelation o){
		return J.equals(o.J);
	}
	/**
	 * Determines if two instances have angular locations that are within Epsilon of each other
	 * 
	 * @param o : Other instance to compare to
	 * @return
	 * 		True if angles along all axes are equivalent where 
	 * 		this.X == o.X && this.Y == o.Y && this.Z == o.Z +- Epsion.
	 */
	public boolean equalsCoordinates(JointRelation o){
		boolean equal = false;
		if (!(this.X == null))
			equal = GestureController.comp(this.X, o.X) == 0;
		if (!(this.Y == null))
			equal  = equal && GestureController.comp(this.Y, o.Y) == 0;
		if (!(this.Z == null))
			equal = equal && GestureController.comp(this.Z, o.Z) ==0;
		return equal;
	}
	public boolean boundedBy(JointRelation lb, JointRelation ub){
		boolean bound = chkBounds(lb.X, this.X, ub.X);
		bound = bound && chkBounds(lb.Y, this.Y, ub.Y);
		bound = bound && chkBounds(lb.Z, this.Z, ub.Z);
		return bound;
	}
	public boolean chkBounds(int lb, int val, int ub){
		if (lb < ub){
			int tmp = lb;
			lb = ub;
			ub = tmp;
		}
		return lb <= val && val <= ub;
	}
	public String toString(){
		String ret = new String();
		ret += "{"+J.toString();
		ret += " < "+X+", "+Y+", "+Z+">";
		ret += " C:"+C+" P:"+prev+"}";
		return ret;
	}
	public String toXML(){
		String content = new String();
		content += "<"+classTag+">"+'\n';
		content += J.toXML();
		content += xmlStatics.createElement("x", X.toString());
		content += xmlStatics.createElement("y", Y.toString());
		content += xmlStatics.createElement("z", Z.toString());
		content += xmlStatics.createElement("c", C.toString());
		content += xmlStatics.createElement("prev", prev.toString());
		content +="</"+classTag+">"+'\n';
		return content;
	}
}