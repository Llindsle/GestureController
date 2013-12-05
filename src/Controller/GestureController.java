package controller;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
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
	/**
	 * Different types of compression available for use on a gesture
	 * The compression ranges from least(NONE), to most(DBL_AVG).
	 * 
	 * DBL_AVG is currently not behaving as expected.
	 * 
	 * @author Levi Lindsley
	 *
	 */
	public enum CompressionType{
		/**Does not compress the gesture, effectively disabling simplifyGesture*/
		NONE,
		/**
		 * Groups the raw data into nodes that are within the same Epsilon range
		 * by panning through the list and taking the first non-used value to start
		 * a new node with. Then takes the head of each node as the node value
		 */
		SIMPLE,
		/**
		 * Groups data into Epsilon nodes and then averages across the node to 
		 * get an approximate value for that node
		 */
		AVG,
		/**
		 * Groups data into Epsilon nodes then takes average, the average values are 
		 * then used to seed the node sorting process a second time and the average
		 * of the secondary Epsilon nodes created in this way is taken as the value
		 */
		DBL_AVG};
		
	/** Used only to toggle debug output */
	@SuppressWarnings("unused")
	private boolean debug = true;
	
	final private String classTag = "gesture";
	
	/** Used to enable/disable logging completion of a gesture*/
	private static BufferedWriter logWriter;
	
	/**The Sequence of joint relationships describing the gesture */
	private Vector<Vector<JointRelation>> sequence; 
	
	//TODO Move this to a local variable in the few spots it is requred
	/**The links between the diffrent levels of sequence, only used for generating
	 * sequence and should be moved to a local variable later if possible */
	private Map<Pair, Pair> link;
	
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
	private static Double Epsilon = Euclidean.getEpsilon(); 
	
	/**Number of completed steps of the gesture */
	private Integer step; 
	private Integer phase;

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
		sequence = new Vector<Vector<JointRelation>>();
		link = new HashMap<Pair, Pair>();
		logWriter = null;
		step = 0;
		phase = 0;
	}
	/**
	 * Adds a joint relation into this, also sets the previous point if there
	 * exists a previous relation of the same joint pair
	 * @param j : JointRelation to add
	 */
	public void add(JointRelation j){
//		//Find the last appearance of the given joint pair in the sequence array
//		int loc = -1;
//		for(int i=sequence.size()-1;i>=0;i--){
//			if (j.equalJoints(sequence.get(i))){
//					loc = i;
//					break;
//			}
//		}
		j.setPrev(link.get(j.J));

		if (sequence.isEmpty() || (link.get(j.J)!= null && link.get(j.J).First==sequence.size()-1))
			sequence.add(new Vector<JointRelation>());	
		
		sequence.lastElement().add(j);
		link.put(j.J, new Pair(sequence.size()-1, sequence.lastElement().size()-1));
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
	public void addPoint(Pair jP, Euclidean first, Euclidean second){
		
		//Create new tmp point using values given
		JointRelation tmp = new JointRelation(jP, first, second);
		if (debug) System.out.println(tmp);
		add(tmp);
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
			logGesture();
			return true; //return the successful completion
		}
		
//		phase = 0;
		Boolean N = true;//call next function
		boolean wait = false; //Used to initiate holding pattern on concurrent gestures
//		int Hold=step; //The holding pattern location for a concurrent gesture
		
		/*
		 * Checks for concurrent gesture completion, holding, or failure
		 * Note the step-1 this is to check if the previous gesture is  listed as  concurrent
		 * with the current gesture so that there is a non-concurrent gesture between sets of
		 * concurrent gestures
		 */
//		while (step > 0 && step < sequence.size() &&sequence.get(step-1).C){
		
		phase = 0;
		while (phase < sequence.get(step).size()){
			N = next(context, user); //continue to the next part of the gesture
//			if (debug) System.out.print("con");
			/*
			 * A part hit a holding pattern thus forcing the entire concurrent sequence into
			 * a holding pattern but all parts of the concurrent sequence must be checked for
			 * failure before so can't stop the while loop
			 */
			if (N == null){
				wait = true; //holding pattern true
			}
			
			//Some part of the gesture failed, reset the entire gesture
			else if (N == false){
				step = 0; //Reset gesture
				return false; //end the while loop a step failed
			}
			phase++;	
		}
		
		// If any part hit a holding patters reset the concurrent gesture to the first gesture
		// in the sequence as remembered by Hold
		if (!wait){
			step ++;
//			step = Hold; 
		}
		
		//The gesture was not finished return false
		return false;
	}
	/**
	 * The isComplete function that places the location of the bounds into a PVector
	 * useful for displaying guided gestures.
	 * 
	 * @param context : SimpleOpenNI instance to get current location from
	 * @param user : user to extract joint information for must have skeleton tracked 
	 * @param points : PVector to place point location information into
	 * @return
	 * 		True if the gesture is complete
	 * 
	 * @see GestureController#isComplete(SimpleOpenNI, int)
	 */
	public boolean isComplete(SimpleOpenNI context, int user, Vector<PVector> points){
		
		//run the regular isComplete function
		if (isComplete(context, user)){
			logGesture();
			return true;
		}
		
		//clear display vector
		points.clear();
		
		Euclidean pos;
		
		//Add position data using the last angle type available
		while (phase < sequence.get(step).size()){
			if (step == 0)
				points.add(new PVector());
			else{
				pos = sequence.get(step-1).firstElement().angle.get(sequence.get(step-1).get(phase).angle.size()-1);
				points.add(new PVector(pos.x.floatValue(),pos.y.floatValue(),pos.z.floatValue()));
			}
			if (step == size()){
				points.add(new PVector());
				points.add(new PVector());
			}
			
			else{
				PVector JointOneReal = getRealCoordinites(context,user, sequence.get(step).get(phase).J.First);
				PVector JointTwoReal = getRealCoordinites(context,user, sequence.get(step).get(phase).J.Second);
				JointRelation rel = compareJointPositions(sequence.get(step).firstElement().J,JointOneReal, JointTwoReal);
				
				pos = rel.angle.get(rel.angle.size()-1);
				points.add(new PVector(pos.x.floatValue(),pos.y.floatValue(),pos.z.floatValue()));
	
				pos = sequence.get(step).get(phase).angle.get(sequence.get(step).get(phase).angle.size()-1);
				points.add(new PVector(pos.x.floatValue(),pos.y.floatValue(),pos.z.floatValue()));
			}
			phase ++;
		}
		return false;
	}
	/**
	 * Uses a JointRecorder to parse gesture data from this should return the 
	 * same value that would be returned if the action was done and the data
	 * was taken directly.
	 * @param context : JointRecorder to get data from
	 * @param tick : Tick of context to view
	 * @return
	 * 		True if gesture is complete<p>
	 * 		False otherwise
	 * @see GestureController#isComplete(SimpleOpenNI, int)
	 */
	public boolean isComplete(JointRecorder context, int tick){
		if (step == size()){
			step = 0;
			logGesture();
			return true;
		}
//		phase = 0;
		Boolean N =true;//call next function
		boolean wait = false; //Used to initiate holding pattern on concurrent gestures
		
		/*
		 * Checks for concurrent gesture completion, holding, or failure
		 * Note the step-1 this is to check if the previous gesture is  listed as  concurrent
		 * with the current gesture so that there is a non-concurrent gesture between sets of
		 * concurrent gestures
		 */
//		while (step > 0 && step < sequence.size() &&sequence.get(step-1).C){
		phase = 0;
		while (phase < sequence.get(step).size()){
			N = next(context, tick);//continue to the next part of the gesture
			/*
			 * A part hit a holding pattern thus forcing the entire concurrent sequence into
			 * a holding pattern but all parts of the concurrent sequence must be checked for
			 * failure before so can't stop the while loop
			 */
			if (N == null){
				wait = true; //holding pattern true
			}
			//Some part of the gesture failed, reset the entire gesture
			else if (N == false){
				step = 0; //Reset gesture
				break; //end the while loop a step failed
			}
			phase++;
			
		}
		
		// If any part hit a holding patters reset the concurrent gesture to the first gesture
		// in the sequence as remembered by Hold
		if (!wait){
			step ++;
//			step = Hold; 
		}
		
		return false;
	}
	private boolean isComplete(Vector<JointRelation> context){
		if (step == size()){
			logGesture();
			return true;
		}
		//TODO finish this function, it is just another isComplete function
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
	protected static int comp(double x, double x2){
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
	 * @see Euclidean#planarAngle()
	 */
	@SuppressWarnings("unused")
	private static int angle(float alphaX, float alphaY, float betaX, float betaY){
		float a = alphaY-betaY; //Calculate length of vertical side
		float b = alphaX-betaX; //Calculate length of horizontal side
		double h = Math.sqrt(Math.pow(a,2)+Math.pow(b,2)); //Calculate length of hypotenuse 
		return (int)Math.round(Math.toDegrees(Math.asin(a/h))); //Calculate sin of angle and round
	}
	/**
	 * Creates a new Joint relation comparing the jointPair n using the vectors jointOne
	 * and jointTwo
	 * 
	 * @param n : Joint pair being compared
	 * @param jointOne : PVector location representation of n.First
	 * @param jointTwo : PVector location representation of n.Second
	 * @return
	 * 		JointRelation containing information about the relationship between the joints
	 * @see JointRelation#equalJoints(JointRelation)
	 */
	protected static JointRelation compareJointPositions(Pair n, PVector jointOne, PVector jointTwo) {
		//Convert PVector to Euclidean
		Euclidean first = new Euclidean(jointOne.x, jointOne.y, jointOne.z);
		Euclidean second = new Euclidean(jointTwo.x, jointTwo.y, jointTwo.z);
		return new JointRelation(n, first, second);
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
	 * Check if the current step matches the relationship given by x,y,z perfectly 
	 * 
	 * @param V : PVector containing the relationship of the joints
	 * @return
	 * 		True if the V.x,V.y,and V.z all match the respective relationship given by the current
	 * step in the sequence
	 */
	private boolean stepMatch(JointRelation V){
		JointRelation target = sequence.get(step).get(phase); //get current step
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
		
		JointRelation cur = sequence.get(step).get(phase); //get current step
		
		//First step of joint type
		if(cur.prev == null){
			return false; //There is no middle ground on the start
		}
			
		/*All Other steps*/
		
		//get previous of equal joint pair step as denoted by cur.prev
		JointRelation prev = sequence.get(cur.prev.First).get(cur.prev.Second); 

		//check relation between all coordinates return true if all are within range else false
		boolean range =  V.boundedBy(cur, prev);
		
	
		//check if current position is between the previous point and the one before that, this is possible
		//in a continuous gesture due to epsilon jumping ahead
		if (!range && prev.prev != null){
			cur = prev;
			prev = sequence.get(cur.prev.First).get(cur.prev.Second);
			range = V.boundedBy(cur, prev);
		}
		return range;
	}
	/**
	 * Checks if val is between cur and prev, the bounds do not need to be in any order thus
	 * if prev > cur, cur > prev, or cur = prev this will still return the proper value
	 * @param cur : bounding value
	 * @param val : value to check
	 * @param prev : bounding value
	 * @return
	 * 		True if val is between cur and prev 
	 * @see JointRelation#chkBounds(double, double, double)
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
		//Fail-fast check if user has no skeletal model
		if (!c.isTrackingSkeleton(user)) return null;
		
		//PVectors to store joint position data and converted position data
		PVector Joint = new PVector();
		PVector Real = new PVector();
		
		//get joint data from context as determined by the c
		c.getJointPositionSkeleton(user, joint, Joint);
		
		//convert data into projective data this seems more useful for comparison
		//the raw data may work just as well though not sure so I use this
		c.convertRealWorldToProjective(Joint, Real);
		return Real;
//		return Joint;
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
			step = 0; //reset gesture 
			return false;
		}
		
		//Get Joint Positions in converted format
		PVector JointOneReal = getRealCoordinites(context,user, sequence.get(step).get(phase).J.First);
		PVector JointTwoReal = getRealCoordinites(context,user, sequence.get(step).get(phase).J.Second);

		//compare each joint locations at each axis
		JointRelation rel = compareJointPositions(sequence.get(step).get(phase).J,JointOneReal, JointTwoReal);


		//IF stepMach() Position is exactly what is expected
		if (stepMatch(rel)){
//			step ++; //Increment Gesture
//			if (debug) System.out.print("! ");
			return true;
		}

		//IF midMatch() Position is not quite right but not wrong yet either
		if (midMatch(rel)){
			//  step = step; //maintain position
//			if (debug) System.out.print(".");
			return null;
		}

		//Position has nothing to do with what was expected
		//Gesture Failed 
//		if (debug && step > 0) System.out.println("X");
		step = 0; //reset gesture
		return false;
	} 
	/**
	 * @see #next(SimpleOpenNI, int)
	 */
	private Boolean next(JointRecorder context, int tick){
		//Get Joint Positions in converted format
		PVector JointOneReal = context.getJoint(tick, sequence.get(step).get(phase).J.First);
		PVector JointTwoReal = context.getJoint(tick, sequence.get(step).get(phase).J.Second);

		if (JointOneReal == null || JointTwoReal == null){
			return false;
		}
		//compare each joint locations at each axis
		JointRelation rel = compareJointPositions(sequence.get(step).get(phase).J,JointOneReal, JointTwoReal);
		
		//IF stepMach() Position is exactly what is expected
		if (stepMatch(rel)){
//			step ++; //Increment Gesture
//			if (debug) System.out.println(Name+" continue");
			return true;
		}

		//IF midMatch() Position is not quite right but not wrong yet either
		if (midMatch(rel)){
			//  step = step; //maintain position
//			if (debug) System.out.println(Name+" Holding");
			return null;
		}

		//Position has nothing to do with what was expected
		//Gesture Failed 
		
//		if (debug && step > 0){
//			System.out.println(Name+" failed");
//			System.out.println("Previous: "+sequence.get(sequence.get(step).prev));
//			System.out.println(" Current: "+rel);
//			System.out.println("    Next: "+sequence.get(step));
//			
//		}
		step = 0; //reset gesture
		return false;
	}
	/**
	 * Converts a gesture into only discreetly detectable steps, the method of
	 * converting is determined by type
	 * 
	 * There may be issues with concurrent pieces
	 * 
	 * @param type : The type of compression to be used
	 * @see CompressionType
	 * 
	 */
	protected void simplifyGesture(CompressionType type){
		//If no compression then return
		if (type == CompressionType.NONE) return;
		
//		Euclidean.changeEpsilon(Euclidean.getEpsilon()*(-0.5));
		
		//array to track what elements have been reduced already
		boolean reduced[] = new boolean[sequence.size()];
		
		//list of lists that shows what points are within epsilon of each other
		List<List<Vector<JointRelation>>> compress = new ArrayList<List<Vector<JointRelation>>>();
		
		//list of vectors showing the index of the point in the corresponding compress list
		List<Vector<Integer>> compIndex = new ArrayList<Vector<Integer>>();
		
		//vector used to add to compIndex
		Vector<Integer> localIndex = new Vector<Integer>();
		
		/* This plays through backwards because if there is multiple sets of focus
		 * joints in the sequence then it must use previous to determine the relationship
		 * between the values of sequence and in a move of utter brilliance I though that
		 * if I could navigate a list in one fashion that it should be a singly linked
		 * list in reverse.
		 */
		for (int i = sequence.size()-1;i>=0;i--){
			//if the point has not been reduced start compress with it
			if(!reduced[i]){
//				compress.add(reduce(i,null,reduced));
				if (debug) System.out.print("X ");
				
				//create new index array for reduce to populate
				localIndex = new Vector<Integer>();
				
				//add the list that is returned from reduce to compress
				compress.add(reduce(i,reduced, localIndex));
				
				//add the index that goes along with the compress list
				compIndex.add(localIndex);
			}
		}
		
		if (debug) System.out.println("nodes: "+compress.size());
		//if the type is simple compression then just take the head of each list
		//and add it to a new vector of JointRelaitons and replace sequence with
		//the new vector
		if (type == CompressionType.SIMPLE){
			//create a new vector
//			Vector<Vector<JointRelation>> simple = new Vector<Vector<JointRelation>>();
			sequence = new Vector<Vector<JointRelation>>();
			link = new HashMap<Pair, Pair>();
			
			//walk the reversed list in reverse for correct direction
			for (int i=compress.size()-1;i>=0;i--){
				//get the head of the list
				Vector<JointRelation> head = compress.get(i).get(0);
				
				for (JointRelation h : head){
					//directly add JointRelation into sequence cause functions are
					//there to be used
					add(h);
//					if (simple.isEmpty() || (link.get(h.J)!= null && link.get(h.J).First==simple.size()-1))
//						simple.add(new Vector<JointRelation>());	
//					
//					simple.lastElement().add(h);
//					link.put(h.J, new Pair(simple.size()-1, simple.lastElement().size()-1));
					
				}
//				//find any previous matches for the JointPair
//				int p = simple.size()-1;
//				for (JointRelation j : simple){
//					if (head.equalJoints(j))
//						break;
//					p --;
//				}
//				head.setPrev(p);
				
				//add the point to the vector
//				simple.add(head);
			}
			//set sequence equal to the simple compression list
//			sequence = simple;
			//the simple compression has been completed, return
			return;
		}
		
		//the compression type is at least average so run averageReduction
		//on the compress list
		Vector<Vector<JointRelation>> average = averageReduction(compress);
		
		//If the compression is DBL_AVG then there is still more to do
		//namely use the average values as the alpha values for computing
		//the list that comprise compress.
		if (type == CompressionType.DBL_AVG){
//			if(debug) System.out.println(compIndex);
			int i=average.size()-1; //average is now correctly ordered
			reduced = new boolean [size()]; //reset reduced
			compress.clear(); //clear the compress array 
			
//			int j = sequence.size()-1;
			
			//for all the vectors in the list of compIndex use the value of
			//average that corresponded to that compress list to rebuild compress
			for (i=0;i<compIndex.size();i++){
//				if (debug) System.out.println(compIndex.get(i));
				if (debug) System.out.print("X ");
				
				//clearing reduce here allows points to be placed into more than
				//one node.
				reduced = new boolean [size()];
				//get the current index
				Vector<Integer> nodeIndex = compIndex.get(i);
				//create a new list to add points to 
				List<Vector<JointRelation>> l = new ArrayList<Vector<JointRelation>>();
				//grab the value from average to use as alpha
				Vector<JointRelation> alpha = average.get(i);
				
//				boolean br = false;
				
				//add prev nodes, runs the reduce function in reverse on the node
				//to see what this is doing go look at reduce()
				if (i > 0){
					Vector<Integer> index = compIndex.get(i-1);
					
					List<Vector<JointRelation>> s = reduce(alpha,index, true);
					if (!s.isEmpty())
						l.addAll(s);
				}
				
				//add current node
				//add all points in the current node to the average sequence as
				//averaging the points across the node guarantees that all the 
				//points within the node are within epsilon as both ends are
				//bound by +- epsilon and the average moves the start into the 
				//center so that both ends are still within epsilon of the average
				//point that is being used to compare.
				for (Integer k : nodeIndex){
					if (debug) System.out.print(k+" ");
					l.add(sequence.get(k));
				}
				
				//add next nodes
				//iterate through the nodes in the order that the reverse index
				//dictates, when one fails then break
//				br = false;
				if (i < compIndex.size()-1){
					Vector<Integer> index = compIndex.get(i+1);
					List<Vector<JointRelation>> s = reduce(alpha, index, false);
					if (!s.isEmpty())
						l.addAll(s);
				}
				
				//add the list to compress
				compress.add(l);
			}
			if (debug) System.out.println("nodes: "+compress.size());
			
			//average the compression list
			average = averageReduction(compress);
		}
		
//		Euclidean.changeEpsilon(Euclidean.getEpsilon()*2);
		
		//set sequence to the appropriate average value
		sequence = average;

	}
	private List<Vector<JointRelation>> reduce(Vector<JointRelation> alpha, Vector<Integer> index, boolean rev){
		List<Vector<JointRelation>> l = new ArrayList<Vector<JointRelation>>();
		Vector<JointRelation> beta;
		Vector<Pair> gamma = new Vector<Pair>();
		for (JointRelation jR : alpha){
			if (jR.prev == null){
				gamma = null;
				return l;
			}
			if (gamma.size()>0 &&  !jR.prev.First.equals(gamma.lastElement().First)){
				gamma = null;
				break;
			}
			gamma.add(jR.prev);
		}
		
		int inc = rev ? -1 : 1;
		int start = rev ? index.size()-1 : 0;
		int end = rev ? -1 : index.size();
		boolean br = false;
		for (int j=start;j != end;j+=inc){
			if (debug) System.out.print(index.get(j)+" ");
			beta = new Vector<JointRelation>();
			if (gamma == null) return l;
			for (int k=0;k<gamma.size();k++){
				beta.add(sequence.get(index.get(j)).get(gamma.get(k).Second));
				//compare alpha to beta 
				if (alpha.get(k).equalsCoordinates(beta.get(k))){
					//smoothing goes here
				}
				else{
					br = true;
					break;
				}
			}
			//all of beta passed so add it to the list
			if (!br){
				l.add(beta);
			}
			//beta did not pass so return
			else
				return l;
			
//			if (debug) System.out.print(gamma.firstElement().First+" ");
			gamma = new Vector<Pair>();
			for (JointRelation jR : beta){
				if (jR.prev == null){
					gamma = null;
					break;
				}
				if (gamma.size()>0 && jR.prev.First != gamma.lastElement().First){
						gamma = null;
						break;
				}
				gamma.add(jR.prev);
			}
		}
		return l;
	}
	/**
	 * Iteratively walk through a vector following the prev link and create a list
	 * of maximum size of continuous points that are within epsilon of i
	 * @param i : Index of sequence to begin on
	 * @param visit : Shows which indexes have been visited
	 * @param index : An index of the positions in the returned list
	 * @return
	 * 		A list of the points that are within epsilon of sequence.get(i)<p>
	 * 		Null if i is outOfBounds or visit[i] is true
	 */
	private List<Vector<JointRelation>> reduce(int i, boolean visit[], Vector<Integer> index){
		//If i is out of bounds or the point has been visited return null
		if (i < 0 || i >= size() || visit[i]){
			index.clear();
			return null;
		}
		
		//set visit to true so this point is marked as seen
		visit[i] = true;
		List<Vector<JointRelation>> l = new ArrayList<Vector<JointRelation>>();
//		Vector<JointRelation> v;
		
		//clear the index and then add i to it
		index.clear();
		index.add(i);
		
		//set sequence[i] as alpha to be used for comparison of other points
		Vector<JointRelation> alpha = sequence.get(i);
		Vector<JointRelation> beta;
		Vector<Pair> gamma = new Vector<Pair>();
		for(JointRelation jR : alpha){
			if (jR.prev == null){
				gamma = null;
				break;
			}
			if (gamma.size()>0 && !jR.prev.First.equals(gamma.lastElement().First)){
				gamma = null;
				break;
			}
			gamma.add(jR.prev);
		}
		//get the point that is before alpha
//		Pair prev = alpha.firstElement().prev;
		//add alpha vector to the return list
		l.add(sequence.get(i));
		if (debug) System.out.print(i+" ");
		//while prev is a valid value
		while (gamma != null){
			//get beta
			beta = new Vector<JointRelation>();
			boolean br = false; //signals a break
			for (int j=0;j<gamma.size();j++){
				
				beta.add(sequence.get(gamma.get(j).First).get(gamma.get(j).Second));
				//compare alpha to beta 
				if (alpha.get(j).equalsCoordinates(beta.get(j))){
//					if (debug) System.out.print("?");
					
					//TODO look at re-enabling smoothing
					
					//This just smoothing, while things are being reworked this 
					//is temporarily disabled
					
/*
					//if the size of the list is greater than 1 (2+) then check
					//that beta fits at the end of the list by checking if the 
					//beginning of the list and beta bound a middle value that has
					//already been put into the list, the value that is randomly 
					//selected is always the end of the list.
					if (l.size() > 1){
						JointRelation outer = l.get(0);
						JointRelation inner = l.get(l.size()-1);
						//if the bound holds then add beta to the list
						if (inner.boundedBy(outer, beta)){
							l.add(beta);
						}
						//beta did not fit within the bounds of the list so it 
						//does not belong thus the list is finished
						else
							return l;
					}
*/
					
					//there was not enough elements to create a bound so add beta
					//to the list as it was in range
//					else{
//						index.add(prev);
//					}
				}
				else{
					br = true;
					break;
				}
			}
			//all of beta passed so add it to the list
			if (!br){
				l.add(beta);
			}
			//beta did not pass so return
			else
				return l;
			
			if (debug) System.out.print(gamma.firstElement().First+" ");
			//mark beta as visited and continue following the prev link through
			//sequence until it terminates or a point fails to that fails to fit
			//into the list is found
			visit[gamma.firstElement().First] = true;
			index.add(gamma.firstElement().First);
//			prev = gamma.firstElement().prev;
			gamma = new Vector<Pair>();
			for (JointRelation jR : beta){
				if (jR.prev == null){
					gamma = null;
					break;
				}
				if (gamma.size()>0 &&  !jR.prev.First.equals(gamma.lastElement().First)){
						gamma = null;
						break;
				}
				gamma.add(jR.prev);
			}
		}
		return l;
	}
	/**
	 * A recursive version of reduce(), the iterative version is less convoluted.
	 * 	@see #reduce(int, boolean[], Vector)
	 */
	/*
	private List<JointRelation> reduce(int i,JointRelation alpha, boolean visited[]){
		if (debug) System.out.print(i+" ");
		if (i < 0 || visited[i])
			return null;
		visited[i] = true;
		
		JointRelation current = sequence.get(i);
		List<JointRelation> l;
		
		//first element in sequence
		if (alpha == null){
			l = reduce(current.prev,current,visited);
		}
		
		else if (current.equalsCoordinates(alpha)){
			//hit the last element in the sequence
			if (current.prev==null || current.prev == -1){
				l = new ArrayList<JointRelation>();
				l.add(current);
				return l;
			}
			l = reduce(current.prev,alpha,visited);			
		}
		else{
			if (debug) System.out.print("unvisit");
			//Unvisit so the driver can start a new node here
			visited[i] = false;
			return null;
		}
		if (l==null)
			l = new ArrayList<JointRelation>();
		
		if (l.size() > 2){
			JointRelation outside = l.get(l.size()-2);
			JointRelation inside = l.get(l.size()-1);
			
			//check if inside is bounded by this and outside
			//if it is not then this point should go in another list
			if (inside.boundedBy(current, outside))
				l.add(current);
			else{
				if (debug) System.out.print("unvisit("+i+")");
				//Unvisit so the driver can start a new node here
				visited[i] = false;
			}
		}
		
		else
			l.add(current);
		return l;
	}
	*/
	/**
	 * Averages all the points within the sublists of compress and returns a vector
	 * of the result
	 * @param compress : List of lists to average
	 * @return
	 * 		Vector<JointRelation> where each element is the average of a sub-list
	 * of compress.
	 */
	private Vector<Vector<JointRelation>> averageReduction(List<List<Vector<JointRelation>>> compress){

		/* Basic average of all equal points.
		 * 
		 * Upholds concurrency by equating if there is a terminating
		 * concurrent condition then the average will be a terminating
		 * condition.
		 */
		Vector<Vector<JointRelation>> average = new Vector<Vector<JointRelation>>();
		link = new HashMap<Pair, Pair>();
		
		for (int i=compress.size()-1;i>=0;i--){
			
			JointRelation sum;
			List<Vector<JointRelation>> current = compress.get(i);
			
			for (int j=0;j<current.get(0).size();j++){
				sum = new JointRelation();
				sum.J = current.get(0).get(j).J;
				sum.angle = new ArrayList<Euclidean>();
				sum.angleType = current.get(0).get(j).angleType;
				
				Vector<Vector<Euclidean>> innerAngle = new Vector<Vector<Euclidean>>();
				for (int k=0;k<current.size();k++){
					JointRelation jR = current.get(k).get(j);
					for (int e=0;e<jR.angle.size();e++){
						if (innerAngle.size()==e)
							innerAngle.add(new Vector<Euclidean>());
						innerAngle.get(e).add(jR.angle.get(e));
					}
				}
				for (Vector<Euclidean> v : innerAngle){
					sum.angle.add(Euclidean.average(v));
				}
				sum.setPrev(link.get(sum.J));

				if (average.isEmpty() || (link.get(sum.J)!= null && link.get(sum.J).First==average.size()-1))
					average.add(new Vector<JointRelation>());	
				
				average.lastElement().add(sum);
				link.put(sum.J, new Pair(average.size()-1, average.lastElement().size()-1));
			}
		}
		return average;
//		JointRelation sum = null;
////		for (List<JointRelation> l : compress){
//		
//		//reverse the reverse for forward direction
//		for (int k=compress.size()-1;k>=0;k--){
//			//get the current sub-list
//			List<Vector<JointRelation>> l = compress.get(k);
//			average.add(new Vector<JointRelation>());
//			for (int i=0;i<l.get(0).size();i++){
//				
//				//Create a new joint relation and set it up
//				sum = new JointRelation();
//				sum.J = new Pair(l.get(0).get(0).J.First, l.get(0).get(0).J.Second);
//				//			sum.C = false;
//				sum.angle = new ArrayList<Euclidean>();
//				 Vector<Vector<Euclidean>> angle = new Vector<Vector<Euclidean>>();
//				
//				for (int j=0;j<l.size();j++){
//					JointRelation p = l.get(j).get(i);
//					angle.add(new Vector<Euclidean>());
//					for (int q=0;q<p.angle.size();q++)
//						angle.get(j).add(p.angle.get(q));
//						//				sum.C = sum.C&p.C; //any false will propagate from here
//					
//				}
//				for (Vector<Euclidean> v : angle){
//					sum.angle.add(Euclidean.average(v));
//				}
//
////				//average each type of angle individually 
////				for (int i=0;i<l.get(0).angle.size();i++){
////
////				}
//				//find and set the previous point
////				int prev = -1;
////				for (int i=average.size()-1;i>=0;i--){
////					if (average.get(i).equalJoints(sum)){
////						prev = i;
////						break;
////					}
////				}
////				sum.setPrev(prev);
//				
//				sum.setPrev(link.get(sum.J));
//				link.put(sum.J, new Pair(k,i));		
//			}
//			average.lastElement().add(sum);
//		}
//		
//		return average;
	}
	/**
	 * Modifies Epsilon by e, a positive e will make position detection less 
	 * sensitive and a negative e will make detection more sensitive.
	 * 
	 * 
	 * @param delta : change of Epsilon
	 */
	public void changeTolerance(double delta){
		Euclidean.changeEpsilon(delta);
		Epsilon = Euclidean.getEpsilon();
	} 
	/**
	 * Resets Epsilon to default value of 0.05
	 */
	public void setDefaultTolerance(){
		Epsilon = 0.05;
	}
	/** 
	 * @return value of Epsilon
	 */
	public static Double getTolerance(){
		return Epsilon.doubleValue();
	}
	/**
	 * Enables logging of gestures upon completion, gestures will be logged
	 * until {@link #closeLog()} is called.
	 *  
	 * @param logFile : String indicating what file should be used for log
	 * @throws IOException
	 */
	public static void enableLog(String logFile){
		try{
		logWriter = new BufferedWriter(new FileWriter(logFile));
		String content = new String();
		content +="<?xml version=\"1.0\"?>"+'\n';
		content +="<root>"+'\n';
		logWriter.write(content);
		}catch(IOException e){
			System.err.println(e.getLocalizedMessage());
			System.err.println("Log file "+logFile+" failed to open");
		}
	}
	/**
	 * Logs a gesture upon completion if log is enabled the log format is
	 * <p> {@literal <gestureCompletion>}
	 * <p> {@link #Name}
	 * <p> {@literal <time> current time </time>}
	 * <p> {@literal </gestureCompletion>}
	 */
	private void logGesture(){
		if (logWriter == null)
			return;
		
		String content = new String();
		Long time = System.currentTimeMillis();
		content += "<gestureCompletion>"+'\n';
		content += Name+'\n';
		content += xmlStatics.createElement("time", time.toString());
		content += "</gestureCompletion>"+'\n';
		try {
			logWriter.write(content);
			logWriter.flush();
		} catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
			System.err.println("Gesture failed to log."+'\n'+"Gesture: "+Name+
					'\n'+"Time: "+System.currentTimeMillis()+'\n'+"logWriter reset");
			logWriter = null;
		}
	}
	/**
	 * If log is enabled this closes the log and disables logging untill it is
	 * re-enabled through {@link #enableLog(String)}
	 * @throws IOException
	 */
	public static void closeLog() throws IOException{
		if (logWriter == null)
			return;
		
		logWriter.write("</root>");
		logWriter.close();
		logWriter = null;
	}
	public Vector<Vector<JointRelation>> getSequence(){
		@SuppressWarnings("unchecked") //this is OK, casting back to Vector<JointRelation>
		Vector<Vector<JointRelation>> r = (Vector<Vector<JointRelation>>) sequence.clone();
		return r;
	}
	/**
	 * There is little reason to what I do in this function. Data is being collected
	 * and will be processed, what gets processed and how it gets processed is yet
	 * to be worked out.
	 * 
	 * @param o
	 * @return
	 */
	public Double comp(GestureController o){
		//TODO make this function return something it does stuff
		//but returns null at present.
		
		//determines if the key set of joints of this is greater or equal
		//to the key set of o
		boolean keyFit = this.link.keySet().containsAll(o.link.keySet());
		
		//They didn't work at all, return infinity
		if (!keyFit)
			return Double.POSITIVE_INFINITY;
		
		//Difference in size of the sequences
		int pointDiff = Math.abs(this.size()-o.size());
		
		//see how many times o.sequence matches up with this.sequence if any
		//and where those matches occur
		Vector<Integer> strikes = new Vector<Integer>();
		for (int i=0;i<o.sequence.size();i++){
			if (this.isComplete(o.sequence.get(i))){
				strikes.add(o.step);
			}
		}
		
		//shows where o could be meshed with this, if possible,
		//-1 values indicate that it is not possible for that element
		Vector<Integer> insertAfter = mesh(o);
		
		return  null;
	}
	private Vector<Integer> mesh(GestureController o){
		//shows where o could be meshed with this, if possible,
		//-1 values indicate that it is not possible for that element
		Vector<Integer> insertAfter = new Vector<Integer>();

		//used to quickly try and guess how the joints of o match up to the 
		//joint order of this based on how they matched on the previous run
		//the first pass initializes -1
		Vector<Pair> temporal = new Vector<Pair>();

		Pair bound = new Pair(0,0);

		int min=0;
		for (int i=0;i<o.size();i++){
			if (this.size()==0) break;
			Vector<JointRelation> ins = o.sequence.get(i);
			for (int j=min;j<this.size()-1;i++){
				Vector<JointRelation> lb = this.sequence.get(j);
				Vector<JointRelation> ub = this.sequence.get(j+1);

				//if the vectors are different sized then they represent different
				//gestures and don't mesh so don't even try
				if (ins.size() != lb.size() || ins.size() != ub.size())
					continue;

				//assume true then set to false if wrong
				boolean boundedBy = true;
				for(int k=0;k<ins.size();k++){
					JointRelation jR = ins.get(k);

					//initilize temporal
					if (temporal.size() == k)
						temporal.add(new Pair(-1,-1));

					//search for the matching pairs of joints, using temporal for
					//a quick check guess
					bound.First = findPair(jR, lb, temporal.get(k).First);
					bound.Second = findPair(jR, ub, temporal.get(k).Second);

					temporal.set(k, bound); //set the temporal guess to the outcome

					//if the bound fails break
					if (!jR.boundedBy(lb.get(bound.First), ub.get(bound.Second))){
						boundedBy = false;
						break;
					}	
				}
				if (boundedBy){
					min = j;
					insertAfter.add(j);
				}
				else{
					insertAfter.add(-1);
				}
			}
		}
		return insertAfter;
	}
	public boolean merge(GestureController o){
		Vector<Integer> insertAfter = mesh(o);
		
		//If at least one point has a valid value then the gestures can be merged
		//else they are two different gestures
		boolean valid = false;
		for (int i=0;i<o.size();i++){
			if (insertAfter.get(i) != -1)
				valid = true;
		}
		if (!valid) return false;
		
		boolean begin = true;
		for (int i=0;i<o.size();i++){
			int position = insertAfter.get(i)+1;
			if (position == 0 ){
				if (begin){
					sequence.insertElementAt(o.sequence.get(i), position);
				}
				else{
					sequence.add(o.sequence.get(i));
				}
			}
			else{
				begin = false;
				sequence.insertElementAt(o.sequence.get(i), position);
			}
		}
		return true;
	}
	public boolean append(GestureController o){
		return sequence.addAll(o.sequence);
	}
	private Integer findPair(JointRelation s, Vector<JointRelation> target, int guess){
		if (guess >= 0 && guess < target.size())
			if (target.get(guess).equalJoints(s))
				return guess;
		for (int i=0;i<target.size();i++){
			if (target.get(i).equalJoints(s))
				return i;
		}
		return null;
	}
	public void mirror(){
		//reset gesture
		step = 0;
		
		//iterator for sequence
		Iterator<Vector<JointRelation>> seq = sequence.iterator();
		//map from current to mirror values
		Map<Integer, Integer> m = new HashMap<Integer, Integer>();
		//set of values previously processed
		Set<Integer> focus = new HashSet<Integer>();

		while (seq.hasNext()){
			//iterator through the relations in each step of sequence
			Iterator<JointRelation> v = seq.next().iterator();
			
			while (v.hasNext()){
				JointRelation j = v.next();
				
				//if the joint has not yet been processed, then process and
				//add to map
				if (focus.add(j.J.First)){
					m.put(j.J.First, Skeleton.mirror(j.J.First));
				}
				if (focus.add(j.J.Second)){
					m.put(j.J.Second, Skeleton.mirror(j.J.Second));
				}
				//change the target joint to the mirror equivalent
				j.J.First = m.get(j.J.First);
				j.J.Second = m.get(j.J.Second);
			}
		}
	}
	/**
	 * @return
	 * 		Number of steps that this gesture contains
	 */
	public int size(){
		return sequence.size();
	}
	/**
	 * Clears ALL steps from this gesture
	 */
	public void clear(){
		sequence.clear();
	}
	public String toString(){
		String info = new String();
		info += "Steps: "+size()+'\n';
		info += "current step: "+step+'\n';
		int i=0;
		info +=i+": ";
		for (Vector<JointRelation> v : sequence){
			for(JointRelation jR : v){
				info += jR.toString()+" ";
//				if (!jR.C){
//					i++;
//					if (i<size())
//						info += '\n'+""+i+": ";
//				}
			}
			info += '\n';
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
		content += xmlStatics.createElement("epsilon",Epsilon.toString());
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
		content += xmlStatics.createElement("epsilon", Epsilon.toString());
		content += toXML();
		content +="</root>"; 
		xmlStatics.write(wr,content);
	}
	public GestureController load(Scanner xmlInput){
		GestureController gC = new GestureController();
		String next = xmlInput.next();
		while (next.compareTo("<"+gC.classTag+">") != 0){
			if (!xmlInput.hasNext()){
				if (debug) System.out.println("no classTag");
				return null;
			}
			next = xmlInput.next();
		}
		
		gC.Name = xmlStatics.parseElement(xmlInput);
		xmlInput.next();
		gC.sequence.clear();

		next = xmlInput.next();
		gC.sequence.add(new Vector<JointRelation>());
		while (xmlInput.hasNext() && next.compareTo("</sequence>")!=0){
			if (next.compareTo("</step>")==0){
				next = xmlInput.next();
				gC.sequence.add(new Vector<JointRelation>());
				continue;
			}
			gC.sequence.lastElement().add(JointRelation.load(xmlInput));
			next = xmlInput.next();
		}

		xmlInput.next();//</classTag>
		
		if (debug) System.out.println(gC.toString());
		return gC;
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
		for (Vector<JointRelation> v : sequence){
			content += "<step>"+'\n';
			for (JointRelation e : v){
				content += e.toXML();
			}
			content += "</step>"+'\n';
		}
		content +="</sequence>"+'\n';
		content +="</"+classTag+">"+'\n';
		
		return content;
	}
	/**
	 * @return
	 * 	True if and only if there is no steps to this gesture, false otherwise.
	 */
	public boolean isEmpty() {
		return sequence.isEmpty();
	}
	protected GestureController clone(){
		GestureController o = new GestureController();
		o.sequence.addAll(this.sequence);
		o.step = this.step;
		return o;
	}
}