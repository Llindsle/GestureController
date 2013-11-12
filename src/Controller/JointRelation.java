package controller;

import java.util.ArrayList;
import java.util.List;

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
	
	//These are used to pan through the different types of comparisons
	//The best choice at current seems to be 0x3 or 0x7
	final static int Interpretation = 0x1;
	private enum AngleType{
		//Good
		CROSS_PRODUCT(0x1),
		//Good
		UNIT_VECTOR(0x2),
		//This is the old grid system
		GRID(0x4),
		//this is really bad with the epsilon needed for the others
		//the bound is to tight and it registers a gesture
		ANGLE_2D(0x8); 
		
		private int mask;

		AngleType(int i){
			this.mask = i;
		}
	}
	
	
	/**J Pair of SimpleOpenNI joints */
	JointPair J;
	
//	Euclidean offset;
	List<Euclidean> angle;
	
	/**Determines if this action is concurrent with the action directly after it */
	Boolean C;
	
	/**Set to the previous appearance of ( JointOne, JointTwo ) */
	Integer prev;
	
	JointRelation(){
		J = null;
		angle = null;
//		offset = new Euclidean();
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
		
//		offset = new Euclidean(pointTwo);
//		offset.translate(pointOne.inverse());
//		offset = offset.unitVector();
		angle = new ArrayList<Euclidean>();
		if ((AngleType.CROSS_PRODUCT.mask & Interpretation)!= 0)
			angle.add(pointOne.unitVector().crossProcuct(pointTwo.unitVector()));
		if ((AngleType.ANGLE_2D.mask & Interpretation)!= 0){
			Euclidean tmp = pointOne.planarAngle(pointTwo);
//			tmp.x = 0.0;
//			tmp.y = 0.0;
			angle.add(tmp);
		}
		if ((AngleType.GRID.mask & Interpretation)!= 0){
			Euclidean tmp = new Euclidean(pointOne);
			tmp.translate(pointTwo.inverse());
			angle.add(tmp.unitize());
		}
		if ((AngleType.UNIT_VECTOR.mask & Interpretation)!= 0){
			Euclidean tmp = new Euclidean();
			tmp = pointOne.unitVector();
			tmp.translate(pointTwo.unitVector().inverse());
			angle.add(tmp);
		}
		//angle = new Euclidean(pointOne).angle(pointTwo);
		
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
		for (int i=0;i<angle.size();i++){
			if (!angle.get(i).isAbout(o.angle.get(i)))
				return false;
		}
		return true;
	}
	public boolean boundedBy(JointRelation lb, JointRelation ub){
		for(int i=0;i<this.angle.size();i++){
			if (!(this.angle.get(i).isBoundedBy(lb.angle.get(i), ub.angle.get(i))))
				return false;
		}
		return true;
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
		ret += "{"+(J==null ? "null":J.toString())+" ";
//		ret += (offset==null ? "null":offset.toString());
		ret += " A:"+(angle==null ? "null":angle.toString());
		ret += " C:"+C+" P:"+prev+"}";
		return ret;
	}
	public String toXML(){
		String content = new String();
		content += "<"+classTag+">"+'\n';
		content += J.toXML();
//		content += offset.toXML();
		content += xmlStatics.createElement("angle", angle.toString());
		content += xmlStatics.createElement("c", C.toString());
		content += xmlStatics.createElement("prev", prev.toString());
		content +="</"+classTag+">"+'\n';
		return content;
	}
}