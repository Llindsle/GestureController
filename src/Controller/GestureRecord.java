package controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import controller.GestureController.JointPair;

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
public class GestureRecord{
	/**Used only for debug purposes */
	@SuppressWarnings("unused")
	private boolean debug = true;
	
	/**Pairs of Joints to record relationship between*/
	private Vector <JointPair> Focus;
	
	/**A log of each focus joint pair relation for every call to record*/
	private Vector<Vector<PVector>> recorder;
	
	/**An instance of GestureController to access its functions*/
	private GestureController masterControl;
	
	/**
	 * Default Constructor, initializes control vectors
	 */
	public GestureRecord(){
		Focus = new Vector<JointPair>();
		recorder = new Vector<Vector<PVector>>();
		masterControl = new GestureController();
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
		JointPair o = masterControl.new JointPair(first, second);
		
		//Focus pairs may not be duplicated
		if (Focus.contains(o))
			return false;
		
		//Add new focus and increase the number of vectors in recorder
		Focus.add(o);
		recorder.add(new Vector<PVector>());
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
		for (int i=0;i<Focus.size();i++){
			//get relevant joint pair
			JointPair X = Focus.get(i);
			
			//get coordinates for both joints
			PVector jointOne = GestureController.getRealCoordinites(context, user, X.First);
			PVector jointTwo = GestureController.getRealCoordinites(context, user, X.Second);
			
			//coordinate retrieval failed on at least one joint
			if(jointOne == null || jointTwo == null)
				return;
			
			//compare joints and get relative position
			PVector Relative = GestureController.compareJointPositions(jointOne, jointTwo);
			
			//add relative coordinates to proper joint pair record vector
			recorder.get(i).add(Relative);
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
//			if (debug) System.out.println(log.toString()+'\n'+startTick+" "+endTick);
			return;
		}
		//Checks that all focus points are available in log
		for (JointPair jP : Focus){
			if (!(log.contains(jP.First) && log.contains(jP.Second))){
				System.out.println("Log does not contain all Focus joints, aborting record");
				return;
			}
		}
		
		//Add recorded data in range [startTime, endTime)
		for (int i=startTick;i<endTick;i++){
			Iterator<Vector<PVector>> itter = recorder.iterator();
			for (JointPair jP : Focus){
				//Get Joints from the log at time i
				PVector jointOne = log.getJoint(i, jP.First);
				PVector jointTwo = log.getJoint(i, jP.Second);
				
				//Calculate relation between the joints
				PVector relative = GestureController.compareJointPositions(jointOne, jointTwo);
				
				//Add to the recorder
				itter.next().add(relative);
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
	 * Removes all duplicate recorded joint comparisons that appear in sequence keeping only the head
	 * of the sequence in R.
	 */
	private void compressRecording(){
		int i=0; //index counter

		//primary sequence to compare to
		Vector<PVector> alpha = new Vector<PVector>();
		
		//while the end of the list has not been reached continue checking
		while(i < recorder.firstElement().size()){
			//sequence to compare alpha with
			Vector<PVector> beta = new Vector<PVector>();
			
			//fill beta with the ith element for every focus pair
			for (int j=0;j<recorder.size();j++){
				beta.add(recorder.get(j).get(i));
			}
			
			//for the first element set alpha and continue
			if (i==0){
				alpha = beta;
				i++;
				continue;
			}
			
			
			boolean reset = true;
//			if (debug) System.out.print(i+": ");
			for (int j=0;j<alpha.size();j++){
				if (!masterControl.equalAxes(alpha.get(j), beta.get(j))){
					alpha = beta;
					reset = false;
					break;
				}
//				else
//					if (debug) System.out.print(j +" equal ");
			}
//			if (debug) System.out.println();
			
			//If on all the focus points alpha == beta remove the duplicate elements
			if (reset){
//				if (debug) System.out.println("Removing duplicate");
				for (int j=0;j<recorder.size();j++){
					recorder.get(j).remove(i);
				}
			}
			
			//If the node was not removed then move on to the next one else
			//the previous one will have been pushed back into the current position
			else{
				i++;
			}
		}
	}
	/**
	 * Creates a GestureController that represents the minimum number of steps
	 * required to represent the recording. The recording is NOT reset upon completion
	 * and may be used to generate more gestures if desired.
	 * 
	 * @return
	 * 		GestureController representing the recorded gesture
	 */
	public GestureController generateGesture(boolean compress){
		//makes sure that something is recored
		if (isEmpty()){
			System.out.println("NO recorded data operation terminated.");
			return null;
		}
		
		if (compress){
			int oldNodes = recorder.firstElement().size();
			System.out.println("Compressing Recording");
			compressRecording();
			System.out.println("Record Compressed from "+oldNodes+" to "+recorder.firstElement().size());
		}
		
		//new gestureController to return
		GestureController control = new GestureController();

		PVector V;
		JointPair F;
		
		//pan through the recording and add each set of recorded points to the GestureController
		for (int i=0;i<recorder.firstElement().size();i++){
			for (int j=0;j<recorder.size()-1;j++){
				//get current step
				V = recorder.get(j).get(i);
				F = Focus.get(j);
				
				//add current step of each joint pair as concurrent
				control.addPoint(F.First, F.Second, (int)V.x, (int)V.y, (int)V.z, true);
			}
			//get final step
			V = recorder.lastElement().get(i);
			F = Focus.lastElement();
			
			//set final step as non-concurrent
			control.addPoint(F.First, F.Second, (int)V.x, (int)V.y, (int)V.z, false);
		}
		
		return control; 
	}
	/**
	 * Adds a recorded node? I'm confused the name seemed obvious.
	 * This is slightly dangerous as it is assumed that all focus points have equal number of 
	 * recorded points and this can invalidate that assumption.
	 * 
	 * @param f : focus joint pair that t relationship taken is from
	 * @param t : relationship to add to recorder
	 * @return
	 * 		True if there is a focus pair f to add t into the recording of.
	 */
	boolean addNode(JointPair f, PVector t){
		//TODO Make this less able to break things
		int index = Focus.indexOf(f);
		if (index == -1)
			return false;
		
		recorder.get(index).add(t);
		
		return true;
	}
	/**
	 * Creates and returns a copy of focus
	 * @return
	 * 		Copy of Focus
	 */
	public Vector<JointPair> getFocus(){
		Vector<JointPair> v = new Vector<JointPair>();
		for (JointPair j : Focus){
			v.add(masterControl.new JointPair(j.First, j.Second));
		}
		return v;
	}
	/**
	 * Creates and returns a set of all unique joints in focus
	 * @return
	 * 		Set of joints in focus
	 */
	public Set<Integer> getFocusSet(){
		Set<Integer> s = new TreeSet<Integer>();
		for (JointPair j : Focus){
			s.add(j.First);
			s.add(j.Second);
		}
		return s;
	}
	/**
	 * Clear all recorded, data focus pairs are preserved.
	 */
	public void resetRecording(){
		for (int i=0;i<recorder.size();i++){
			recorder.get(i).clear();
		}
	}
	/**
	 * Returns data about this recording in a nice string format
	 */
	@Override
	public String toString(){
		char nl = '\n'; //the new line char, man I'm lazy
		String ret = new String(); //string to return
		
		
		//basic info about recording
		ret += "Number of Joint Pairs: "+Focus.size()+nl;
		
		if(Focus.size() == 0){
			ret += "No Recorded Data"+nl;
			return ret;
		}
		ret += "Number of recorded steps: "+recorder.firstElement().size()+nl;
		
		
		JointPair F;
		PVector V;
		
		//Show all focus pairs on a line and display each step on a new line
		for (int i=0;i<recorder.firstElement().size();i++){
			ret += i+": { ";
			for (int j=0;j<recorder.size();j++){
				F = Focus.get(j);
				V = recorder.get(j).get(i);
				ret += F.First+" "+F.Second+" ["+V.x+", "+V.y+", "+V.z+"] ";
			}
			ret += " }"+nl;
		}
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
		GestureController gC = generateGesture(false);
		return gC.toXML();
	}
	/**
	 * Returns true if there is no recorded data. Will return true if there is focus pairs but
	 * no data has been recorded.
	 * 
	 * @return
	 * 		True if no data is recored.
	 */
	public boolean isEmpty(){
		return (recorder.isEmpty() || recorder.firstElement().isEmpty());
	}
}
