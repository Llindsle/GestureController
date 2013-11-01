package Controller;

import java.util.Vector;

import processing.core.PVector;
import SimpleOpenNI.*;

/**
 * A class to create and recognize simple gestures using the relationship between two joints
 * provided by SimpleOpenNI.
 * 
 * An instance of this class should be used for each individual gesture.
 *
 * Currently does NOT work with multiple users.
 * 
 * @author Levi Lindsley
 *
 */
public class GestureController{
	/** Used only to toggle debug output */
	@SuppressWarnings("unused")
	private boolean debug = false;
	
	/**
	 * Class to hold various information about joins and relation to each other
	 * and the other actions in sequence.
	 * Creates as a class because it is easier to work with and expand as needed
	 * 
	 * @author Levi Lindsley
	 *
	 */
	class P{
		/**J Pair of SimpleOpenNI joints */
		JointPair J;
		/**X relationship between JointTwo and JointOne */
		Integer X;
		/**Y relationship between JointTwo and JointOne */
		Integer Y; 
		/**Z relationship between JointTwo and JointOne */
		Integer Z;
		/**Determines if this action is concurrent with the action directly after it */
		boolean C;
		/**Set to the previous appearance of ( JointOne, JointTwo ) */
		int prev; 
		
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
		P(int J1, int J2,Integer x, Integer y, Integer z, boolean conn){
			J = new JointPair(J1,J2);
			X = x; 
			Y = y;
			Z = z;
			C = conn;
		}
		/**
		 * Sets the previous value this is found after the JointOne and JointTwo are know
		 * so it is called after the constructor but could be inclueded in the constructor 
		 * at a later time.
		 * 
		 * @param p : 
		 * The value to set previous to
		 */
		void setPrev(int p){
			prev = p;
		}
		/**
		 * Determines if two instances have the same Joint locations used for finding the 
		 * previous value
		 * 
		 * @param o : 
		 * Another P class to compare to
		 * @return
		 * True if BOTH joints in this and o are equivalent
		 */
		public boolean equalJoints(P o){
			return J.equals(o);
		}
	}
	
	class JointPair{
		Integer First;
		Integer Second;

		JointPair(int f, int s){
			First = new Integer(f);
			Second = new Integer(s);
		}
		
		public boolean equals(Object o){
			if (o instanceof JointPair)
				return (this.First == ((JointPair)o).First && this.Second == ((JointPair)o).Second);
			return false;
		}
		 
		public int hashCode(){
			Vector<Integer> h = new Vector<Integer>();
			h.add(First);
			h.add(Second);
			return h.hashCode();
		}
	}
	
	/**The Sequence if joint relationships describing the gesture */
	private Vector<P> seq;
	/**A list of constant positions that must be true for the gesture to complete*/
	private Vector<P> con; 
	
	/**Name Identifier of the Gesture*/
	public String Name;

	/** 
	 * Epsilon used to widen zero, modifying this will make zero have a larger range and thus
	 * be easier to hit but will decreace the sensitivity of noting when joints are not alligned
	 * It may be beneficial to split this into an x,y,z epsilon.
	 */
	private static Integer ep = 15; 
	/**Number of completed steps of the gesture */
	private Integer step; 

