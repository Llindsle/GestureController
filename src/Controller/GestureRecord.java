package controller;

import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import processing.core.PVector;

import SimpleOpenNI.SimpleOpenNI;

/**
 * Class used to capture and process specific joint relations to generate a gesture from.
 *
 * Recording does not support multiple users, more that one user may interfere
 * with recording.
 * 
 * @author Levi Lindsley
 *
 */
public class GestureRecord extends GestureController{
	/**Used only for debug purposes */
	@SuppressWarnings("unused")
	private boolean debug = true;
	
	/**Pairs of Joints to record relationship between*/
	private Vector <Pair> Focus;
	
	/**
	 * Default Constructor, initializes control vectors
	 */
	public GestureRecord(){
		super();
		Focus = new Vector<Pair>();
	}
	/**
	 * If there is no recorded data, add a new joint pair to Focus. 
	 * @param first : First Joint in pair
	 * @param second : Second Joint in pair
	 * @return
	 * 		True if the joint pair was added successfully.
	 * 		False if there is recorded data and the pair may not be currently added
	 * or if the joint pair is already a focus pair.
	 */
	public boolean addFocusJoints(int first, int second){
		//Focus pairs may not be changed mid record
		if (!isEmpty()){
			return false;
		}
		//create a new JointPair with specified joints
		Pair o = new Pair(first, second);
		
		//Focus pairs may not be duplicated
		if (Focus.contains(o))
			return false;
		
		//Add new focus and increase the number of vectors in recorder
		Focus.add(o);
		//recorder.add(new Vector<PVector>());
		return true;
	}
	/**
	 * Calculates relationships of all focus pairs retrieved from context and
	 * stores them in recorder
	 * 
	 * @param context : SimpleOpenNI instance 
	 * @param user : user id to retrieve skeletal info from
	 */
	public void record(SimpleOpenNI context, int user){
		if (!context.isTrackingSkeleton(user))
			return;
		int i=0;
		for (Pair jP : Focus){
			
			//get coordinates for both joints
			PVector jointOne = super.getRealCoordinites(context, user, jP.First);
			PVector jointTwo = super.getRealCoordinites(context, user, jP.Second);
			
			//coordinate retrieval failed on at least one joint
			if(jointOne == null || jointTwo == null)
				return;
			
			//compare joints and get relative position
			JointRelation relative = super.compareJointPositions(jP,jointOne, jointTwo);
			
			boolean conn = (i<Focus.size()-1);
			relative.C = conn;
			//add relative coordinates to proper joint pair record vector
			super.add(relative);
			i++;
		}
	}
	/**
	 * Processes the JointRecorder from [startTick , endTick) and adds data 
	 * to this based on focus joint pairs if available in log. If log does
	 * not have all focus joints that this requires then no data will be 
	 * processed. Note endTick is not included in the processed data.
	 *  
	 * @param log : JointRecorder to parse data from
	 * @param startTick : record tick to start at
	 * @param endTick : record tick after the final processed tick
	 */
	public void record(JointRecorder log, int startTick, int endTick){
		//basic checks about invalid input
		if (log == null || endTick > log.getTicks() || startTick >= endTick){
			System.out.println("Invalid arguments detected, aborting record");
			return;
		}
		//Checks that all focus points are available in log
		for (Pair jP : Focus){
			if (!(log.contains(jP.First) && log.contains(jP.Second))){
				System.out.println("Log does not contain all Focus joints, aborting record");
				return;
			}
		}
		
		//Add recorded data in range [startTime, endTime)
		for (int i=startTick;i<endTick;i++){
			int j=0;
			for (Pair jP : Focus){
				//Get Joints from the log at time i
				PVector jointOne = log.getJoint(i, jP.First);
				PVector jointTwo = log.getJoint(i, jP.Second);
				
				//Calculate relation between the joints
				JointRelation relative = super.compareJointPositions(jP,jointOne, jointTwo);
				
				//Add to the recorder
				boolean conn = (j<Focus.size()-1);
				relative.C = conn;
				super.add(relative);
				j++;
			}
		}
	}
	/**
	 * Records the entire log to this.recorder
	 * @param log : JointRecorder storing the data to process
	 */
	public void record(JointRecorder log){
		record(log, 0, log.getTicks());
	}
	/**
	 * Creates a GestureController that represents the minimum number of steps
	 * required to represent the recording. The recording is NOT reset upon completion
	 * and may be used to generate more gestures if desired.
	 * 
	 * @return
	 * 		GestureController representing the recorded gesture
	 */
	public GestureController generateGesture(CompressionType type){
		//makes sure that something is recored
		if (isEmpty()){
			System.out.println("NO recorded data operation terminated.");
			return null;
		}
		
		if (type != CompressionType.NONE){
			int oldNodes = super.size();
			System.out.println("Compressing Recording using CompressionType."+type);
			super.simplifyGesture(type);
			System.out.println("Record Compressed from "+oldNodes+" to "+super.size());
		}
		
		GestureController out = super.clone(); 
		if (debug) System.out.println(out.size());
		return out; 
	}
	/**
	 * Creates and returns a copy of focus
	 * @return
	 * 		Copy of Focus
	 */
	public Vector<Pair> getFocus(){
		Vector<Pair> v = new Vector<Pair>();
		v.addAll(Focus);
		return v;
	}
	/**
	 * Creates and returns a set of all unique joints in focus
	 * @return
	 * 		Set of joints in focus
	 */
	public Set<Integer> getFocusSet(){
		Set<Integer> s = new TreeSet<Integer>();
		for (Pair j : Focus){
			s.add(j.First);
			s.add(j.Second);
		}
		return s;
	}
	/**
	 * Clear all recorded data, focus pairs are preserved.
	 */
	public void clear(){
		super.clear();
	}
	@Override
	public String toString(){
		String ret = new String(); //string to return
		
		//basic info about recording
		ret += "Number of Joint Pairs: "+Focus.size()+'\n';
		
		if(Focus.size() == 0){
			ret += "No Recorded Data"+'\n';
			return ret;
		}
		ret += "Focus Pairs: "+Focus.size();
		for (Pair j : Focus){
			ret += j.toString()+'\n';
		}
		ret += super.toString();
		
		return ret;
	}
	/**
	 * Returns the xml representation of the gesture that this recording
	 * parses into without compression.
	 * @return
	 * 		xml representation of this
	 * @see #generateGesture(boolean)
	 * @see GestureController#toXML()
	 */
	public String toXML(){
		GestureController gC = generateGesture(CompressionType.NONE);
		return gC.toXML();
	}
}
