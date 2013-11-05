package Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import processing.core.PVector;

import SimpleOpenNI.SimpleOpenNI;

/**
 * JointRecorder records the position of joints and can be used to access 
 * the location of a joint at a specific point in time.
 * @author Levi Lindsley
 *
 */
public class JointRecorder {
	@SuppressWarnings("unused")
	private boolean debug = true;

	private Set<Integer> Joints;
	private List<Map<Integer, PVector>> recorder;
	
	public JointRecorder(){
		Joints = new TreeSet<Integer>();
		recorder = new ArrayList<Map<Integer, PVector>>();
	}
	
	public boolean addJoint(int j){
//		if (debug) System.out.println(j);
		
		if (!isEmpty())
			return false;
		
		return Joints.add(j);
	}
	public boolean addAll(){
		if (!isEmpty())
			return false;
		
		boolean set = false;
		for (int i=0;i<25;i++){
			//Add Joints and profiles, this may work
			set = addJoint(i) || set;
		}
		return set;
	}
	public void record(SimpleOpenNI context, int user){
		if (debug) System.out.println("recording ...");
		Map<Integer, PVector> v = new HashMap<Integer, PVector>();
		PVector p = new PVector();
		for (Integer j : Joints){
			p = GestureController.getRealCoordinites(context, user, j);
			v.put(j, p);
		}
		recorder.add(v);
	}
	
	public PVector getJoint(int time, int joint){
		if (!Joints.contains(joint) || !(time < recorder.size())){
			return null;
		}
		return recorder.get(time).get(joint);
	}
	public int getTicks(){
		return recorder.size();
	}
	public boolean contains(int j){
		return Joints.contains(j);
	}
	public boolean containsAll(Set<Integer> j){
		return Joints.containsAll(j);
	}
	public boolean isEmpty(){
		return recorder.isEmpty();
	}
	public void clear(){
		recorder.clear();
	}
	public String toString(){
		String ret = new String();
		
		ret += "Number of joints recorded: "+Joints.size()+'\n';
		ret += "Number of recording ticks: "+recorder.size()+'\n';
		for (Map<Integer, PVector> m : recorder){
			ret += m.toString()+'\n';
		}
		return ret;
	}
}
