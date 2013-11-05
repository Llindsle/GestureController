package Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import processing.core.PVector;

import SimpleOpenNI.SimpleOpenNI;

/**
 * JointRecorder records the position of joints and can be used to access 
 * the location of a joint at a specific point in time.
 * 
 * Recording does not support multiple users, more that one user may interfere
 * with recording.
 * 
 * @author Levi Lindsley
 *
 */
public class JointRecorder {
	/**Used only to print debug output*/
	@SuppressWarnings("unused")
	private boolean debug = true;

	/**SimpleOpenNI skeletal points to record*/
	private Set<Integer> joints;
	
	/**The record of all joints at each distinct record call*/
	private List<Map<Integer, PVector>> recorder;
	
	/**
	 * Default Constructor
	 */
	public JointRecorder(){
		joints = new TreeSet<Integer>();
		recorder = new ArrayList<Map<Integer, PVector>>();
	}
	/**
	 * Adds a joint if it is not already in the set. Joints may not be added
	 * if there is recorded data, thus data may be added if isEmpty() is true. 
	 * 
	 * @param j : Integer to add to joints
	 * @return
	 * 		True if j added successfully.
	 * 		False if j already existed or record data exists
	 */
	public boolean addJoint(int j){
//		if (debug) System.out.println(j);
		
		//make sure that no record exists
		if (!isEmpty())
			return false;
		
		return joints.add(j);
	}
	/**
	 * Add what may be all the integers that SimpleOpenNI uses for skeletal
	 * joint positions. It seems to record all the points though some do 
	 * always record as null. 
	 * 
	 * If record exists returns False and adds no points.
	 * 
	 * @return
	 * 		True if at least one point was added to joints.
	 * 		False if a current record exist or no points were added.
	 */
	public boolean addAll(){
		//check anti-record constraint
		if (!isEmpty())
			return false;
		
		//if any int gets added then set returns true
		boolean set = false;
		for (int i=0;i<25;i++){
			set = joints.add(i) || set;
		}
		return set;
	}
	/**
	 * Takes a snapshot of each focus joint and adds to record
	 * @param context : SimpleOpenNI instance to retrieve skeletal data from
	 * @param user : user id to retrieve skeleton from
	 */
	public void record(SimpleOpenNI context, int user){
//		if (debug) System.out.println("recording ...");
		
		//Make sure user is being tracked
		if (!context.isTrackingSkeleton(user))
			return;
		
		//new map to add each joint data to
		Map<Integer, PVector> v = new HashMap<Integer, PVector>();
		PVector p = new PVector(); //vector for joint positions
		
		//for each joint j get coordinates from context
		for (Integer j : joints){
			p = GestureController.getRealCoordinites(context, user, j);
			v.put(j, p);
		}
		
		//add the map representing this time tick
		recorder.add(v);
	}
	/**
	 * Retrieves the position of a joint at a given time
	 * @param time : the number of ticks from the beginning of the recording
	 * @param joint : the index of the joint to retrieve
	 * @return
	 * 		PVector representing joint at the given time, this is the same as
	 * calling GestureController.getRealcoordinites() at the time that the 
	 * recording represents.
	 */
	public PVector getJoint(int time, int joint){
		//Make sure that time is within bounds
		if (!joints.contains(joint) || !(time < recorder.size())){
			return null;
		}
		return recorder.get(time).get(joint);
	}
	/**
	 * @return
	 * 		The size of the recorder, thus representing the number of ticks
	 */
	public int getTicks(){
		return recorder.size();
	}
	/**
	 * Checks if joints contains j
	 * @param j : value to check for
	 * @return
	 * 		True if joints contains j.
	 * 		False in all other cases.
	 */
	public boolean contains(int j){
		return joints.contains(j);
	}
	/**
	 * Check if j is a subset of joints
	 * @param j : Set to check for
	 * @return
	 * 		True if j is a subset of joints.
	 * 		False otherwise.
	 */
	public boolean containsAll(Set<Integer> j){
		return joints.containsAll(j);
	}
	/**
	 * Checks if there is any current recorded data, this will return true
	 * if there is no recorded data but there is focus joints.
	 * @return
	 * 		True if recorder is empty.
	 * 		False else.
	 */
	public boolean isEmpty(){
		return recorder.isEmpty();
	}
	/**
	 * Clears the recorder, deleting the current recording, isEmpty() will
	 * return true after this is called until record() is called.
	 */
	public void clear(){
		recorder.clear();
	}
	/**
	 * Returns a string of the recording and info about it, yea its toString()
	 * what where you expecting?
	 */
	@Override
	public String toString(){
		String ret = new String();
		
		ret += "Number of joints recorded: "+joints.size()+'\n';
		ret += "Number of recording ticks: "+recorder.size()+'\n';
		for (Map<Integer, PVector> m : recorder){
			ret += m.toString()+'\n';
		}
		return ret;
	}
}