	/**
	 * Default Constructor:
	 * Initializes the control arrays and sets step to 0
	 */
	public GestureController(){
		init();
		Name = null;
	}
	/**
	 * Constructor sets name to N
	 * @param N
	 */
	public GestureController(String N){
		init();
		Name = N;
	}
	/**
	 * Initializes both seq and con vector and sets step to 0.
	 * Used with constructor and may be useful for reseting gesture sequences.
	 */
	private void init(){
		seq = new Vector<P>();
		con = new Vector<P>();
		step = 0;
	}
	/**
	 * Adds a joint relationship to the seq array.
	 * @param J1 : SimpleOpenNI constant for a joint
	 * @param J2 : SimpleOpenNI constant for a joint
	 * @param x : Valid values {-1,0,1}
	 * @param y : Valid values {-1,0,1} 
	 * @param z : Valid values {-1,0,1}
	 * @param conn :
	 *  Determines if this is concurrent with the gesture after it should be false for last gesture
	 */
	public void addPoint(int J1, int J2, 
			Integer x, Integer y, Integer z, boolean conn){
		int loc=-1;
		P tmp = new P(J1,J2,x,y,z,conn); //Create new tmp point using values given
		
		//Find the last appearance of the given joint pair in the seq array
		for(int i=seq.size()-1;i>=0;i--){
			if (tmp.equalJoints(seq.get(i))){
					loc = i;
					break;
			}
		}
		tmp.setPrev(loc); //set the previous location to the one found or -1 if not found
		seq.add(tmp); //add to seq array
	}
	/**
	 * Add a Joint relationship to con vector. The x,y,z points may be null and if they are
	 * the null axis will be ignored this is so a constant constraint can use fewer constraint
	 * axis if it is suitable for the gesture.
	 * 
	 * @param J1 : SimpleOpenNI constant for a joint
	 * @param J2 : SimpleOpenNI constant for a joint
	 * @param x : Valid values {-1,0,1} or null which translates into ignore this axis
	 * @param y : Valid values {-1,0,1} or null which translates into ignore this axis
	 * @param z : Valid values {-1,0,1} or null which translates into ignore this axis
	 */
	public void addConstant(int J1, int J2,
			Integer x, Integer y, Integer z){
		con.add(new P(J1,J2, x,y,z,false));
	}
	
	/**
	 * This function should be called to check for gesture completion and to update the gesture.
	 * 
	 * Checks if the gesture designated is complete or not. If it is not complete then the skeletal
	 * model from context is checked against a the current step of the gesture if the skeletal model
	 * matches then the step is incremented. If the retrieved skeletal position is between the current
	 * point and the previous competed point for the joint pair then the gesture is incremented or reset
	 * it hits a 'holding pattern' waiting for success or failure. If the gesture does not match either of
	 * the previous two conditions then it is assumed that the gesture failed and is reset accordingly.
	 * 
	 * @param context : A SimpleOpenNI instance
	 * @param user : Id for users to check skeleton
	 * @return
	 * 	True if gesture is compete.
	 * 	Gesture is reset if complete. 
	 *  Gesture is incremented if not complete. 
	 */
	public boolean isComplete(SimpleOpenNI context, int user){
		
		//if step == seq.size then all steps of the gesture have been completed
		//reset and return true
		if (step == seq.size()){
			step = 0; //reset gesture
			return true; //return the successful completion
		}
		
		Boolean N = next(context, user);//call next function
		
		
		boolean wait = false; //Used to initiate holding pattern on concurrent gestures
		int Hold=step; //The holding pattern location for a concurrent gesture
		
		/*
		 * Checks for concurrent gesture completion, holding, or failure
		 * Note the step-1 this is to check if the previous gesture is  listed as  concurrent
		 * with the current gesture so that there is a non-concurrent gesture between sets of
		 * concurrent gestures
		 */
		while (step > 0 && step < seq.size() &&seq.get(step-1).C){
			//Some part of the gesture failed, reset the entire gesture
			if (N == false){
				step = 0; //Reset gesture
				break; //end the while loop a step failed
			}
			/*
			 * A part hit a holding pattern thus forcing the entire concurrent sequence into
			 * a holding pattern but all parts of the concurrent sequence must be checked for
			 * failure before so can't stop the while loop
			 */
			if (N == null){
				wait = true; //holding pattern true
			}
			step ++; //continue to the next part of the gesture
		}
		
		// If any part hit a holding patters reset the concurrent gesture to the first gesture
		// in the sequence as remembered by Hold
		if (wait){
			step = Hold; 
		}
		
		// Check to assure that the constant bounds are being upheld
		for (P c : con){
			//if the constants are being violated the reset the gesture
			if (!constMatch(c,context,user)){
				step = 0; //reset gesture
			}
		}
		//The gesture was not finished return false
		return false;
	}
	
	/**
	 * Manually restart the gesture may be useful to impose other restrictions on the gesture
	 * that the controller does not account for such as being within a bounding box or holding
	 * an object
	 */
	public void reset(){
		step = 0; //reset gesture
	}
	
