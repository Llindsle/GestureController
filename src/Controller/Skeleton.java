package controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import SimpleOpenNI.SimpleOpenNI;
import SimpleOpenNI.SimpleOpenNIConstants;
/**
 * An easier to manipulate enum representing the skeletal constants available 
 * in the SimpleOpenNIConstants. This enum provided easy selection of left and
 * right side also supports mirroring. It is possible to retrieve all points in
 * a category using {@link #getAllLeft()}, {@link #getAllRight()}, 
 * {@link #getAllCenter()}.
 * 
 * @bug This requires an active SimpleOpenNI instance that is currently running
 * in order to access the values in this enum. 
 * @author Levi Lindsley
 *
 */
public enum Skeleton {
	TEST(SimpleOpenNIConstants.SKEL_HEAD),
	HEAD(SimpleOpenNI.SKEL_HEAD),
	NECK(SimpleOpenNI.SKEL_NECK),
	TORSO(SimpleOpenNI.SKEL_TORSO),
	WAIST(SimpleOpenNI.SKEL_WAIST),
	ANKLE(SimpleOpenNI.SKEL_LEFT_ANKLE, SimpleOpenNI.SKEL_RIGHT_ANKLE),
	COLLAR(SimpleOpenNI.SKEL_LEFT_COLLAR, SimpleOpenNI.SKEL_RIGHT_COLLAR),
	ELBOW(SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_RIGHT_ELBOW),
	FINGERTIP(SimpleOpenNI.SKEL_LEFT_FINGERTIP, SimpleOpenNI.SKEL_RIGHT_FINGERTIP),
	FOOT(SimpleOpenNI.SKEL_LEFT_FOOT, SimpleOpenNI.SKEL_RIGHT_FOOT),
	HAND(SimpleOpenNI.SKEL_LEFT_HAND, SimpleOpenNI.SKEL_RIGHT_HAND),
	HIP(SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_RIGHT_HIP),
	KNEE(SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_RIGHT_KNEE),
	SHOULDER(SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_SHOULDER),
	WRIST(SimpleOpenNI.SKEL_LEFT_WRIST, SimpleOpenNI.SKEL_RIGHT_WRIST);
	
	/**Left Skeletal value*/
	private Integer LEFT;
	/**Right Skeletal value*/
	private Integer RIGHT;
	/**indicates if this is a single value or has diffrent left/right values*/
	private boolean single;
	/**
	 * Determines if the LEFT value should be retrieved by the general get()
	 * function
	 */
	static boolean default_Left=true;
	/**
	 * Creates a different LEFT/RIGHT values
	 * @param l - Value for LEFT
	 * @param r - Value for RIGHT
	 */
	Skeleton(int l, int r){
		LEFT = l;
		RIGHT = r;
		single = false;
	}
	/**
	 * Single value constructor
	 * @param v - Value to set both LEFT and RIGHT to.
	 */
	Skeleton(int v){
		LEFT  = v;
		RIGHT = v;
		single = true;
	}
	/**
	 * Fetches the default value determined by {@link #default_Left}. For
	 * a single value points this will always return the value.
	 * @return
	 * 		this.LEFT if (default_Left)
	 *  <p> this.RIGHT otherwise
	 */
	public Integer get(){
		if (default_Left)
			return LEFT;
		return RIGHT;
	}
	/**
	 * Retrieves the value of LEFT, if this is a single value point then 
	 * this will return null. 
	 * @return
	 * 		null if single value
	 * 	<p>	LEFT else
	 * 
	 * @see #get()
	 */
	public Integer left(){
		if (!single)
			return LEFT;
		return null;
	}
	/**
	 * Retrieves the value of RIGHT, if this is a single value point then 
	 * this will return null. 
	 * @return
	 * 		null if single value
	 * 	<p>	RIGHT else
	 * 
	 * @see #get()
	 */
	public Integer right(){
		if (!single)
			return RIGHT;
		return null;
	}
	/**
	 * Creates a set of values that is the LEFT value of all non-single values
	 * contained in this enum.
	 * @return
	 * 		Set of left values
	 */
	public static Set<Integer> getAllLeft(){
		Set<Integer> set = new TreeSet<Integer>();
		Skeleton skel [] = Skeleton.values();
		for (int i=0;i<skel.length;i++){
			if (!skel[i].single)
				set.add(skel[i].left());
		}
		return set;
	}
	/**
	 * Creates a set of values that is the RIGHT value of all non-single values
	 * contained in this enum.
	 * @return
	 * 		Set of right values
	 */
	public static Set<Integer> getAllRight(){
		Set<Integer> set = new TreeSet<Integer>();
		Skeleton skel [] = Skeleton.values();
		for (int i=0;i<skel.length;i++){
			if (!skel[i].single)
				set.add(skel[i].right());
		}
		return set;
	}
	/**
	 * Creates a set of all single values
	 * @return
	 * 		Set of center (single) values
	 */
	public static Set<Integer> getAllCenter(){
		Set<Integer> set = new TreeSet<Integer>();
		Skeleton skel [] = Skeleton.values();
		for (int i=0;i<skel.length;i++){
			if (skel[i].single)
				set.add(skel[i].get());
		}
		return set;
	}
	/**
	 * Sets {@link #default_Left} to true
	 * @see #get()
	 */
	public static void setDefaultLeft(){
		default_Left = true;
	}
	/**
	 * Sets {@link #default_Left} to false
	 * @see #get()
	 */
	public static void setDefaultRight(){
		default_Left = false;
	}
	/**
	 * Locates the opposing side value (left/right) that is contained in the
	 * enum values. If val is not represented or if it represents a center
	 * value then this returns null.  
	 * @param val - value to mirror
	 * @return
	 * 		Left/Right mirror of val
	 *  <p> null if no mirror-able point is located
	 *  
	 *  @see #mirror(Set)
	 */
	public static Integer mirror(Integer val){
		Skeleton skel [] = Skeleton.values();
		for (int i=0;i<skel.length;i++){
			if (val == skel[i].left())
				return skel[i].right();
			if (val == skel[i].right())
				return skel[i].left();
		}
		return null;
	}
	/**
	 * Creates a set of all values from val that could be mirrored OR were 
	 * found to be single values. This is more efficient that calling 
	 * {@link #mirror(Integer)} for each value in the set assuming the set
	 * is backed by an efficient look up manner ({@link TreeSet} or 
	 * {@link HashSet} for example).
	 * @param val - Set to attempt to mirror
	 * @return
	 * 		Set that contains all mirrored values OR single values. Values
	 *in val that are not represented by Skeleton are not represented here.
	 */
	public static Set<Integer> mirror(Set<Integer>val){
		Set<Integer> set = new TreeSet<Integer>();
		Skeleton skel [] = Skeleton.values();
		for (int i=0;i<skel.length;i++){
			if (skel[i].single){
				if (val.contains(skel[i].get()))
					set.add(skel[i].get());
			}
			else{
				if (val.contains(skel[i].left()))
					set.add(skel[i].right());
				if (val.contains(skel[i].right()))
					set.add(skel[i].left());
			}
		}
		return set;
	}
}
