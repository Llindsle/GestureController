package controller;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import processing.core.PVector;
import SimpleOpenNI.*;

/**
 * A class to create and recognize simple gestures using the relationship between two joints
 * provided by SimpleOpenNI. Gestures are recognized by detecting a series of steps that form the
 * gesture. The steps are composed of two joints and relative position of the two focus joints.
 * A step may be declared to be consecutive with the next declared step so that the two must
 * be done at the same time, all consecutive steps appearing in sequence should have unique joint
 * pairs or it will not be possible to complete the gesture. If a step is declared concurrent there
 * must be a step following it.
 * Gestures may be further constrained by the addition of constant constraints that must be met
 * at every step or the gesture will be considered failed. Constant constraints are not required
 * to declare a relationship for all three axes, declaring an axis to be null will disregard that
 * axis while calculating if the constraint is upheld.
 *  
 * 
 * An instance of this class should be used for each individual gesture.
 *
 *Bugs:
 * Currently does NOT work with multiple users.
 * Probably some with relation to concurrent and constant constraints not much testing has been done
 * 
 * @author Levi Lindsley
 *
 */
public class GestureController implements xmlGestureParser<GestureController>{
	public enum CompressionType{NONE, SIMPLE, AVG, DBL_AVG};
	/** Used only to toggle debug output */
	@SuppressWarnings("unused")
	private boolean debug = false;
	
	final private String classTag = "gesture";
	
	/**The Sequence if joint relationships describing the gesture */
	private Vector<JointRelation> sequence; 
	
	/*This is not used for recorded gestures and as focus is moving to using and
	* identifying recorded gestures this is removed.
	*/
	/**A list of constant positions that must be true for the gesture to complete*/
	//private Vector<P> constants; 
	
	/**Name Identifier of the Gesture*/
	public String Name;