	/**
	 * Compares to floating point values taking ep (Epsilon) into account for equality.
	 * Returns the relationship proper between x and x2 (x {=, <, >} x2).
	 * This function may be modified to get range relation instead of relationship.
	 * 
	 * @param x : A floating point value to be compared
	 * @param x2 : A floating point value to compare against
	 * @return
	 * 		0 : values were within the range given by ep
	 * 		1 :  x > (x2+ep)
	 * 		-1 : x < (x2-ep)
	 */
	protected static int comp(float x, float x2){
		if ((x2+ep)>= x && (x2-ep) <= x)
			return 0;
		else if ((x2+ep) < x )
			return 1;
		else 
			return -1;
	}
	
	private static int angle(float alphaX, float alphaY, float betaX, float betaY){
		float a = alphaY-betaY;
		float b = alphaX-betaX;
		double h = Math.sqrt(Math.pow(a,2)+Math.pow(b,2));
		return (int)Math.round(Math.toDegrees(Math.asin(a/h)));
	}
	protected static PVector compareJointPositions(PVector jointOne, PVector jointTwo) {
		
//		int x = comp(jointOne.x, jointTwo.x);
//		int y = comp(jointOne.y, jointTwo.y);
//		int z = comp(jointOne.z, jointTwo.z);
		
		int angleX = angle(jointOne.x, jointOne.z, jointTwo.x, jointTwo.z);
		int angleY = angle(jointOne.y, jointOne.z, jointTwo.y, jointTwo.z);
		int angleZ = angle(jointOne.x, jointOne.y, jointTwo.x, jointTwo.y);

		return new PVector(angleX, angleY, angleZ);
		//return new PVector(x,y,z);
	}
	
	/**
	 * Determines if a skeletal model taken from context matches a constant relation given
	 * by c.
	 * 
	 * @param c : A constant joint relationship should be from con vector
	 * @param context : SimpleOpenNI instance
	 * @param user : Id of user to get skeleton from context
	 * @return
	 * 		True if the relation ship matches given relationship from c
	 */
	private boolean constMatch(P c, SimpleOpenNI context, int user){

		//if not tracking user then that user auto fails
		if (!context.isTrackingSkeleton(user)){
			//if (debug) System.out.println("NOT TRACKING USER:"+user);
			step = 0; //reset gesture 
			return false;
		}
		
		//Get Joint Positions in converted format
		PVector JointOneReal = getRealCoordinites(context,user, c.J.First);
		PVector JointTwoReal = getRealCoordinites(context,user, c.J.Second);

		//compare two points
		PVector rel = compareJointPositions(JointOneReal, JointTwoReal);
		
		//if (debug) System.out.println("C: "+x+" "+y+" "+z);
		
		//If c.X is not null and the x relationship is incorrect the gesture fails 
		if (c.X !=null && comp(rel.x, c.X) != 0){
			return false;
		}
		//If c.Y is not null and the y relationship is wrong the gesture fails 
		if (c.Y != null && comp(rel.y, c.Y) != 0){
			return false;
		}
		//If c.Z is not null and the z relationship is incorrect the gesture fails 
		if (c.Z != null && comp(rel.z,  c.Z) != 0){
			return false;
		}
		//it did not fail thus it passed
		return true;
	}
	
	/**
	 * Check if the current step matches the relationship given by x,y,z perfectly 
	 * 
	 * @param x : The x axis relationship of the joints
	 * @param y : The y axis relationship of the joints
	 * @param z : The z axis relationship of the joints
	 * @return
	 * 		True if the x,y,z all match the respecive relationship given by the current
	 * step in the sequence
	 */
	private boolean stepMatch(PVector V){
		P target = seq.get(step); //get current step
		//check for x,y,and z matches againts target
		if (comp(V.x, target.X) ==0 && comp(V.y, target.Y)==0 && comp(V.z, target.Z)==0)
			return true; //match was good
		return false; // match failed
	}
	
