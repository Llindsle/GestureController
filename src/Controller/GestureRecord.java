/**
 * 
 */
package Controller;

import java.util.Vector;

import processing.core.PVector;

import SimpleOpenNI.SimpleOpenNI;

/**
 * @author Levi Lindsley
 *
 */
public class GestureRecord extends GestureController {
	
	private class JointPair{
		Integer First;
		Integer Second;

		JointPair(int f, int s){
			First = new Integer(f);
			Second = new Integer(s);
		}
	}
	Vector <JointPair> Focus;
	Vector<Vector<PVector>> R;
	
	public GestureRecord(){
		Focus = new Vector<JointPair>();
		R = new Vector<Vector<PVector>>();
	}
	
	public void addFocusJoints(int first, int second){
		Focus.add(new JointPair(first,second));
		R.add(new Vector<PVector>());
	}
	
	public void record(SimpleOpenNI context, int user){
		for (int i=0;i<Focus.size();i++){
			//get relevant joint pair
			JointPair X = Focus.get(i);
			
			//get coordinates for both joints
			PVector JointOne = super.getRealCoordinites(context, user, X.First);
			PVector JointTwo = super.getRealCoordinites(context, user, X.Second);
			
			//compare joints and get relative position
			PVector Relative = super.comareJointPositions(JointOne,JointTwo);
			
			//add relative coordinates to proper joint pair record vector
			R.get(i).add(Relative);
		}
	}
	
	/**
	 * Removes all duplicate recorded joint comparisons that appear in sequence keeping only the head
	 * of the sequence in R.
	 */
	private void compressRecording(){
		for (Vector<PVector> RecordPair : R){
			if (RecordPair.size() == 0) continue;
			PVector alpha = RecordPair.firstElement();
			
			int i=1;
			while(i<RecordPair.size()){
				PVector beta = RecordPair.get(i);
				if (alpha.equals(beta)){
					RecordPair.remove(i);
				}
			}
		}
	}
	
	public GestureController generateGesture(){
		compressRecording();
		
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
	
	public void resetRecording(){
		Focus = new Vector<JointPair>();
		R = new Vector<Vector<PVector>>();
	}
}
