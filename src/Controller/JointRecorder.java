package controller;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import processing.core.PVector;

import SimpleOpenNI.SimpleOpenNI;

/**
 * JointRecorder records the position of joints and can be used to access 
 * the location of a joint at a specific point in time. Joints can be added
 * using SimpleOpenNIConstants or the enum {@link Skeleton}.
 * 
 * Recording does not support multiple users, more that one user may interfere
 * with recording.
 * 
 * @see GestureRecord#java
 * @see Skeleton#java
 * 
 * @author Levi Lindsley
 *
 */
public class JointRecorder implements xmlGestureParser<JointRecorder>{
	/**Generated serialVersionUID*/
	private static final long serialVersionUID = -4796232190131039155L;

	/**Used only to print debug output*/
	@SuppressWarnings("unused")
	private boolean debug = true;
	
	/**XML classTag*/
	final String classTag = "recorder";

	/**Integers corresponding to SimpleOpenNI skeletal points to record*/
	private Set<Integer> joints;
	
	/**The record of all joints at each distinct record call*/
	private List<Map<Integer, PVector>> recorder;
	
	/**Denotes current tick for play back*/
	private int playBackTick;
	
	/**Determines if left-right mirror is active or not*/
	private boolean mirror;
	
	/**
	 * Default Constructor. joints and recorder are initialized and playBackTick
	 * is set to 0, mirror is false by default.
	 */
	public JointRecorder(){
		joints = new TreeSet<Integer>();
		recorder = new ArrayList<Map<Integer, PVector>>();
		playBackTick = 0;
		mirror = false;
	}
	/**
	 * Adds a joint if it is not already in the set. Joints may not be added
	 * if there is recorded data, thus data may be added if isEmpty() is true. 
	 * 
	 * @param j : Integer to add to joints
	 * @return
	 * 		True if j added successfully.
	 * 	<p>	False if j already existed or record data exists
	 */
	public boolean addJoint(Integer j){
		//make sure that no record exists
		if (!isEmpty())
			return false;
		
		return joints.add(j);
	}
	/**
	 * Adds all the Integers represented by collection to joints.
	 * @param c - Collection to add
	 * @return
	 * 		True if at least one element was added successfully 
	 *  <p> False no element could be added.
	 */
	public boolean addAll(Collection<Integer> c){
		return joints.addAll(c);
	}
	/**
	 * Adds all joint locations tracked by SimpleOpenNI into the recorder.
	 * This is done by calling addLeft(), addright() and addCenter().
	 * 
	 * If record exists returns False and adds no points.
	 * 
	 * @return
	 * 		True if at least one point was added to joints.
	 * 	<p>	False if a current record exist or no points were added.
	 * 
	 * @see JointRecorder#addLeft()
	 * @see JointRecorder#addRight()
	 * @see JointRecorder#addCenter()
	 */
	public boolean addAll(){
		//check anti-record constraint
		if (!isEmpty())
			return false;
		
		//if any int gets added then set returns true
		boolean set = false;
		set = addLeft() || set;
		set = addRight() || set;
		set = addCenter() || set;
		return set;
	}
	/**
	 * Adds Left: Shoulder, elbow, hand, hip, knee, and foot to the
	 * current tracked set.
	 * @return
	 * 		True if at least one point was added to joints.
	 * 	<p>	False if a current record exist or no points were added.
	 * 
	 * @see JointRecorder#addRight()
	 */
	public boolean addLeft(){
		return joints.addAll(getLeft());
	}
	/**
	 * Adds Right: Shoulder, elbow, hand, hip, knee, and foot to the
	 * current tracked set.
	 * @return
	 * 		True if at least one point was added to joints.
	 * 	<p>	False if a current record exist or no points were added.
	 * 
	 * @see JointRecorder#addLeft()
	 */
	public boolean addRight(){
		return joints.addAll(getRight());
	}
	/**
	 * Adds Head, Neck and Torso into the tracked set
	 * 
	 * @return
	 * 		True if at least one point was added to joints.
	 * 	<p>	False if a current record exist or no points were added.
	 */
	public boolean addCenter(){
		return joints.addAll(Skeleton.getAllCenter());
	}
	/**
	 * Gets a set of points that represent the left half of the skeleton.
	 * If mirror is active then this set of points is mirrored and the right
	 * points are retrieved.
	 * @return
	 * 		Set of Left points from Skeleton.
	 */
	private Set<Integer> getLeft(){
		if (mirror){
			return Skeleton.getAllRight();
		}
		else{
			return Skeleton.getAllLeft();
		}
	}
	/**
	 * Gets a set of points that represent the right half of the skeleton.
	 * If mirror is active then this set of points is mirrored and the left
	 * points are retrieved.
	 * @return
	 * 		Set of Right points from Skeleton.
	 */
	private Set<Integer> getRight(){
		if (mirror){
			return Skeleton.getAllLeft();
		}
		else{
			return Skeleton.getAllRight();
		}
	}
	/**
	 * Uses {@link Skeleton} to mirror the set of focus joints and
	 * changes joints to the mirror set. 
	 */
	private void mirror(){
		joints = Skeleton.mirror(joints);
	}
	/**
	 * Flips the mirror mode and mirrors all current focus joints. This could
	 * invalidate recordings so it may only be done without an active record
	 * if a record is active then this function will return false and mirror
	 * will NOT change.
	 * 
	 * @return
	 * 		True if the recording is empty.
	 * 	<p>	False if there is a current record
	 */
	public boolean flipMirror(){
		if (!isEmpty())
			return false;
		
		mirror();
		
		return true;
	}
	/**
	 * @return
	 * 		the value of mirror
	 * @see #mirror
	 * @see #mirror()
	 */
	public boolean getMirror(){
		return mirror;
	}
	/**
	 * Takes a snapshot of each focus joint and adds to record
	 * @param context : SimpleOpenNI instance to retrieve skeletal data from
	 * @param user : user id to retrieve skeleton from
	 */
	public void record(SimpleOpenNI context, int user){
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
	 * @param tick : the number of ticks from the beginning of the recording
	 * @param joint : the index of the joint to retrieve
	 * @return
	 * 		PVector representing joint at the given time, this is the same as
	 * calling GestureController.getRealcoordinites() at the time that the 
	 * recording represents.
	 */
	public PVector getJoint(int tick, int joint){
		//Make sure that time is within bounds
		if (!joints.contains(joint) || !(tick < recorder.size())){
			return null;
		}
		return recorder.get(tick).get(joint);
	}
	/**
	 * Sets playBackTick to 0 restarting play back
	 */
	public void resetPlayBack(){
		playBackTick = 0;
	}
	/**
	 * Calls {@link JointRecorder#playBack(Set)} with the joints that this
	 * records
	 * 
	 * @return
	 * 		List of PVector[2] each representing the coordinates of a joint
	 * @see JointRecorder#playBack(Collection)
	 */
	public List<PVector[]> playBack(){
		return playBack(joints);
	}
	/**
	 * Steps through the recording one tick per call to this function the 
	 * playBackTick is auto incremented. Joint pairs are determined by generic
	 * human anatomy and joint pairs are only included in the result if both
	 * joints are available in the recording and in focus.
	 * 
	 * @param focus : list of joints to include in return vector
	 * @return
	 * 		List of PVector[2] each representing the coordinates of a joint
	 * @see JointRecorder#playBack(int, Collection)
	 */
	public List<PVector []> playBack(Collection<Integer> focus){
		playBackTick ++;
		if (playBackTick > recorder.size()){
			return null;
		}
		return playBack(playBackTick-1, focus);
	}
	/**
	 * Joint pairs are determined by generic human anatomy and joint pairs 
	 * are only included in the result if both joints are available in the 
	 * recording and in focus.
	 * 
	 * @param tick : recording tick to use for return value
	 * @param focus : collection of joints
	 * @return
	 * 		List of PVector[2] each representing the coordinates of a joint
	 * @see JointRecorder#playBack(Set)
	 */
	public List<PVector []> playBack(int tick, Collection<Integer> focus){
		if (tick < 0 || tick >= recorder.size()){
			return null;
		}
		List<PVector []> coordinites = new ArrayList<PVector[]>();
		PVector v[] = null;
		int con1, con2;

		con1 = SimpleOpenNI.SKEL_HEAD;
		con2 = SimpleOpenNI.SKEL_NECK;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_NECK);
		
		con1 = con2;
		con2 = SimpleOpenNI.SKEL_LEFT_SHOULDER;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_LEFT_SHOULDER);
		
