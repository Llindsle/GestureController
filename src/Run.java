

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import SimpleOpenNI.SimpleOpenNI;

import controller.Skeleton;

import processing.core.PApplet;
import processing.core.PVector;

public class Run extends PApplet {
	Sidebar s;
	float jointSize=(float)7.5;
	Set<Integer> selected;
	Map<Integer, PVector> skeleton;
	Integer hitNode;
	int finish[] = {50,20,75,20};
	SimpleOpenNI si;
	public void setup(){
		si = new SimpleOpenNI(this);
		size(width(),height());
		selected = new HashSet<Integer>();
		skeleton = new HashMap<Integer, PVector>();
		s = new Sidebar(this);
	}
	public void draw(){
//		background(0);
		drawSidebar();
		float midx = width()/2;
		float midy = height()/2;
		hitNode = null;
		
		skeleton.put(Skeleton.TORSO.get(), new PVector(midx, midy));
		skeleton.put(Skeleton.SHOULDER.left(), new PVector(midx-50, midy-100));
		skeleton.put(Skeleton.SHOULDER.right(),  new PVector(midx+50, midy-100));
		skeleton.put(Skeleton.NECK.get(), new PVector(midx, skeleton.get(Skeleton.SHOULDER.left()).y-10));
		skeleton.put(Skeleton.HEAD.get(), new PVector(skeleton.get(Skeleton.NECK.get()).x, skeleton.get(Skeleton.NECK.get()).y-35));
		skeleton.put(Skeleton.HIP.left(), new PVector(midx-50, midy+100));
		skeleton.put(Skeleton.HIP.right(), new PVector(midx+50, midy+100));
		skeleton.put(Skeleton.ELBOW.left(), new PVector(skeleton.get(Skeleton.TORSO.get()).x-85, skeleton.get(Skeleton.TORSO.get()).y-40));
		skeleton.put(Skeleton.ELBOW.right(), new PVector(skeleton.get(Skeleton.TORSO.get()).x+85, skeleton.get(Skeleton.TORSO.get()).y-40));
		skeleton.put(Skeleton.HAND.left(), new PVector(skeleton.get(Skeleton.HIP.left()).x-35, skeleton.get(Skeleton.HIP.left()).y+10));
		skeleton.put(Skeleton.HAND.right(), new PVector(skeleton.get(Skeleton.HIP.right()).x+35, skeleton.get(Skeleton.HIP.right()).y+10));
		skeleton.put(Skeleton.KNEE.left(), new PVector(skeleton.get(Skeleton.HIP.left()).x-5, skeleton.get(Skeleton.HIP.left()).y+100));
		skeleton.put(Skeleton.KNEE.right(), new PVector(skeleton.get(Skeleton.HIP.right()).x+5, skeleton.get(Skeleton.HIP.right()).y+100));
		skeleton.put(Skeleton.FOOT.left(), new PVector(skeleton.get(Skeleton.KNEE.left()).x-5, skeleton.get(Skeleton.KNEE.left()).y+100));
		skeleton.put(Skeleton.FOOT.right(), new PVector(skeleton.get(Skeleton.KNEE.right()).x+5, skeleton.get(Skeleton.KNEE.right()).y+100));
		
		fill(0);
		stroke(0);
		drawLimb(Skeleton.NECK.get(), Skeleton.HEAD.get());
		drawLimb(Skeleton.SHOULDER.left(), Skeleton.NECK.get());
		drawLimb(Skeleton.SHOULDER.right(), Skeleton.NECK.get());
		drawLimb(Skeleton.TORSO.get(), Skeleton.SHOULDER.left());
		drawLimb(Skeleton.TORSO.get(), Skeleton.SHOULDER.right());
		drawLimb(Skeleton.TORSO.get(), Skeleton.HIP.left());
		drawLimb(Skeleton.TORSO.get(), Skeleton.HIP.right());
		
		Skeleton.setDefaultLeft();
		drawLimb(Skeleton.SHOULDER.get(), Skeleton.ELBOW.get());
		drawLimb(Skeleton.ELBOW.get(), Skeleton.HAND.get());
		drawLimb(Skeleton.HIP.get(), Skeleton.KNEE.get());
		drawLimb(Skeleton.KNEE.get(), Skeleton.FOOT.get());
		
		Skeleton.setDefaultRight();
		drawLimb(Skeleton.SHOULDER.get(), Skeleton.ELBOW.get());
		drawLimb(Skeleton.ELBOW.get(), Skeleton.HAND.get());
		drawLimb(Skeleton.HIP.get(), Skeleton.KNEE.get());
		drawLimb(Skeleton.KNEE.get(), Skeleton.FOOT.get());
		
		drawTextBox();
	}
	private void drawSidebar(){
		Iterator<Integer> iter = selected.iterator();
		String tag=new String();
		while (iter.hasNext()){
			tag += iter.next().toString();
			if (iter.hasNext())
				tag+= ", ";
		}
		Integer i = selected.size();
		s.update("Num Selected", i.toString());
		s.update("Selected", tag);
		if (hitNode != null)
			s.update("Mouse Over", hitNode.toString());
		else
			s.clear("Mouse Over");
		s.draw();
	}
	private void drawTextBox(){
		pushMatrix();
		pushStyle();
		int hit=0;
		if (mouseHit(new PVector(mouseX, mouseY),finish))
			hit=1;
		else
			hit=0;
		fill(255*hit);
		rectMode(CENTER);
		rect(finish[0],finish[1],finish[2], finish[3]);
//		fill(255);
		fill(255*((hit+1)%2));
		textAlign(CENTER);
		text("Done", finish[0],finish[1]+5);
		popMatrix();
		popStyle();
	}
	private void drawLimb(Integer A, Integer B){
		PVector First = skeleton.get(A);
		PVector Second = skeleton.get(B);
		line(First.x, First.y, Second.x, Second.y);
		if (selected.contains(B) || mouseHit(new PVector(mouseX, mouseY), Second)){
			pushStyle();
			fill(255);
			ellipse(Second.x, Second.y, jointSize, jointSize);
			popStyle();
			if (!selected.contains(B))
				hitNode = B;
		}
		else{
			ellipse(Second.x, Second.y, jointSize, jointSize);
		}
	}
	private boolean mouseHit(PVector mouse, PVector joint){
		float deltaX = joint.x -mouse.x;
		float deltaY = joint.y-mouse.y;
		if (sqrt(sq(deltaX)+sq(deltaY)) < jointSize){
			return true;
		}
		return false;
	}
	private boolean mouseHit(PVector mouse, int [] box){
		if (mouse.x >= box[0]-(box[2]/2) && mouse.x <= box[0]+(box[2]/2))
			if (mouse.y >= box[1]-(box[3]/2)&& mouse.y <= box[1]+(box[3]/2))
				return true;
		return false;
	}
	public void mouseClicked(){
		if (hitNode != null){
//			println(hitNode);
			selected.add(hitNode);
		}
		else if (mouseHit(new PVector(mouseX, mouseY), finish)){
			System.out.println("Nodes Selected: "+selected.toString());
			System.out.println("DONE");
			selected.clear();
		}
	}
	public int width(){
		return 300;
	}
	public int height(){
		return 700;
	}
}
