
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import processing.core.PApplet;
import processing.core.PVector;
import controller.Pair;
import controller.Skeleton;


public class SkeletalSelector {
	//Selector variables
	float jointSize=(float)7.5;
	
	Set<Integer> selected;
	Set<Pair> selectedPair;
	
	/**Denotes if the last action was an add so the undo is a remove.*/
	boolean lastActionAdd;
	Set<Integer> undoSelected;
	Set<Pair> undoSelectedPair;
	
	private Map<Integer, PVector> skeleton;
	private float scale;
	
	private Integer hitNode;
	private Integer heldNode;
	
	//stuff to draw text boxes with
	private int textDef[] = {50,20,75,20};
	private int finish[];
	private int undo[];
	private int clear[];
	private int all[];
	
	//PApplet stuff
	private PApplet screen;
	private Sidebar s;
	private int width, height;
	
	public SkeletalSelector(PApplet p, Sidebar side, int w, int h){
		screen = p;
		s = side;
		
		resize(w,h);
		
		finish= createTextBox(0);
		undo = createTextBox(1);
		clear = createTextBox(2);
		all = createTextBox(3);

	}
	private int[] createTextBox(int boxNum){
		int [] box = new int[4];
		box[0] = (int) (textDef[0]*scale);
		box[1] = (int) ((textDef[1]+((textDef[3]+5)*(boxNum)))*scale);
		box[2] = (int) (textDef[2]*scale);
		box[3] = (int) (textDef[3]*scale);
		return box;
	}
	private void initSelector(){
		selected = new HashSet<Integer>();
		selectedPair = new HashSet<Pair>();
		
		undoSelected = new HashSet<Integer>();
		undoSelectedPair = new HashSet<Pair>();
		
		skeleton = new HashMap<Integer, PVector>();
		
		float midx = width/2;
		float midy = (5*height)/16;
		float tH = 100*scale;
		float tW = (float)0.5*tH*scale;
		skeleton.put(Skeleton.TORSO.get(), new PVector(midx, midy));
		skeleton.put(Skeleton.SHOULDER.left(), new PVector(midx-tW, midy-tH));
		skeleton.put(Skeleton.SHOULDER.right(),  new PVector(midx+tW, midy-tH));
		skeleton.put(Skeleton.NECK.get(), new PVector(midx, skeleton.get(Skeleton.SHOULDER.left()).y));
		skeleton.put(Skeleton.HEAD.get(), new PVector(skeleton.get(Skeleton.NECK.get()).x,skeleton.get(Skeleton.NECK.get()).y-35*scale));
		skeleton.put(Skeleton.HIP.left(), new PVector(midx-tW, midy+tH));
		skeleton.put(Skeleton.HIP.right(), new PVector(midx+tW, midy+tH));
		skeleton.put(Skeleton.ELBOW.left(), new PVector(skeleton.get(Skeleton.TORSO.get()).x-85*scale, skeleton.get(Skeleton.TORSO.get()).y-40*scale));
		skeleton.put(Skeleton.ELBOW.right(), new PVector(skeleton.get(Skeleton.TORSO.get()).x+85*scale, skeleton.get(Skeleton.TORSO.get()).y-40*scale));
		skeleton.put(Skeleton.HAND.left(), new PVector(skeleton.get(Skeleton.HIP.left()).x-35*scale, skeleton.get(Skeleton.HIP.left()).y+10*scale));
		skeleton.put(Skeleton.HAND.right(), new PVector(skeleton.get(Skeleton.HIP.right()).x+35*scale, skeleton.get(Skeleton.HIP.right()).y+10*scale));
		skeleton.put(Skeleton.KNEE.left(), new PVector(skeleton.get(Skeleton.HIP.left()).x-5*scale, skeleton.get(Skeleton.HIP.left()).y+tH));
		skeleton.put(Skeleton.KNEE.right(), new PVector(skeleton.get(Skeleton.HIP.right()).x+5*scale, skeleton.get(Skeleton.HIP.right()).y+tH));
		skeleton.put(Skeleton.FOOT.left(), new PVector(skeleton.get(Skeleton.KNEE.left()).x-5*scale, skeleton.get(Skeleton.KNEE.left()).y+tH));
		skeleton.put(Skeleton.FOOT.right(), new PVector(skeleton.get(Skeleton.KNEE.right()).x+5*scale, skeleton.get(Skeleton.KNEE.right()).y+tH));
	}
	public void draw(){
		screen.background(0);
		drawSelectorSidebar();
		
		hitNode = null;
		screen.pushStyle();
		
		screen.fill(0);
		screen.stroke(0,0,255);
		drawLimb(Skeleton.NECK.get(), Skeleton.HEAD.get());
		drawLimb(Skeleton.NECK.get(), Skeleton.SHOULDER.left());
		drawLimb(Skeleton.NECK.get(), Skeleton.SHOULDER.right());
		drawLimb(Skeleton.SHOULDER.left(), Skeleton.TORSO.get());
		drawLimb(Skeleton.SHOULDER.right(), Skeleton.TORSO.get());
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
		
		screen.popStyle();
		
		Iterator<Pair> iter = selectedPair.iterator();
		while (iter.hasNext()){
			Pair linePair = iter.next();
			drawLimb(linePair.getFirst(),linePair.getSecond());
		}
		
		drawTextBox();
		
		drawHoldLine();
	}
	private void drawHoldLine() {
		if (heldNode == null){
			return;
		}
		PVector joint = skeleton.get(heldNode);
		PVector mouse = getMouse();
		screen.pushStyle();
		screen.fill(0xFFFF0000);
		screen.stroke(0xFFFF0000);
		screen.ellipse(joint.x, joint.y, jointSize, jointSize);
		screen.line(mouse.x, mouse.y, joint.x, joint.y);
		screen.popStyle();
	}
	private void drawSelectorSidebar(){
		Iterator<Integer> iter = selected.iterator();
		String tag=new String();
		while (iter.hasNext()){
			tag += iter.next().toString();
			if (iter.hasNext())
				tag+= ", ";
		}
		Iterator<Pair> pairIter = selectedPair.iterator();
		String pairTag = new String();
		while (pairIter.hasNext()){
			pairTag += pairIter.next().toString();
			if (pairIter.hasNext())
				pairTag += ", ";
		}
		Integer i = selected.size();
		s.update("Num Selected", i.toString());
		s.update("Selected", tag);
		s.update("Pairs Selected", pairTag);
		
		if (hitNode != null)
			s.update("Mouse Over", hitNode.toString());
		else
			s.clear("Mouse Over");
		s.draw();
	}
	private void addPair(Pair p){
		lastActionAdd = true;
		
		if(selectedPair.add(p))
			undoSelectedPair.add(p);
		
		addJoint(p.getFirst());
		addJoint(p.getSecond());
	}
	private void removePair(Pair p){
		lastActionAdd = false;
		
		if (selectedPair.remove(p))
			undoSelectedPair.add(p);
	}
	private void addJoint(Integer j){
		lastActionAdd = true;
		if (selected.add(j))
			undoSelected.add(j);
	}
	private void addAll(){
		Set<Integer> joints = Skeleton.getAllLeft();
		joints.addAll(Skeleton.getAllCenter());
		joints.addAll(Skeleton.getAllRight());
		Iterator<Integer> iter = joints.iterator();
		while (iter.hasNext()){
			addJoint(iter.next());
		}
	}
	private void removeJoint(Integer j){
		lastActionAdd = false;
		
		if (selected.remove(j)){
			undoSelected.add(j);
		}
		Iterator<Pair> iter =selectedPair.iterator();
		while (iter.hasNext()){
			Pair tmp = iter.next();
			if (tmp.getFirst().equals(j) || tmp.getSecond().equals(j)){
				undoSelectedPair.add(tmp);
				iter.remove();
			}
		}
	}
	private void undo(){
		if (lastActionAdd){
			if (!undoSelected.isEmpty()){
				Iterator<Integer> iter = undoSelected.iterator();
				while (iter.hasNext())
					removeJoint(iter.next());
			}
			if (!undoSelectedPair.isEmpty()){
				Iterator<Pair> iter = undoSelectedPair.iterator();
				while (iter.hasNext())
					removePair(iter.next());
			}
		}
		else{
			if (!undoSelected.isEmpty()){
				Iterator<Integer> iter = undoSelected.iterator();
				while (iter.hasNext())
					addJoint(iter.next());
			}
			if (!undoSelectedPair.isEmpty()){
				Iterator<Pair> iter = undoSelectedPair.iterator();
				while (iter.hasNext())
					addPair(iter.next());
			}
		}
		clearUndo();
	}
	private void clearUndo(){
		undoSelected.clear();
		undoSelectedPair.clear();
	}
	private void clearSidebar(){
		s.clear("Num Selected");
		s.clear("Selected");
		s.clear("Mouse Over");
		s.clear("Pairs Selected");
	}
	private void clear(){
		clearUndo();
		lastActionAdd = false;
		Iterator<Integer> iter = selected.iterator();
		while (iter.hasNext()){
			undoSelected.add(iter.next());
		}
		Iterator<Pair> iterPair = selectedPair.iterator();
		while (iterPair.hasNext()){
			undoSelectedPair.add(iterPair.next());
		}
		
		selected.clear();
		selectedPair.clear();
	}
	private void drawTextBox(){
		screen.pushMatrix();
		screen.pushStyle();
		screen.stroke(0);
		PVector mouse = getMouse();
		
		//style setup
		screen.rectMode(PApplet.CENTER);
		screen.textAlign(PApplet.CENTER);
		screen.textSize(12);
		
		//done box
		drawTextBox(mouse,finish,"Done");
		
		//undo box
		drawTextBox(mouse,undo,"Undo");
		
		//clear box
		drawTextBox(mouse,clear,"Clear");
		
		//all box
		drawTextBox (mouse, all, "Add all");
		
		screen.popMatrix();
		screen.popStyle();
	}
	private void drawTextBox(PVector mouse, int[] coord, String tag){
		int hit = foo(mouseHit(mouse, coord));
		screen.pushStyle();
		screen.fill(255*hit);
		screen.stroke(255*((hit+1)%2));
		screen.rect(coord[0], coord[1], coord[2], coord[3]);
		screen.fill(255*((hit+1)%2));
		screen.text(tag, coord[0],coord[1]+5);
		screen.popStyle();
	}
	private void drawLimb(Integer A, Integer B){
		PVector first = skeleton.get(A);
		PVector second = skeleton.get(B);
		
		screen.pushStyle();
		
		screen.fill(0);
		screen.stroke(0,0,255);
		if (selectedPair.contains(new Pair(A,B))){
			screen.stroke(255,0,0);
		}
		screen.line(first.x, first.y, second.x, second.y);
		
		drawJoint(A);
		drawJoint(B);
		
		screen.popStyle();
	}
	private void drawJoint(int joint){
		PVector point = skeleton.get(joint);
		
		boolean mH = mouseHit(new PVector(screen.mouseX, screen.mouseY),point);
		boolean cont = selected.contains(joint);
		
		if (mH){
			screen.fill(255,0,0);
			hitNode = joint;
		}
		else if (cont&& mH){
			screen.fill(200,0,0);
			hitNode = joint;
		}
		else if (cont){
			screen.fill(255);
		}
		else{
			screen.fill(0);
		}
		screen.ellipse(point.x, point.y, jointSize, jointSize);
	}
	private boolean mouseHit(PVector mouse, PVector joint){
		float deltaX = joint.x -mouse.x;
		float deltaY = joint.y-mouse.y;
		if (Math.sqrt(PApplet.sq(deltaX)+PApplet.sq(deltaY)) < jointSize){
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
	public boolean mouseClicked(){
		PVector mouse = getMouse();
		if (hitNode != null){
			clearUndo();
			if (selected.contains(hitNode)){
				removeJoint(hitNode);
			}
			else
				addJoint(hitNode);
		}
		else if (mouseHit(mouse, finish)){
			System.out.println("Nodes Selected: "+selected.toString());
			System.out.println("DONE");
			clearSidebar();
			return false;
		}
		else if (mouseHit(mouse, undo)){
			undo();
		}
		else if (mouseHit(mouse, clear)){
			clear();
		}
		else if (mouseHit(mouse, all)){
			addAll();
		}
		return true;
	}
	public void mousePressed(){
		if (hitNode != null){
			heldNode = hitNode;
		}
		else
			heldNode = null;
	}
	public void mouseReleased(){
		if (hitNode != null && heldNode != null && hitNode != heldNode){
			clearUndo();
			Pair selection = new Pair(heldNode, hitNode);
			if (selectedPair.contains(selection))
				removePair(selection);
			else
				addPair(selection);
		}
		heldNode = null;
	}
	public PVector getMouse(){
		return new PVector(screen.mouseX, screen.mouseY);
	}
	private int foo (boolean bar){
		if (bar) return 1;
		return 0;
	}
	public void resize(int w, int h){
		width = w;
		float scaleX = (float) (w/300.0);
		
		height = h;
		float scaleY = (float) (h/700.0);
		
		//choose the smaller of the two to keep the skeleton on screen and 
		//in the proper ratio.
		scale = Math.min(scaleX, scaleY);
		//because the skeleton was drawn small the first time upsize
		scale*=1.25; 
		
		initSelector();
	}
}