	/** 
	 * Epsilon used to widen zero, modifying this will make zero have a larger range and thus
	 * be easier to hit but will decrease the sensitivity of noting when joints are not aligned
	 *
	 * It may be beneficial to split this into an x,y,z epsilon calculation complexity would increase
	 * but accuracy of gesture recognition may increase as a result
	 * 
	 * Static across all GestureControllers change with caution.
	 */
	private static Integer Epsilon = 15; 
	
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
	 * @param N : Value to set name to
	 */
	public GestureController(String N){
		init();
		Name = N;
	}
	/**
	 * Initializes both sequence and constants vector and sets step to 0.
	 * Used with constructor and may be useful for reseting gesture sequences.
	 */
	private void init(){
		sequence = new Vector<JointRelation>();
		//constants = new Vector<P>();
		step = 0;
	}
	public void add(JointRelation j){
		sequence.add(j);
	}
	/**
	 * Adds a joint relationship to the sequence array.
	 * @param J1 : SimpleOpenNI constant for a joint
	 * @param J2 : SimpleOpenNI constant for a joint
	 * @param x : Valid values +- [0 , 89]
	 * @param y : Valid values +- [0 , 89]
	 * @param z : Valid values +- [0 , 89]
	 * @param conn : Determines if this is concurrent with the gesture after it 
	 * 	 NOTE: should be false for last gesture
	 */
	public void addPoint(int J1, int J2, 
			Integer x, Integer y, Integer z, boolean conn){
		int loc=-1;
		
		//TODO Check concurrent sequence to assure unique joint pairs
		
		JointRelation tmp = new JointRelation(J1,J2,x,y,z,conn); //Create new tmp point using values given
		
		//Find the last appearance of the given joint pair in the sequence array
		for(int i=sequence.size()-1;i>=0;i--){
//			if (debug) System.out.println(tmp.J.toString()+sequence.get(i).J.toString());
			if (tmp.equalJoints(sequence.get(i))){
					loc = i;
					break;
			}
		}
		tmp.setPrev(loc); //set the previous location to the one found or -1 if not found
		sequence.add(tmp); //add to sequence array
	}
	/**
	 * Add a Joint relationship to constants vector. The x,y,z points may be null and if they are
	 * the null axis will be ignored this is so a constant constraint can use fewer constraint
	 * axis if it is suitable for the gesture.
	 * 
	 * @param J1 : SimpleOpenNI constant for a joint
	 * @param J2 : SimpleOpenNI constant for a joint
	 * @param x : Valid values +- [0 , 89] or null which ignores this axis
	 * @param y : Valid values +- [0 , 89] or null which ignores this axis
	 * @param z : Valid values +- [0 , 89] or null which ignores this axis
	 * @deprecated
	 * 		function has been removed
	 */
	public void addConstant(int J1, int J2,
			Integer x, Integer y, Integer z){
//		//If all axes are being ignored the constraint is null ignore it
//		if(x==null && y == null && z==null) return;
//		
//		/* Check all previous constraints and make sure that this constraint does not
//		 * contradict with any of the previous constraints
//		 */
//		
//		constants.add(new P(J1,J2, x,y,z,false));
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
		if (step == sequence.size()){
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
		while (step > 0 && step < sequence.size() &&sequence.get(step-1).C){
			if (debug) System.out.print("con");
			//Some part of the gesture failed, reset the entire gesture
			if (N == false){
				step = 0; //Reset gesture
				return false; //end the while loop a step failed
			}
			/*
			 * A part hit a holding pattern thus forcing the entire concurrent sequence into
			 * a holding pattern but all parts of the concurrent sequence must be checked for
			 * failure before so can't stop the while loop
			 */
			if (N == null){
				wait = true; //holding pattern true
			}
			N = next(context, user); //continue to the next part of the gesture
		}
		
		// If any part hit a holding patters reset the concurrent gesture to the first gesture
		// in the sequence as remembered by Hold
		if (wait){
			step = Hold; 
		}
		
		/*
		// Check to assure that the constant bounds are being upheld
		for (P c : constants){
			//if the constants are being violated the reset the gesture
			if (!constMatch(c,context,user)){
				step = 0; //reset gesture
				return false;
			}
		}
		*/
		
		//The gesture was not finished return false
		return false;
	}
	public boolean isComplete(JointRecorder context, int tick){
		if (step == context.getTicks()){
			step = 0;
			return true;
		}

		Boolean N = next(context, tick);//call next function
		
		
		boolean wait = false; //Used to initiate holding pattern on concurrent gestures
		int Hold=step; //The holding pattern location for a concurrent gesture
		
		/*
		 * Checks for concurrent gesture completion, holding, or failure
		 * Note the step-1 this is to check if the previous gesture is  listed as  concurrent
		 * with the current gesture so that there is a non-concurrent gesture between sets of
		 * concurrent gestures
		 */
		while (step > 0 && step < sequence.size() &&sequence.get(step-1).C){
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
			N = next(context, tick);//continue to the next part of the gesture
		}
		
		// If any part hit a holding patters reset the concurrent gesture to the first gesture
		// in the sequence as remembered by Hold
		if (wait){
			step = Hold; 
		}
		
		/*
		// Check to assure that the constant bounds are being upheld
		for (P c : constants){
			//if the constants are being violated the reset the gesture
			if (!constMatch(c,context,tick)){
				step = 0; //reset gesture
			}
		}
		*/
		
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
	 * Compares to floating point values taking Epsilon into account for equality.
	 * Returns the relationship proper between x and x2 (x {=, <, >} x2).
	 * This function may be modified to get range relation instead of relationship.
	 * 
	 * @param x : A floating point value to be compared
	 * @param x2 : A floating point value to compare against
	 * @return
	 * 		0 : values were within the range given by Epsilon
	 * 		1 :  x > (x2+Epsilon)
	 * 		-1 : x < (x2-Epsilon)
	 */
	protected static int comp(float x, float x2){
		if ((x2+Epsilon)>= x && (x2-Epsilon) <= x)
			return 0;
		else if ((x2+Epsilon) < x )
			return 1;
		else 
			return -1;
	}
	/**
	 * Calculates the angle between the two given points given by forming a triangle with
	 * the vertices alpha,beta,gamma where gammaX = alphaX and gammaY = betaY.
	 * The angle retrieved is sin(theta)
	 * 
	 * Due to the three dimensional nature of the input data and the two dimensional calculation
	 * used here the X and Y referrals may not be to X, Y in the standard plane but 
	 * to a 2D plane in 3D space with 3D points flattened onto it.
	 * 
	 * @param alphaX : X coordinate of point alpha
	 * @param alphaY : Y coordinate of point alpha
	 * @param betaX : X coordinate of point beta
	 * @param betaY : Y coordinate of point beta
	 * @return
	 * 		Angle between alpha and beta rounded to the nearest integer angle in degrees.
	 */
	private static int angle(float alphaX, float alphaY, float betaX, float betaY){
		float a = alphaY-betaY; //Calculate length of vertical side
		float b = alphaX-betaX; //Calculate length of horizontal side
		double h = Math.sqrt(Math.pow(a,2)+Math.pow(b,2)); //Calculate length of hypotenuse 
		return (int)Math.round(Math.toDegrees(Math.asin(a/h))); //Calculate sin of angle and round
	}
	/**
	 * Calculates the angle between two points along three planes.
	 * 
	 * @param jointOne : First joint to compare
	 * @param jointTwo : Second joint to compare
	 * @return
	 * 		Vector of the angular differences between jointOne and jointTwo with the return values as
	 * 			x: angle created on x-z plane
	 * 			y: angle created on y-z plane
	 * 			z: angle created on x-y plane
	 */
	protected static JointRelation compareJointPositions(JointPair n, PVector jointOne, PVector jointTwo) {
		
//		int x = comp(jointOne.x, jointTwo.x);
//		int y = comp(jointOne.y, jointTwo.y);
//		int z = comp(jointOne.z, jointTwo.z);
		
		//Calculate angle on X-Z plane
		int angleX = angle(jointOne.x, jointOne.z, jointTwo.x, jointTwo.z);
		
		//Calculate angle on Y-Z plane
		int angleY = angle(jointOne.y, jointOne.z, jointTwo.y, jointTwo.z);
		
		//Calculate angle on X-Y plane
		int angleZ = angle(jointOne.x, jointOne.y, jointTwo.x, jointTwo.y);
		if (n == null){
			return new JointRelation(0,0,angleX, angleY, angleZ, false);
		}
		return new JointRelation(n.First,n.Second,angleX, angleY, angleZ,false);
		//return new PVector(angleX, angleY, angleZ);
		//return new PVector(x,y,z);
	}
	/**
	 * Checks if the PVectors a and b are within Epsilon of each other on each axis
	 * @param a : PVector to compare to b
	 * @param b : PVector to compare to a
	 * @return
	 * 		True if a == b, +- Epsilon on each axis
	 */
	public boolean equalAxes(PVector a, PVector b){
		return comp(a.x, b.x) == 0 && comp(a.y,b.y) ==0 && comp(a.z, b.z)==0;
	}
	/**
	 * Determines if a skeletal model taken from context matches a constant relation given
	 * by c.
	 * 
	 * @param c : A constant joint relationship should be from constants vector
	 * @param context : SimpleOpenNI instance
	 * @param user : Id of user to get skeleton from context
	 * @return
	 * 		True if the relation ship matches given relationship from c
	 * @deprecated
	 */
	@SuppressWarnings("unused")
	private boolean constMatch(JointRelation c, SimpleOpenNI context, int user){

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
		JointRelation rel = compareJointPositions(c.J,JointOneReal, JointTwoReal);
		
		//if (debug) System.out.println("C: "+x+" "+y+" "+z);
		
//		//If c.X is not null and the x relationship is incorrect the gesture fails 
//		if (c.X !=null && comp(rel.x, c.X) != 0){
//			return false;
//		}
//		//If c.Y is not null and the y relationship is wrong the gesture fails 
//		if (c.Y != null && comp(rel.y, c.Y) != 0){
//			return false;
//		}
//		//If c.Z is not null and the z relationship is incorrect the gesture fails 
//		if (c.Z != null && comp(rel.z,  c.Z) != 0){
//			return false;
//		}
		//it did not fail thus it passed
		return c.equalsCoordinates(rel);
	}
	/**
	 * @deprecated
	 * @see
	 * 	GestureController#constMatch(JointRelation,SimpleOpenNI,int)
	 */
	@SuppressWarnings("unused")
	private Boolean constMatch(JointRelation c, JointRecorder context, int tick){

		//if not tracking user then that user auto fails
		if (tick < 0 || tick >= context.getTicks()){
			step = 0;
			return false;
		}
		
		//Get Joint Positions in converted format
		PVector JointOneReal = context.getJoint(tick, c.J.First);
		PVector JointTwoReal = context.getJoint(tick, c.J.Second);
		
		if (JointOneReal == null || JointTwoReal == null){
			return false;
		}

		//compare two points
		JointRelation rel = compareJointPositions(c.J,JointOneReal, JointTwoReal);
		
		//if (debug) System.out.println("C: "+x+" "+y+" "+z);
		
//		//If c.X is not null and the x relationship is incorrect the gesture fails 
//		if (c.X !=null && comp(rel.x, c.X) != 0){
//			return false;
//		}
//		//If c.Y is not null and the y relationship is wrong the gesture fails 
//		if (c.Y != null && comp(rel.y, c.Y) != 0){
//			return false;
//		}
//		//If c.Z is not null and the z relationship is incorrect the gesture fails 
//		if (c.Z != null && comp(rel.z,  c.Z) != 0){
//			return false;
//		}
		//it did not fail thus it passed
		return c.equalsCoordinates(rel);
	}
	/**
	 * Check if the current step matches the relationship given by x,y,z perfectly 
	 * 
	 * @param V : PVector containing the relationship of the joints
	 * @return
	 * 		True if the V.x,V.y,and V.z all match the respective relationship given by the current
	 * step in the sequence
	 */
	private boolean stepMatch(JointRelation V){
		JointRelation target = sequence.get(step); //get current step
		//check for x,y,and z matches against target
		return target.equalsCoordinates(V);
	}
	/**
	 * Checks to see if the given x,y,z relationships fall between the relationships given by
	 * the current step and the previous step. This is used to check if a gesture may have paused
	 * but not terminated such as the user is moving slow so they haven't made it to the next step
	 * but did not do anything wrong yet the controller just needs to wait on the user to finish the
	 * gesture.
	 * 
	 * @param V : PVector containing the relationship of the joints
	 * 
	 * @return
	 * 		True if the given relationships fall between the current and previous step.
	 */
	private boolean midMatch(JointRelation V){
		
		JointRelation cur = sequence.get(step); //get current step
		
		//First step of joint type
		if(cur.prev == -1){
			return false; //There is no middle ground on the start
		}
		
		//All Other steps
		JointRelation prev = sequence.get(cur.prev); //get previous of equal joint pair step as denoted by cur.prev

		//check relation between all coordinates return true if all are within range else false
		return V.boundedBy(cur, prev);
//		return (chkCoord(cur.X, V.X, prev.X) && chkCoord(cur.Y, V.Y, prev.Y) 
//				&& chkCoord(cur.Z, V.Z, prev.Z));
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
	@SuppressWarnings("unused")
	private boolean chkCoord(int cur, int val, int prev){
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
	 * Retrieves a specified joint of the specified user from a SimpleOpenNI instance and converts
	 * the coordinates. 
	 * 
	 * @param c : A SimpleOpenNI context
	 * @param user : A user id to retrieve joint data on
	 * @param joint : A SimpleOpenNI constant representing a joint
	 * @return
	 * 		Converted coordinates of joint for the given user
	 */
	protected static PVector getRealCoordinites(SimpleOpenNI c, int user, int joint){
		//Fail-fast check if user has not skeletal model
		if (!c.isTrackingSkeleton(user)) return null;
		
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
		PVector JointOneReal = getRealCoordinites(context,user, sequence.get(step).J.First);
		PVector JointTwoReal = getRealCoordinites(context,user, sequence.get(step).J.Second);

		//compare each joint locations at each axis
		JointRelation rel = compareJointPositions(sequence.get(step).J,JointOneReal, JointTwoReal);
		
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
	private Boolean next(JointRecorder context, int tick){
		//Get Joint Positions in converted format
		PVector JointOneReal = context.getJoint(tick, sequence.get(step).J.First);
		PVector JointTwoReal = context.getJoint(tick, sequence.get(step).J.Second);

		if (JointOneReal == null || JointTwoReal == null){
			return false;
		}
		//compare each joint locations at each axis
		JointRelation rel = compareJointPositions(sequence.get(step).J,JointOneReal, JointTwoReal);
		
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
	/**
	 * Converts a gesture into only discreetly detectable steps, to be used after increasing
	 * Epsilon. 
	 * 
	 * There may be issues with concurrent pieces
	 * 
	 */
	protected void simplifyGesture(CompressionType type){
		if (type == CompressionType.NONE) return;
		
		boolean reduced[] = new boolean[sequence.size()];
		List<List<JointRelation>> compress = new ArrayList<List<JointRelation>>();
		for (int i = sequence.size()-1;i>=0;i--){
			if(!reduced[i]){
				compress.add(reduce(i,null,reduced));
			}
		}
		if (type == CompressionType.SIMPLE){
			Vector<JointRelation> simple = new Vector<JointRelation>();
			for (List<JointRelation> l : compress){
				JointRelation head = l.get(0);
				int p = simple.size()-1;
				for (JointRelation j : simple){
					if (head.equalJoints(j))
						break;
					p --;
				}
				head.setPrev(p);
				simple.add(l.get(0));
			}
			sequence = simple;
			return;
		}
		/* Basic average of all equal points.
		 * 
		 * Upholds concurrency by equating if there is a terminating
		 * concurrent condition then the average will be a terminating
		 * condition.
		 * 
		 * May want to move the average function into P class.
		 */
		Vector<JointRelation> average = new Vector<JointRelation>();
		JointRelation sum;
		for (List<JointRelation> l : compress){
			if (l == null)
				System.out.println("null");
			sum = new JointRelation();
			sum.J = new JointPair(l.get(0).J.First, l.get(0).J.Second);
			sum.C = false;
			for (JointRelation p : l){
				sum.X += p.X;
				sum.Y += p.Y;
				sum.Z += p.Z;
				sum.C = sum.C&p.C; //any false will propagate from here
			}
			sum.X /=l.size();
			sum.Y /=l.size();
			sum.Z /=l.size();

			int prev = -1;
			for (int i=average.size()-1;i>=0;i--){
				if (average.get(i).equalJoints(sum)){
					prev = i;
					break;
				}
			}
			sum.setPrev(prev);
			average.add(sum);
		}
		if (type == CompressionType.DBL_AVG){
			/*TODO run average against original using average points as the alpha points
			 *for reduce()
			 */
			int i=size()-1;
			reduced = new boolean [size()];
			for (JointRelation j : average){
				while (!reduced[i]){
					reduce(i,j,reduced);
					i--;
					if (i<0)
						break;
				}
				if (i<0)
					break;
			}
		}
		else{
			sequence = average;
		}
	}

	private List<JointRelation> reduce(int i,JointRelation alpha, boolean visited[]){
		if (debug) System.out.print(i+" ");
		if (visited[i])
			return null;
		visited[i] = true;
		
		JointRelation current = sequence.get(i);
		
		//hit the last element in the sequence
		if (current.prev==null || current.prev == -1){
			List<JointRelation> l = new ArrayList<JointRelation>();
			l.add(current);
			return l;
		}
		
		//first element in sequence
		if (alpha == null){
			List<JointRelation> l = reduce(current.prev,current,visited);
			if (l==null)
				l = new ArrayList<JointRelation>();
			l.add(current);
			return l;
		}
		
		if (current.equalsCoordinates(alpha)){
			List<JointRelation> l = reduce(current.prev,alpha,visited);
			if (l==null)
				l = new ArrayList<JointRelation>();
			l.add(current);
			return l;
			
		}
		else{
			if (debug) System.out.print("unvisit");
			//Unvisit so the driver can start a new node here
			visited[i] = false;
			return null;
		}
	}
	/**
	 * Modifies Epsilon by e, a positive e will make position detection less 
	 * sensitive and a negative e will make detection more sensitive.
	 * 
	 * Epsilon is bounded below by 0 and above by 90 (0<=Epsilon<=90)
	 * 
	 * @param delta : change of Epsilon
	 */
	public void changeTolerance(int delta){
		Epsilon += delta;
		
		//Minimum Epsilon = 0
		if (Epsilon < 0)
			Epsilon = 0;
		
		//Maximum Epsilon = 90
		if (Epsilon > 90)
			Epsilon = 90;
	} 
	/**
	 * Resets Epsilon to default value of 15
	 */
	public void setDefaultTolerance(){
		Epsilon = 15;
	}
	public static Integer getTolerance(){
		return Epsilon;
	}
	public int size(){
		return sequence.size();
	}
	public void clear(){
		sequence.clear();
	}
	public String toString(){
		String info = new String();
		info += "Steps: "+size()+'\n';
		info += "current step: "+step+'\n';
		int i=0;
		info +=i+": ";
		for(JointRelation jR : sequence){
			info += jR.toString()+" ";
			if (!jR.C){
				i++;
				if (i<size())
					info += '\n'+""+i+": ";
			}
		}
		return info;
	}
	public void save(String fileName, List<GestureController> g){
		BufferedWriter wr;
		try {
			wr = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			System.out.println("IOException: "+e.getMessage());
			e.printStackTrace();
			return;
		}
		String content = new String();
		
		content +="<?xml version=\"1.0\"?>"+'\n';
		content +="<root>"+'\n';
		content += xmlStatics.createElement("epsilon",GestureController.getTolerance().toString());
		xmlGestureParser.xmlStatics.write(wr,content);
		
		for(GestureController c : g){
			xmlStatics.write(wr,c.toXML());
		}
		
		xmlStatics.write(wr,"</root>");

	}
	public void save(String fileName){
		BufferedWriter wr;
		try {
			wr = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			System.out.println("IOException: "+e.getMessage());
			e.printStackTrace();
			return;
		}
		
		String content = new String();
		
		content +="<?xml version=\"1.0\"?>"+'\n';
		content +="<root>"+'\n';
		content +="<epsilon>"+GestureController.getTolerance()+"</epsilon>";
		content += toXML();
		content +="</root>"; 
		xmlStatics.write(wr,content);
	}
	/**
	 * Returns xml Representation of a gesture.
	 * @return
	 * 	String in xml format representative of this
	 */
	public String toXML(){

		
		String content = new String();
		content +="<"+classTag+">"+'\n';
		content += xmlStatics.createElement("name", Name);
		
		content +="<sequence>"+'\n';
		for (JointRelation e : sequence){
			content += e.toXML();
		}
		content +="</sequence>"+'\n';
		/*
		content +="<constants>"+'\n';
		for (P e : constants){
			content += e.toXML();
		}
		content +="</constants>"+'\n';
		*/
		content +="</"+classTag+">"+'\n';
		
		return content;
	}
	public boolean isEmpty() {
		return sequence.isEmpty();
	}
//	@Override
//	public Iterator<JointRelation> iterator() {
//		return sequence.iterator();
//	}
	protected GestureController clone(){
		GestureController o = new GestureController();
		o.sequence.addAll(this.sequence);
		o.step = 0;
		return o;
	}
}