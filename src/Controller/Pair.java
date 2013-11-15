package controller;

import java.util.Vector;

import controller.xmlGestureParser.xmlStatics;


/**
 * Class used to hold pairs of joints as all recordings are done in two
 * joint format currently this allows for comparison between linked joints and
 * access to both joints being compared. The joint represented by First is compared
 * to the joint represented by Second for gesture purposes. 
 * 
 * @author Levi Lindsley
 *
 */
class Pair{
	final private String classTag = "j";
	/** First Joint in focus */
	Integer First;
	
	/** Second Joint in focus */
	Integer Second;

	/**
	 * Initialize First and Second with the values f and s
	 * @param f : Value to initialize First with
	 * @param s : Value to initialize Second with
	 */
	Pair(int f, int s){
		First = new Integer(f);
		Second = new Integer(s);
	}
	/**
	 * Override of equals to check for comparison between two JointPairs
	 */
	@Override
	public boolean equals(Object o){
		if (o instanceof Pair){
			return (this.First.equals(((Pair)o).First) && this.Second.equals(((Pair)o).Second));
		}
		return false;
	}
	/**
	 * Override of hashCode because equals got an override now the hashCode will be
	 * equal when equals() returns true
	 */
	@Override
	public int hashCode(){
		Vector<Integer> h = new Vector<Integer>();
		h.add(First);
		h.add(Second);
		return h.hashCode();
	}
	/**Prints out first and second being enclosed by chevrons*/
	@Override
	public String toString(){
		return "<"+First+", "+Second+">";
	}
	/**
	 * Creates and xml representation of this with no default leading tabs
	 * @return String xml representation of this
	 * @see Pair#toXML(String)
	 */
	public String toXML(){
		String context = new String();
		context +="<"+classTag+">"+'\n';
		context += xmlStatics.createElement("first", First.toString());
		context += xmlStatics.createElement("second", Second.toString());
		context +="</"+classTag+">"+'\n';
		return context;
	}
}
