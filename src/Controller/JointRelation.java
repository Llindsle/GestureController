package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import controller.xmlGestureParser.xmlStatics;

/**
 * Class to hold various information about joins and relation to each other
 * and the other actions in sequence.
 * 
 * @author Levi Lindsley
 *
 */
class JointRelation{
	private static boolean debug = true;
	final private String classTag = "p";
	
	/**
	 * This is used to pan through the different types of comparisons
	 * The best choice at current seems to be 0x1, 0x3, 0x7 each increase slightly
	 * more bounded than the previous.
	 */
	final static int Interpretation = 0x1;
	private enum AngleType{
		//Best
		CROSS_PRODUCT(0x1),
		//Good
		UNIT_VECTOR(0x2),
		/* This is the old grid system
		 * still works good unless you hover around a boundary or never
		 * change zones so in other words, not really thats why other systems
		 * got added.
		 */
		GRID(0x4),
		//this is really bad with the epsilon needed for the others
		//the bound is to tight and it registers a gesture
		ANGLE_2D(0x8); 
		
		private int mask;

		AngleType(int i){
			this.mask = i;
		}
	}
	
	
	/**Pair of SimpleOpenNI joints */
	Pair J;
	
//	Euclidean offset;
	/** */
	List<Euclidean> angle;
	
	/**Describes the AngleType associated with each of the Euclideans stored
	 * in the angle list
	 */
	List<Integer> angleType;
	
	/**Determines if this action is concurrent with the action directly after it */
//	Boolean C;
	
	/**Set to the previous appearance of ( JointOne, JointTwo ) */
	Pair prev;
	
	/**
	 * Default no argument constructor cause this is a handy thing to have, 
	 * it just sets everything to null or a null like value.
	 */
	JointRelation(){
		J = null;
		angle = null;
		angleType = null;
//		offset = new Euclidean();
//		C = false;
		prev = null;
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
	JointRelation(Pair j,Euclidean pointOne, Euclidean pointTwo){
		J = j;
		
//		offset = new Euclidean(pointTwo);
//		offset.translate(pointOne.inverse());
//		offset = offset.unitVector();
		angle = new ArrayList<Euclidean>();
		angleType = new ArrayList<Integer>();
		
		//Add all AngleType that are appropriate for Interpretation 
		if ((AngleType.CROSS_PRODUCT.mask & Interpretation)!= 0){
			angle.add(pointOne.unitVector().crossProcuct(pointTwo.unitVector()));
			angleType.add(AngleType.CROSS_PRODUCT.mask);
		}
		if ((AngleType.ANGLE_2D.mask & Interpretation)!= 0){
			Euclidean tmp = pointOne.planarAngle(pointTwo);
//			tmp.x = 0.0;
//			tmp.y = 0.0;
			angle.add(tmp);
			angleType.add(AngleType.ANGLE_2D.mask);
		}
		if ((AngleType.GRID.mask & Interpretation)!= 0){
			Euclidean tmp = new Euclidean(pointOne);
			tmp.translate(pointTwo.inverse());
			angle.add(tmp.unitize());
			angleType.add(AngleType.GRID.mask);
		}
		if ((AngleType.UNIT_VECTOR.mask & Interpretation)!= 0){
			Euclidean tmp = new Euclidean();
			tmp = pointOne.unitVector();
			tmp.translate(pointTwo.unitVector().inverse());
			angle.add(tmp);
			angleType.add(AngleType.UNIT_VECTOR.mask);
		}
		//angle = new Euclidean(pointOne).angle(pointTwo);
		
//		C = conn;
		prev = null;
	}
	/**
	 * Sets the previous value this is found after the JointOne and JointTwo are know
	 * so it is called after the constructor but could be included in the constructor 
	 * at a later time.
	 * 
	 * @param p : The value to set previous to
	 */
	void setPrev(Pair p){
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
	public boolean equals(Object o){
		if (!(o instanceof JointRelation))
			return false;
		
		JointRelation other = (JointRelation)o;
		
		boolean e;
		e = this.J.equals(other.J);
		e = e && this.angle.equals(other.angle);
//		e = e && this.C==other.C;
		e = e && this.prev.equals(other.prev);
		return e;
	}
	public String toString(){
		String ret = new String();
		ret += "{"+(J==null ? "null":J.toString())+" ";
//		ret += (offset==null ? "null":offset.toString());
		ret += " A:"+(angle==null ? "null":angle.toString());
//		ret += " C:"+C;
		ret += " P:"+prev+"}";
		return ret;
	}
	public static JointRelation load(Scanner xmlInput){
		JointRelation jR = new JointRelation();
		String next = xmlInput.next();
		while (next.compareTo("<"+jR.classTag+">")!=0){
			if (!xmlInput.hasNext())
				return null;
			next = xmlInput.next();
		}
		jR.J = Pair.load(xmlInput);
		
		next = xmlInput.next();
		jR.angle = new ArrayList<Euclidean>();
		//so this only works for a one length angle list but it does work
			Euclidean e = Euclidean.load(xmlInput);
			jR.angle.add(e);
		next = xmlInput.next(); //</angle>
		if (debug) System.out.println(next);
		xmlInput.next();//<prev>
		jR.prev = Pair.load(xmlInput);
		xmlInput.next();//</prev>
//		xmlInput.next();//</classTag>
		return jR;
	}
	public String toXML(){
		String content = new String();
		content += "<"+classTag+">"+'\n';
		content += J.toXML();
//		content += offset.toXML();
		content += "<angle>"+'\n';
		for (Euclidean e: angle){
			content += e.toXML();
		}
		content += "</angle>"+'\n';
//		content += xmlStatics.createElement("c", C.toString());
		if (prev == null)
			content += xmlStatics.createElement("prev", "null");
		else
			content += xmlStatics.createElement("prev", prev.toXML());
		content +="</"+classTag+">"+'\n';
		return content;
	}
}