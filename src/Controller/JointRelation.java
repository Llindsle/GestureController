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
	
	Euclidean offset;
	Double angle;
	
	/**Determines if this action is concurrent with the action directly after it */
	Boolean C;
	
	/**Set to the previous appearance of ( JointOne, JointTwo ) */
	Integer prev;
	
	JointRelation(){
		J = null;
		angle = null;
		offset = null;
		C = false;
		prev = -1;
		angle = null;
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
	JointRelation(JointPair j,Euclidean pointOne, Euclidean pointTwo, boolean conn){
		J = j;
		
		offset = new Euclidean(pointTwo);
		offset.translate(pointOne.inverse());
		offset = offset.unitVector();
		
		angle = new Euclidean(pointOne).angle(pointTwo);
		
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
		boolean equal;
		equal = this.offset.isAbout(o.offset);
		equal = equal && (GestureController.comp(angle, o.angle)==0);
		return equal;
	}
	public boolean boundedBy(JointRelation lb, JointRelation ub){
		boolean bound = offset.isBoundedBy(lb.offset, ub.offset);
		bound = bound && chkBounds(lb.angle, this.angle, ub.angle);
		return bound;
	}
	public boolean chkBounds(double lb, double val, double ub){
		if (lb < ub){
			double tmp = lb;
			lb = ub;
			ub = tmp;
		}
		return lb <= val && val <= ub;
	}
	public String toString(){
		String ret = new String();
		ret += "{"+J.toString()+" ";
		ret += offset.toString();
		ret += " A:"+angle.toString();
		ret += " C:"+C+" P:"+prev+"}";
		return ret;
	}
	public String toXML(){
		String content = new String();
		content += "<"+classTag+">"+'\n';
		content += J.toXML();
		content += offset.toXML();
		content += xmlStatics.createElement("angle", angle.toString());
		content += xmlStatics.createElement("c", C.toString());
		content += xmlStatics.createElement("prev", prev.toString());
		content +="</"+classTag+">"+'\n';
		return content;
	}
}