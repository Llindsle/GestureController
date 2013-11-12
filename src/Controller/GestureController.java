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
	private boolean debug = true;
	
	final private String classTag = "gesture";
	
	/**The Sequence if joint relationships describing the gesture */
	private Vector<JointRelation> sequence; 
	
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
	private static Double Epsilon = 0.01; 
	
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
		step = 0;
	}
	public void add(JointRelation j){
		//Find the last appearance of the given joint pair in the sequence array
		int loc = -1;
		for(int i=sequence.size()-1;i>=0;i--){
			if (j.equalJoints(sequence.get(i))){
					loc = i;
					break;
			}
		}
		j.setPrev(loc); //set the previous location to the one found or -1 if not found
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
	public void addPoint(JointPair jP, Euclidean first, Euclidean second, boolean conn){
		int loc=-1;
		
		//TODO Check concurrent sequence to assure unique joint pairs
		
		//Create new tmp point using values given
		JointRelation tmp = new JointRelation(jP, first, second,conn); 
		
		//Find the last appearance of the given joint pair in the sequence array
		for(int i=sequence.size()-1;i>=0;i--){
			if (tmp.equalJoints(sequence.get(i))){
					loc = i;
					break;
			}
		}
		tmp.setPrev(loc); //set the previous location to the one found or -1 if not found
		sequence.add(tmp); //add to sequence array
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
		
		//The gesture was not finished return false
		return false;
	}
	public boolean isComplete(JointRecorder context, int tick){
		if (step == size()){
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
	 * @see JointRelation
	 */
	protected static JointRelation compareJointPositions(JointPair n, PVector jointOne, PVector jointTwo) {
		//Convert PVector to Euclidean
		Euclidean first = new Euclidean(jointOne.x, jointOne.y, jointOne.z);
		Euclidean second = new Euclidean(jointTwo.x, jointTwo.y, jointTwo.z);
		return new JointRelation(n, first, second, false);
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
		PVector JointOneReal = getRealCoordinites(context,user, sequence.get(step).J.First);
		PVector JointTwoReal = getRealCoordinites(context,user, sequence.get(step).J.Second);

		//compare each joint locations at each axis
		JointRelation rel = compareJointPositions(sequence.get(step).J,JointOneReal, JointTwoReal);


		//IF stepMach() Position is exactly what is expected
		if (stepMatch(rel)){
			step ++; //Increment Gesture
			return true;
		}

		//IF midMatch() Position is not quite right but not wrong yet either
		if (midMatch(rel)){
			//  step = step; //maintain position
			return null;
		}

		//Position has nothing to do with what was expected
		//Gesture Failed 
		
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
			step ++; //Increment Gesture
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
		
		/* This plays through backwards because if there is multiple sets of focus
		 * joints in the sequence then it must use previous to determine the relationship
		 * between the values of sequence
		 */
		for (int i = sequence.size()-1;i>=0;i--){
			if(!reduced[i]){
//				compress.add(reduce(i,null,reduced));
				if (debug) System.out.print("X ");
				compress.add(reduce(i,reduced));
			}
		}
		
		if (debug) System.out.println("nodes: "+compress.size());
		if (type == CompressionType.SIMPLE){
			Vector<JointRelation> simple = new Vector<JointRelation>();
//			for (List<JointRelation> l : compress){
			//walk the reversed list in reverse for correct direction
			for (int i=compress.size()-1;i>=0;i--){
				List<JointRelation> l = compress.get(i);
				JointRelation head = l.get(0);
				int p = simple.size()-1;
				for (JointRelation j : simple){
					if (head.equalJoints(j))
						break;
					p --;
				}
				head.setPrev(p);
				simple.add(head);
			}
			sequence = simple;
			return;
		}
		Vector<JointRelation> average = averageReduction(compress);
		
		if (type == CompressionType.DBL_AVG){
			int i=0;
			reduced = new boolean [size()];
			compress.clear();
			for (int j=sequence.size()-1;j>=0;j--){
				JointRelation r = average.get(i);
				if (!reduced[j]){
					List<JointRelation> l = reduce(j,r,reduced);
					if (l != null)
						compress.add(l);
					i++;
					if (i==average.size())
						break;
				}
				if (i== average.size())
					break;
			}
			if (debug) System.out.println("nodes: "+compress.size());
			average = averageReduction(compress);
		}
		
		sequence = average;

	}
	private List<JointRelation> reduce(int i, boolean visit[]){
		if (i < 0 || visit[i])
			return null;
		
		visit[i] = true;
		List<JointRelation> l = new ArrayList<JointRelation>();
		JointRelation alpha = sequence.get(i);
		JointRelation beta;
		Integer prev = alpha.prev;
		
		l.add(alpha);
		if (debug) System.out.print(i+" ");
		while (prev != null && prev > -1){
			beta = sequence.get(prev);
			if (alpha.equalsCoordinates(beta)){
				if (l.size() > 2){
					JointRelation outer = l.get(l.size()-2);
					JointRelation inner = l.get(l.size()-1);
					if (inner.boundedBy(outer, beta))
						l.add(beta);
					else
						return l;
				}
				else{
					l.add(beta);
				}
			}
			else
				return l;
			if (debug) System.out.print(prev+" ");
			visit[prev] = true;
			prev = beta.prev;
		}
		return l;
	}
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
	private Vector<JointRelation> averageReduction(List<List<JointRelation>> compress){

		/* Basic average of all equal points.
		 * 
		 * Upholds concurrency by equating if there is a terminating
		 * concurrent condition then the average will be a terminating
		 * condition.
		 * 
		 * concurrent check is not implemented
		 * 
		 */
		Vector<JointRelation> average = new Vector<JointRelation>();
		JointRelation sum;
//		for (List<JointRelation> l : compress){
		
		//reverse the reverse for forward direction
		for (int j=compress.size()-1;j>=0;j--){
			List<JointRelation> l = compress.get(j);
			
			sum = new JointRelation();
			sum.J = new JointPair(l.get(0).J.First, l.get(0).J.Second);
			sum.C = false;
			sum.angle = new ArrayList<Euclidean>();
			for (int i=0;i<l.get(0).angle.size();i++){
				Vector<Euclidean> angle = new Vector<Euclidean>();
				for (JointRelation p : l){
					angle.add(p.angle.get(i));
	//				sum.C = sum.C&p.C; //any false will propagate from here
				} 
				
				sum.angle.add(Euclidean.average(angle));
			}
			
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
		
		return average;
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
		//TODO fix bounds of epsilon
		Epsilon += delta;
		
		//Minimum Epsilon = 0
		if (Epsilon < 0)
			Epsilon = .0;
		
		//Maximum Epsilon = 90
		if (Epsilon > 90)
			Epsilon = 90.0;
	} 
	/**
	 * Resets Epsilon to default value of 0.05
	 */
	public void setDefaultTolerance(){
		Epsilon = 0.05;
	}
	public static Double getTolerance(){
		return Epsilon;
	}
	public Vector<JointRelation> getSequence(){
		@SuppressWarnings("unchecked") //this is OK, casting back to JointRelation
		Vector<JointRelation> r = (Vector<JointRelation>) sequence.clone();
		return r;
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
		content +="</"+classTag+">"+'\n';
		
		return content;
	}
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