	/**
	 * Checks to see if the given x,y,z relationships fall between the relationships given by
	 * the current step and the previous step. This is used to check if a gesture may have paused
	 * but not terminated such as the user is moving slow so they haven't made it to the next step
	 * but did not do anything wrong yet the controller just needs to wait on the user to finish the
	 * gesture.
	 * 
	 * @param x : The x axis relationship of the joints
	 * @param y : The y axis relationship of the joints
	 * @param z : The z axis relationship of the joints
	 * @return
	 * 		True if the given relationships fall between the current and previous step.
	 */
	private boolean midMatch(PVector V){
		
		P cur = seq.get(step); //get current step
		
		//First step of joint type
		if(cur.prev == -1){
			return false; //There is no middle ground on the start
		}
		
		//All Other steps
		P prev = seq.get(cur.prev); //get previous of equal joint pair step as denoted by cur.prev

		//check relation between all coordinates return true if all are within range else false
		return (chkCoord(cur.X, V.x, prev.X) && chkCoord(cur.Y, V.y, prev.Y) && chkCoord(cur.Z, V.z, prev.Z));
	}
	/**
	 * Checks if val is between cur and prev, the bounds do not need to be in any order thus
	 * if prev > cur, cur > prev, or cur = prev this will still return the proper value
	 * @param cur : bounding value
	 * @param val : value to check
	 * @param prev : bounding value
	 * @return
	 * 		True if val is between cur and prev 
	 */
	private boolean chkCoord(int cur, float val, int prev){
		//if the current > previous then check them in (prev <= val <= cur) 
		//else check (cur <= val <= prev) order
		if (cur > prev){
			if (!(prev <= val && val <= cur))
				return false;
		}
		else if (!(cur <= val && val <= prev)){
			return false;
		}
		return true;
	}
	
	/**
	 * Retrieves and converts 
	 * @param c
	 * @param user
	 * @param joint
	 * @return
	 */
	protected static PVector getRealCoordinites(SimpleOpenNI c, int user, int joint){
		//PVectors to store joint position data and converted position data
		PVector Joint = new PVector();
		PVector Real = new PVector();
		
		//get joint data from context as determined by the c
		c.getJointPositionSkeleton(user, joint, Joint);
		
		//convert data into realworld data this seems more useful for comparison
		//the raw data may work just as well though not sure so I use this
		c.convertRealWorldToProjective(Joint, Real);
		return Real;
	}
	/**
	 * Checks the next step to see if the skeletal model derived from context matches the expected
	 * values and increments step accordingly
	 * @param context : SimpleOpenNI instance
	 * @param user : Id of user to get skeletal model data from
	 * @return
	 * 		True if the gesture step completed and step was incremented
	 * 		False if the gesture failed and step was reset
	 * 		null if the gesture hit a holding pattern and step did not change
	 */
	private Boolean next(SimpleOpenNI context, int user){

		//if not tracking user then that user auto fails
		if (!context.isTrackingSkeleton(user)){
			//if (debug) System.out.println("NOT TRACKING USER:"+user);
			step = 0; //reset gesture 
			return false;
		}
		
		//Get Joint Positions in converted format
		PVector JointOneReal = getRealCoordinites(context,user, seq.get(step).J.First);
		PVector JointTwoReal = getRealCoordinites(context,user, seq.get(step).J.Second);

		//compare each joint locations at each axis
		PVector rel = compareJointPositions(JointOneReal, JointTwoReal);
		
		//  if (debug) System.out.println(JointOneReal.x+" "+JointOneReal.y+" "+JointOneReal.z);
		//  if (debug) System.out.println(x+" "+y+" "+z);


		//IF stepMach() Position is exactly what is expected
		if (stepMatch(rel)){
			//if (debug) System.out.println("step "+step+" good");
			step ++; //Increment Gesture
			return true;
		}

		//IF midMatch() Position is not quite right but not wrong yet either
		if (midMatch(rel)){
			// if (debug) System.out.println("holding pattern on step "+step);
			//  step = step; //maintain position
			return null;
		}

		//Position has nothing to do with what was expected
		//Gesture Failed 
		
		//   if (debug) System.out.println("step "+step+" failed");
		step = 0; //reset gesture
		return false;
	} 
	
	public void simplifyGesture(){
		GestureRecord log = new GestureRecord();
		for (P target : seq){
			log.addFocusJoints(target.J.First, target.J.Second);
			log.addNode(new JointPair(target.J.First, target.J.Second),
					new PVector(target.X, target.Y, target.Z));
		}
		GestureController gen = log.generateGesture();
		seq = gen.seq;
		con = gen.con;
		step = 0;
	}
	public void changeTolerance(int e){
		ep += e;
	} 
}