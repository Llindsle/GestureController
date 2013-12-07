package controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import SimpleOpenNI.SimpleOpenNI;

public enum Skeleton {
	HEAD(SimpleOpenNI.SKEL_HEAD),
	NECK(SimpleOpenNI.SKEL_NECK),
	TORSO(SimpleOpenNI.SKEL_TORSO),
	WAIST(SimpleOpenNI.SKEL_WAIST),
	ANKLE(SimpleOpenNI.SKEL_LEFT_ANKLE, SimpleOpenNI.SKEL_RIGHT_ANKLE),
	COLLAR(SimpleOpenNI.SKEL_LEFT_COLLAR, SimpleOpenNI.SKEL_RIGHT_COLLAR),
	ELBOW(SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_RIGHT_ELBOW),
	FINGERTIP(SimpleOpenNI.SKEL_LEFT_FINGERTIP, SimpleOpenNI.SKEL_RIGHT_FINGERTIP),
	FOOT(SimpleOpenNI.SKEL_LEFT_FOOT, SimpleOpenNI.SKEL_RIGHT_FOOT),
	HAND(SimpleOpenNI.SKEL_LEFT_HAND, SimpleOpenNI.SKEL_RIGHT_HAND),
	HIP(SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_RIGHT_HIP),
	KNEE(SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_RIGHT_KNEE),
	SHOULDER(SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_SHOULDER),
	WRIST(SimpleOpenNI.SKEL_LEFT_WRIST, SimpleOpenNI.SKEL_RIGHT_WRIST);
	
	private Integer LEFT;
	private Integer RIGHT;
	private boolean single;
	static boolean default_Left=true;
	Skeleton(int l, int r){
		LEFT = l;
		RIGHT = r;
		
	}
	Skeleton(int v){
		LEFT  = v;
		RIGHT = v;
		single = true;
	}
	public Integer get(){
		if (default_Left)
			return LEFT;
		return RIGHT;
	}
	public Integer left(){
		if (!single)
			return LEFT;
		return null;
	}
	public Integer right(){
		if (!single)
			return RIGHT;
		return null;
	}
	public Set<Integer> getAllLeft(){
		Set<Integer> set = new TreeSet<Integer>();
		Skeleton skel [] = Skeleton.values();
		for (int i=0;i<skel.length;i++){
			if (!skel[i].single)
				set.add(skel[i].left());
		}
		return set;
	}
	public Set<Integer> getAllRight(){
		Set<Integer> set = new TreeSet<Integer>();
		Skeleton skel [] = Skeleton.values();
		for (int i=0;i<skel.length;i++){
			if (!skel[i].single)
				set.add(skel[i].right());
		}
		return set;
	}
	public Set<Integer> getAllCenter(){
		Set<Integer> set = new TreeSet<Integer>();
		Skeleton skel [] = Skeleton.values();
		for (int i=0;i<skel.length;i++){
			if (skel[i].single)
				set.add(skel[i].get());
		}
		return set;
	}
	public static void setDefaultLeft(){
		default_Left = true;
	}
	public static void setDefaultRight(){
		default_Left = false;
	}
	public static Integer mirror(Integer val){
		Skeleton skel [] = Skeleton.values();
		for (int i=0;i<skel.length;i++){
			if (val == skel[i].left())
				return skel[i].right();
			if (val == skel[i].right())
				return skel[i].left();
		}
		return null;
	}
	public static Set<Integer> mirror(Set<Integer>val){
		Set<Integer> set = new TreeSet<Integer>();
		Skeleton skel [] = Skeleton.values();
		for (int i=0;i<skel.length;i++){
			if (skel[i].single){
				if (val.contains(skel[i].get()))
					set.add(skel[i].get());
			}
			else{
				if (val.contains(skel[i].left()))
					set.add(skel[i].right());
				if (val.contains(skel[i].right()))
					set.add(skel[i].left());
			}
		}
		return set;
	}
}