		con1 = con2;
		con2 = SimpleOpenNI.SKEL_LEFT_ELBOW;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_LEFT_ELBOW);
		
		con1 = con2;
		con2 = SimpleOpenNI.SKEL_LEFT_HAND;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);

		con1 = SimpleOpenNI.SKEL_NECK;
		con2 = SimpleOpenNI.SKEL_RIGHT_SHOULDER;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_RIGHT_SHOULDER);
		
		con1 = con2;
		con2 = SimpleOpenNI.SKEL_RIGHT_ELBOW;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_ELBOW);
		
		con1 = con2;
		con2 = SimpleOpenNI.SKEL_RIGHT_HAND;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);

		con1 = SimpleOpenNI.SKEL_LEFT_SHOULDER;
		con2 = SimpleOpenNI.SKEL_TORSO;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
		
		con1 = SimpleOpenNI.SKEL_RIGHT_SHOULDER;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_TORSO);

		con1 = con2;
		con2 = SimpleOpenNI.SKEL_LEFT_HIP;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_LEFT_HIP);
		
		con1 = con2;
		con2 = SimpleOpenNI.SKEL_LEFT_KNEE;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_LEFT_KNEE);
		
		con1 = con2;
		con2 = SimpleOpenNI.SKEL_LEFT_FOOT;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_LEFT_FOOT);

		con1 = SimpleOpenNI.SKEL_TORSO;
		con2 = SimpleOpenNI.SKEL_RIGHT_HIP;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_RIGHT_HIP);
		
		con1 = con2;
		con2 = SimpleOpenNI.SKEL_RIGHT_KNEE;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_HIP, SimpleOpenNI.SKEL_RIGHT_KNEE);
		
		con1 = con2;
		con2 = SimpleOpenNI.SKEL_RIGHT_FOOT;
		v = getJointPosition(tick, con1, con2, focus);
		if (v!= null) coordinites.add(v);
