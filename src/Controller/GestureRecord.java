
package Controller;

import java.util.Vector;

import processing.core.PVector;

import SimpleOpenNI.SimpleOpenNI;

/**
 * @author Levi Lindsley
 *
 */
public class GestureRecord extends GestureController{
	private boolean debug = false;
	
	private Vector <JointPair> Focus;
	private Vector<Vector<PVector>> R;
	
	public GestureRecord(){
		Focus = new Vector<JointPair>();
		R = new Vector<Vector<PVector>>();
	}
	
	public boolean addFocusJoints(int first, int second){
		if (!isEmpty()){
			return false;
		}
		JointPair o = new JointPair(first, second);
		if (Focus.contains(o))
			return false;
		
		Focus.add(o);
		R.add(new Vector<PVector>());
		return true;
	}
	
	public void record(SimpleOpenNI context, int user){
		for (int i=0;i<Focus.size();i++){
			//get relevant joint pair
			JointPair X = Focus.get(i);
			
			//get coordinates for both joints
			PVector jointOne = GestureController.getRealCoordinites(context, user, X.First);
			PVector jointTwo = GestureController.getRealCoordinites(context, user, X.Second);
			
			//compare joints and get relative position
			PVector Relative = GestureController.compareJointPositions(jointOne, jointTwo);
			
			//add relative coordinates to proper joint pair record vector
			R.get(i).add(Relative);
		}
	}
	
	/**
	 * Removes all duplicate recorded joint comparisons that appear in sequence keeping only the head
	 * of the sequence in R.
	 */
	private void compressRecording(){
		int i=0;

		Vector<PVector> alpha = new Vector<PVector>();
		
		
		while(i < R.firstElement().size()){
			Vector<PVector> beta = new Vector<PVector>();
			
			for (int j=0;j<R.size();j++){
				beta.add(R.get(j).get(i));
			}
			if (i==0){
				alpha = beta;
				i++;
				continue;
			}
			
			boolean reset = true;
			PVector zero = new PVector(0,0,0);
			PVector diff = new PVector();
			if (debug) System.out.print(i+": ");
			for (int j=0;j<alpha.size();j++){
				diff.x = GestureController.comp(alpha.get(j).x, beta.get(j).x);
				diff.y = GestureController.comp(alpha.get(j).y, beta.get(j).y);
				diff.z = GestureController.comp(alpha.get(j).z, beta.get(j).z);
				if (!diff.equals(zero)){
					alpha = beta;
					reset = false;
					break;
				}
				else
					if (debug) System.out.print(j +" equal ");
			}
			if (debug) System.out.println();
			
			if (reset){
				if (debug) System.out.println("Removing duplicate");
				for (int j=0;j<R.size();j++){
					R.get(j).remove(i);
				}
			}
			else{
				i++;
			}
		}
	}
	
	public GestureController generateGesture(){
		if (debug) System.out.println("Compressing");
		compressRecording();
		if (debug) System.out.println("Record Compressed");
		
		GestureController control = new GestureController();
		
		//Assures that there is a recording
		if (R.isEmpty() || R.firstElement().isEmpty()) return null;

		PVector V;
		JointPair F;
		for (int i=0;i<R.firstElement().size();i++){
			for (int j=0;j<R.size()-1;j++){
				V = R.get(j).get(i);
				F = Focus.get(j);
				
				//add current step of each joint pair as concurrent
				control.addPoint(F.First, F.Second, (int)V.x, (int)V.y, (int)V.z, true);
			}
			V = R.lastElement().get(i);
			F = Focus.lastElement();
			
			//set final step as non-concurrent
			control.addPoint(F.First, F.Second, (int)V.x, (int)V.y, (int)V.z, false);
		}
		
		return control; 
	}
	
	boolean addNode(JointPair f, PVector t){

		int index = Focus.indexOf(f);
		if (index == -1)
			return false;
		
		R.get(index).add(t);
		
		return true;
	}
	
	public void resetRecording(){
		for (int i=0;i<R.size();i++){
			R.get(i).clear();
		}
	}
	
	public String toString(){
		char nl = '\n'; //the new line char 
		String ret = new String(); //string to return
		
		
		//basic info about recording
		ret += "Number of Joint Pairs: "+Focus.size()+nl;
		
		if(Focus.size() == 0){
			ret += "No Recorded Data"+nl;
			return ret;
		}
		ret += "Number of recorded steps: "+R.firstElement().size()+nl;
		
		
		JointPair F;
		PVector V;
		
		//each joint pairs step shown on a line
		for (int i=0;i<R.firstElement().size();i++){
			ret += i+": { ";
			for (int j=0;j<R.size();j++){
				F = Focus.get(j);
				V = R.get(j).get(i);
				ret += F.First+" "+F.Second+" ["+V.x+", "+V.y+", "+V.z+"] ";
			}
			ret += " }"+nl;
		}
		return ret;
	}
	 
	public boolean isEmpty(){
		return (R.isEmpty() || R.firstElement().isEmpty());
	}
}
