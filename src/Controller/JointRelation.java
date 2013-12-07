package controller;

import java.io.Serializable;
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
class JointRelation implements Serializable{
	/**Generated serialVersionUID*/
	private static final long serialVersionUID = -1971456988738778271L;
	/**Used to toggle debug output*/
	private static boolean debug = true;
	/**XML classTag*/
	final private String classTag = "jR";
	
	/**
	 * This is used to pan through the different types of comparisons
	 * The best choice at current seems to be 0x1, 0x3, 0x7 each increase slightly
	 * more bounded than the previous.
	 * @see AngleType
	 */
	final static int Interpretation = 0x1;
	/**
	 * Enumerator to determine what type(s) of comparison is used when 
	 * creating a new JointRelation {@link JointRelation#Interpretation}
	 * is used to mask each value to determine which are added to
	 * {@link JointRelation#angle}.
	 * @author Levi Lindsley
	 *
	 */
	private enum AngleType{
		//Best
		/**mask 0x1*/
		CROSS_PRODUCT(0x1),
		//Good
		/**mask 0x2*/
		UNIT_VECTOR(0x2),
		/* This is the old grid system
		 * still works good unless you hover around a boundary or never
		 * change zones so in other words, not really thats why other systems
		 * got added.
		 */
		/**mask 0x4*/
		GRID(0x4),
		//this is really bad with the epsilon needed for the others
		//the bound is to tight and it registers a gesture
		/**mask 0x8*/
		ANGLE_2D(0x8); 
		
		private int mask;

		AngleType(int i){
			this.mask = i;
		}
	}
	
	
	/**Pair of SimpleOpenNI joints */
	Pair J;
	
	/**List of comparison types determined by {@link #Interpretation} */
	List<Euclidean> angle;
	
	/**Describes the AngleType associated with each of the Euclideans stored
	 * in the angle list
	 */
	List<Integer> angleType;
	
	/**Set to the previous appearance of ( JointOne, JointTwo ) */
	Pair prev;
	
	/**
	 * Default no argument constructor cause this is a handy thing to have, 
	 * it just sets everything to null.
	 */
	JointRelation(){
		J = null;
		angle = null;
		angleType = null;
		prev = null;
		angle = null;
	}
	/**
	 * Constructs the relation between the two joints represented by j, with
	 * the relation being determined by pointOne and pointTwo with type of 
	 * comparison between them determined by {@link #Interpretation}
	 * @param j	- Pair representing joints
	 * @param pointOne - Value representative of j.First
	 * @param pointTwo - Value representative of j.Second
	 */
	JointRelation(Pair j,Euclidean pointOne, Euclidean pointTwo){
		J = j;
		setAngle(pointOne,pointTwo);
		prev = null;
	}
	/**
	 * Sets the angle between the two points to the comparison type expressed
	 * by {@link #Interpretation}
	 * @param pointOne - Value representative of J.First
	 * @param pointTwo - Value representative of J.First
	 */
	void setAngle(Euclidean pointOne, Euclidean pointTwo){
		angle = new ArrayList<Euclidean>();
		angleType = new ArrayList<Integer>();
		
		//Add all AngleType that are appropriate for Interpretation 
		if ((AngleType.CROSS_PRODUCT.mask & Interpretation)!= 0){
			Euclidean tmp = (pointOne.unitVector().crossProcuct(pointTwo.unitVector()));
			angle.add(tmp);
			angleType.add(AngleType.CROSS_PRODUCT.mask);
		}
		if ((AngleType.ANGLE_2D.mask & Interpretation)!= 0){
			Euclidean tmp = pointOne.planarAngle(pointTwo);
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
			Euclidean tmp = pointOne.unitVector();
			tmp.translate(pointTwo.unitVector().inverse());
			angle.add(tmp);
			angleType.add(AngleType.UNIT_VECTOR.mask);
		}
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
	/**
	 * Determines if this is bounded above and below by lb and ub. The bounds
	 * are not strict so lb could be the upper bound as long as ub is then the
	 * lower bound so if this returns true then lb and ub are guaranteed to bound
	 * this but which is the uppper and which is the lower bound is not known.
	 * <p>
	 * The bounds are checked for all active AngleTypes and all must be bound this
	 * but the bounds of the different angle types need not be on the same side. 
	 * Thus there could be an upper bound that on the next angle type is a 
	 * lower bound due to the different types of calculation.
	 * 
	 * @param lb - JointRelation forming lower bound
	 * @param ub - JointRelation forming upper bound
	 * @return
	 * 		True if this is bounded by lb and ub
	 * <p> False otherwise
	 */
	public boolean boundedBy(JointRelation lb, JointRelation ub){
		for(int i=0;i<this.angle.size();i++){
			if (!(this.angle.get(i).isBoundedBy(lb.angle.get(i), ub.angle.get(i))))
				return false;
		}
		return true;
	}
	/**
	 * Determines if val is bounded by ub and lb. The bounds are not strictly
	 * in that order so ub may be the lower bound but if that is so then lb 
	 * must be the upper bound so if this returns true then val is bounded by
	 * ub and lb but which is the upper bound and which is the lower bound is
	 * unknown.
	 * 
	 * @param ub - Upper bound
	 * @param val - value to check if bounded
	 * @param lb - Lower bound
	 * @return
	 * 		True if val is bounded by ub and lb.
	 * <p> False otherwise
	 */
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