//		  context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, SimpleOpenNI.SKEL_RIGHT_FOOT);  
		return coordinites;
	}
	/**
	 * Returns list of coordinates dictated by the JointPairs in focus
	 * @param tick
	 * @param focus
	 * @return
	 */
	public List<PVector []> playBack(int tick, Vector<Pair> focus){
		Set<Integer> f = new TreeSet<Integer>();
		for (Pair p : focus){
			f.add(p.First);
			f.add(p.Second);
		}
		return playBack(tick,f);
	}
	/**
	 * Retrieves the position of two joints as specified by con1 and con2
	 * the joints are only retrieved if both contained in this and in focus.
	 * @param tick : time tick to pull coordinates from
	 * @param con1 : joint one
	 * @param con2 : joint two
	 * @param focus : collection of focus joints
	 * @return
	 * 		Position of two joints as a PVector[2]
	 * 	<p>	null if the joints are not available/focused
	 */
	private PVector[] getJointPosition(int tick, int con1, int con2, Collection<Integer>focus){
		if ((contains(con1)&&contains(con2))&&focus.contains(con1)&&focus.contains(con2)){
			PVector[] ret = new PVector[2];
			ret[0] = recorder.get(tick).get(con1);
			ret[1] = recorder.get(tick).get(con2);
			return ret;
		}
		return null;
	}
	/**
	 * @return
	 * 		The size of the recorder, thus representing the number of ticks
	 */
	public int getTicks(){
		return recorder.size();
	}
	/**@return The current playBackTick tick*/
	public int getPlayBackTick(){
		return playBackTick;
	}
	/**
	 * Checks if joints contains j
	 * @param j : value to check for
	 * @return
	 * 		True if joints contains j.
	 * 	<p>	False in all other cases.
	 */
	public boolean contains(int j){
		return joints.contains(j);
	}
	/**
	 * Check if j is a subset of joints
	 * @param j : Set to check for
	 * @return
	 * 		True if j is a subset of joints.
	 * 	<p>	False otherwise.
	 */
	public boolean containsAll(Set<Integer> j){
		return joints.containsAll(j);
	}
	/**
	 * Checks if there is any current recorded data, this will return true
	 * if there is no recorded data but there is focus joints.
	 * @return
	 * 		True if recorder is empty.
	 * 	<p>	False else.
	 */
	public boolean isEmpty(){
		return recorder.isEmpty();
	}
	public void getJoints(Collection<Integer> j){
		j.addAll(joints);
	}
	/**
	 * Clears the recorder, deleting the current recording, isEmpty() will
	 * return true after this is called until record() is called. Focus joints
	 * will remain. To remove focus joints use {@link #clearFocus()}
	 */
	public void clear(){
		recorder.clear();
	}
	/**
	 * Clears focus joints, as these are linked to the recording the recording
	 * is also cleared.
	 * 
	 * @see #clear()
	 */
	public void clearFocus(){
		clear();
		joints.clear();
	}
	/**
	 * Returns a string of the recording and info about it, yea its toString()
	 * what where you expecting?
	 */
	@Override
	public String toString(){
		String ret = new String();
		
		ret += "Number of joints tracked: "+joints.size()+'\n';
		ret += "Number of recording ticks: "+recorder.size()+'\n';
		for (Map<Integer, PVector> m : recorder){
			ret += m.toString()+'\n';
		}
		return ret;
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
		content +=toXML();
		content +="</root>"+'\n';
		xmlStatics.write(wr, content);
		
	}
	@Override
	public void save(String fileName, List<JointRecorder> jR) {
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
		xmlStatics.write(wr, content);
		for (JointRecorder r : jR){
			xmlStatics.write(wr, r.toXML());
		}
		xmlStatics.write(wr,"</root>"+'\n');
	}
	public String toXML(){
		String context = new String();
		PVector v;
		context +="<"+classTag+">"+'\n';
		for (int i=0;i<recorder.size();i++){
			context +="<tick>"+'\n';
			context += i+" "+'\n';
			String inner;
			for (Integer j : joints){ 
				inner = j.toString()+'\n';
				v = recorder.get(i).get(j);
				context += xmlStatics.createPVectorElem("joint", inner, v);
			}
			context +="</tick>"+'\n';
		}
		context += "</"+classTag+">"+'\n';
		return context;
	}
	@Override
	public JointRecorder load(Scanner xmlInput) throws UnsupportedOperationException {
		UnsupportedOperationException e = new UnsupportedOperationException();
		throw e;
	}

}
