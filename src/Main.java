import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.XMLFormatter;

import controller.*;
import controller.GestureController.CompressionType;

import SimpleOpenNI.*;
import processing.core.*;

public class Main extends PApplet{
	/** A serialVersionUID to make eclipse happy*/
	private static final long serialVersionUID = 1L;
	SimpleOpenNI  context;
	boolean autoCalib=true;
	boolean debug = true;
	boolean Recording = false;
	boolean playback = false;
	boolean unitMode = false;
	int compressionMask = 0xf;

	Vector<GestureController> gesture;
	GestureRecord log;
	JointRecorder jR;
	int color = 0;

	VirtualKitchen vK;
	Sidebar s;

	//selector variables
	boolean selectorActive = true;
	float jointSize=(float)7.5;
	Set<Integer> selected;
	Set<Pair> selectedPair;
	Map<Integer, PVector> skeleton;
	Integer hitNode;
	Integer heldNode;
	int finish[] = {50,20,75,20};
 
	public void setup()
	{	
		vK = new VirtualKitchen(this);
		
		context = new SimpleOpenNI(this);
//		printSkeletalConst();
//		boolean b= true;
//		if (b) return;
		
		gesture = new Vector<GestureController>();
		log = new GestureRecord();
		jR = new JointRecorder();
		jR.addJoint(SimpleOpenNI.SKEL_LEFT_ELBOW);
		jR.addJoint(SimpleOpenNI.SKEL_LEFT_HAND);
		jR.addJoint(SimpleOpenNI.SKEL_LEFT_SHOULDER);
		System.out.println(jR.toString());
		//	  jR.addJoint(SimpleOpenNI.SKEL_RIGHT_HAND);
		//	  jR.addJoint(SimpleOpenNI.SKEL_RIGHT_ELBOW);
		//	  jR.addAll();

		//	  gesture.add(new GestureController("Wave"));
		//	  createWaveGesture(gesture.lastElement());

//		log.addFocusJoints(SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);
			  log.addFocusJoints(SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);
		//	  log.addFocusJoints(SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_LEFT_ELBOW);

		//	  try{
		//		  Scanner xmlInput = new Scanner(new File("Gesture.xml"));
		//		  GestureController l = new GestureController();
		//		  l = l.load(xmlInput);
		//		  System.out.println(l.toString());
		//	  }catch(Exception e){
		//		  System.err.println(e.getMessage());
		//		  e.printStackTrace();
		//	  }

		//	  gesture.add(new GestureController("Stir"));
		//	  createStirGesture(gesture.lastElement());

		//	  xmlGestureParser.save("Gesture.xml", gesture);
		//	  
		//	  gesture.add(new GestureController("High Wave"));
		//	  createWaveGesture(gesture.lastElement());
		//	  gesture.lastElement().addConstant(SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_LEFT_HAND, 
		//			  null, 1, null);

//		printSkeletalConst(); 
		
		// enable depthMap generation 
		if(context.enableDepth() == false)
		{
			println("Can't open the depthMap, maybe the camera is not connected!"); 
			exit();
			return;
		}

		// enable skeleton generation for all joints
		context.enableUser(SimpleOpenNI.SKEL_PROFILE_ALL);
		context.setMirror(true);

		background(200,0,0);
		smooth();

		size(width(), height()); 
		textAlign(CENTER);

		//create sidebar
		s = new Sidebar(this);
		
		//init collections for selector
		initSelector();
		activateSelector();
		
		System.out.println("Setup complete");
	}
	public void draw()
	{
		if (selectorActive){
			drawSelector();
			return;
		}
		// update the cam
		context.update();

		if (unitMode){
			unitDraw();
			return;
		}
		// draw depthImageMap
		image(context.depthImage(),0,0);

		//	  stroke (255,0,color);
		//	  color = (color+1)%255;
		// draw the skeleton if it's available
		int[] userList = context.getUsers();
		for(int i=0;i<userList.length;i++)
		{
			if(context.isTrackingSkeleton(userList[i])){
				PVector leftHand = new PVector(), projLeftHand = new PVector();
				PVector rightHand = new PVector(), projRightHand = new PVector();
				context.getJointPositionSkeleton(userList[i], SimpleOpenNI.SKEL_LEFT_HAND, leftHand);
				context.getJointPositionSkeleton(userList[i], SimpleOpenNI.SKEL_RIGHT_HAND, rightHand);

				context.convertRealWorldToProjective(rightHand, projRightHand);
				context.convertRealWorldToProjective(leftHand, projLeftHand);

				vK.drawContext(userList[i], projLeftHand, projRightHand,width(), height());
//				drawSkeleton(userList[i]);
//				drawSkeletonPrime(userList[i]);
				if(playback){
					viewRecord();
					for (int j=0;j<gesture.size();j++)
						if (gesture.get(j).isComplete(jR, jR.getPlayBackTick())){
							System.out.println(gesture.get(j).Name);
						}
//					return;
				}
				 else
					  drawSkeletonPrime(userList[i]);

				//check the gesture for completion
				for (int j=0;j<gesture.size();j++)
					if (gesture.get(j).isComplete(context, userList[i])){
						System.out.println(gesture.get(j).Name);
					}
				if (Recording){
					jR.record(context, userList[i]);
				}
			}
		}    
		drawSidebar();
	}
	private void drawSidebar(){
		s.update("Tracking Gestures", gesture.size()+"");
		s.update("jR", jR.toString());
		s.draw();
	}
	private void activateSelector(){
		jR.getJoints(selected);
		selectedPair.addAll(log.getFocus());
		jR.clearFocus();
		log = new GestureRecord();
		selectorActive = true;
	}
	private void deactivateSelector(){
		s.clear("Num Selected");
		s.clear("Selected");
		s.clear("Mouse Over");
		jR.addAll(selected);
		
		Iterator<Pair> iter = selectedPair.iterator();
		while (iter.hasNext()){
			Pair linePair = iter.next();
			log.addFocusJoints(linePair.getFirst(), linePair.getSecond());
		}
		selectorActive = false;
	}
	private void initSelector(){
		selected = new HashSet<Integer>();
		selectedPair = new HashSet<Pair>();
		skeleton = new HashMap<Integer, PVector>();
		
		float midx = width()/2;
		float midy = height()/2;
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
	}
	private void drawSelector(){
		background(0);
		drawSelectorSidebar();
		
		hitNode = null;
		pushStyle();
		
		fill(0);
		stroke(0,0,255);
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
		
		
		Iterator<Pair> iter = selectedPair.iterator();
		while (iter.hasNext()){
			Pair linePair = iter.next();
			stroke(200,0,0);
			drawLimb(linePair.getFirst(),linePair.getSecond());
		}
		popStyle();
		
		drawTextBox();
	}
	private void drawSelectorSidebar(){
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
		stroke(0);
		int hit=0;
		if (mouseHit(new PVector(mouseX, mouseY),finish))
			hit=1;
		else
			hit=0;
		fill(255*hit);
		rectMode(CENTER);
		rect(finish[0],finish[1],finish[2], finish[3]);
		fill(255*((hit+1)%2));
		textAlign(CENTER);
		textSize(12);
		text("Done", finish[0],finish[1]+5);
		popMatrix();
		popStyle();
	}
	private void drawLimb(Integer A, Integer B){
		PVector First = skeleton.get(A);
		PVector Second = skeleton.get(B);
		pushStyle();
		line(First.x, First.y, Second.x, Second.y);
		boolean mH = mouseHit(new PVector(mouseX, mouseY), Second);
		
		if (selected.contains(B)&& mH){
			fill(200,0,0);
			hitNode = B;
		}
		else if (mH){
			fill(240,248,255);
			hitNode = B;
		}
		else if (selected.contains(B)){
			fill(255);
		}
		else{
			fill(0);
		}
		ellipse(Second.x, Second.y, jointSize, jointSize);
		popStyle();
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
		if (!selectorActive)
			return;
		
		if (hitNode != null){
//			println(hitNode);
			if (selected.contains(hitNode))
				selected.remove(hitNode);
			else
				selected.add(hitNode);
		}
		else if (mouseHit(new PVector(mouseX, mouseY), finish)){
			System.out.println("Nodes Selected: "+selected.toString());
			System.out.println("DONE");
			deactivateSelector();
		}
	}
	public void mousePressed(){
		if (!selectorActive)
			return;
		if (hitNode != null){
			heldNode = hitNode;
		}
		else
			heldNode = null;
	}
	public void mouseReleased(){
		if (!selectorActive)
			return;
		if (hitNode != null && heldNode != null){
			selected.add(heldNode);
			selected.add(hitNode);
			Pair selection = new Pair(heldNode, hitNode);
			if (selectedPair.contains(selection))
				selectedPair.remove(selection);
			else
				selectedPair.add(new Pair(heldNode, hitNode));
		}
	}
	private void boringShape(PVector corner, int v){
		if (corner.equals(new PVector(0,0,0)))
			return;
		float midY = context.depthHeight()/2;
		midY += (v<2) ? -1*(midY/2) : (midY/2); 
		float midX = context.depthWidth()/2;
		midX += (v&1)==0 ? -1*(midX/2) : (midX/2);
		float radius = GestureController.getTolerance().floatValue()*1000;
		ellipse(corner.x+midX, corner.y+midY, radius, radius);
	}
	private void unitDraw() {
		fill(240,248,255);
		rect(0,0,context.depthWidth(),context.depthHeight());
		// draw the skeleton if it's available
		int[] userList = context.getUsers();
		for(int i=0;i<userList.length;i++)
		{
			if(context.isTrackingSkeleton(userList[i])){
				Vector<PVector> doodle = new Vector<PVector>();
				//check the gesture for completion
				for (int j=0;j<gesture.size();j++){
					if (gesture.get(j).isComplete(context, userList[i],doodle)){
						System.out.println(gesture.get(j).Name);
					}
					if (!doodle.isEmpty()){
						//		    		  System.out.println("drawing");
						stroke(255,0,0);
						doodle.get(0).mult(-500);
						boringShape(doodle.get(0),j);

						stroke(0,255,0);
						doodle.get(1).mult(-500);
						boringShape(doodle.get(1),j);

						stroke(0,0,255);
						doodle.get(2).mult(-500);
						boringShape(doodle.get(2),j);

					}
				}
				if (Recording){
					jR.record(context, userList[i]);
				}
			}
		}    
	}

	void togglePlayBack(){
		playback = !playback;
		System.out.print("Playback mode: ");
		if(!playback){
			s.clear("Playback");
			jR.resetPlayBack();
			System.out.print("dis");
		}
		System.out.println("engaged");
	}
	void viewRecord(){
		s.update("Playback", playback+"");
		drawSkeleton(jR.playBack());
	}
	void drawSkeleton(List<PVector[]> points){
		if (points == null){
			togglePlayBack();
			return;
		}
		for (PVector[] V : points){
			pushStyle();
			stroke(0,255,0);
			line (V[0].x, V[0].y, V[1].x, V[1].y);
			popStyle();
		}
	}
	void printSkeletalConst(){
		System.out.println("Center:");
		System.out.println("Head "+SimpleOpenNI.SKEL_HEAD);
		System.out.println("Neck "+SimpleOpenNI.SKEL_NECK);
		System.out.println("Torso "+SimpleOpenNI.SKEL_TORSO);
		System.out.println("Waist "+SimpleOpenNI.SKEL_WAIST);
		System.out.println('\n'+"Left:");
		System.out.println("Ankle "+SimpleOpenNI.SKEL_LEFT_ANKLE);
		System.out.println("Collar "+SimpleOpenNI.SKEL_LEFT_COLLAR);
		System.out.println("Elbow "+SimpleOpenNI.SKEL_LEFT_ELBOW);
		System.out.println("Fingertip "+SimpleOpenNI.SKEL_LEFT_FINGERTIP);
		System.out.println("Foot "+SimpleOpenNI.SKEL_LEFT_FOOT);
		System.out.println("Hand "+SimpleOpenNI.SKEL_LEFT_HAND);
		System.out.println("Hip "+SimpleOpenNI.SKEL_LEFT_HIP);
		System.out.println("Knee "+SimpleOpenNI.SKEL_LEFT_KNEE);
		System.out.println("Shoulder "+SimpleOpenNI.SKEL_LEFT_SHOULDER);
		System.out.println("Wrist "+SimpleOpenNI.SKEL_LEFT_WRIST);
		System.out.println('\n'+"Right:");
		System.out.println("Ankle "+SimpleOpenNI.SKEL_RIGHT_ANKLE);
		System.out.println("Collar "+SimpleOpenNI.SKEL_RIGHT_COLLAR);
		System.out.println("Elbow "+SimpleOpenNI.SKEL_RIGHT_ELBOW);
		System.out.println("Fingertip "+SimpleOpenNI.SKEL_RIGHT_FINGERTIP);
		System.out.println("Foot "+SimpleOpenNI.SKEL_RIGHT_FOOT);
		System.out.println("Hand "+SimpleOpenNI.SKEL_RIGHT_HAND);
		System.out.println("Hip "+SimpleOpenNI.SKEL_RIGHT_HIP);
		System.out.println("Knee "+SimpleOpenNI.SKEL_RIGHT_KNEE);
		System.out.println("Shoulder "+SimpleOpenNI.SKEL_RIGHT_SHOULDER);
		System.out.println("Wrist "+SimpleOpenNI.SKEL_RIGHT_WRIST);

	}
	void drawLimb(int user, int First, int Second){
		//PVectors to store joint position data and converted position data
		PVector Joint = new PVector();
		PVector R1 = new PVector();
		PVector R2 = new PVector();
		
		//get joint data from context as determined by the c
		context.getJointPositionSkeleton(user, First, Joint);
		context.convertRealWorldToProjective(Joint, R1);
		context.getJointPositionSkeleton(user, Second, Joint);
		context.convertRealWorldToProjective(Joint, R2);
		
		pushStyle();
		if (jR.contains(First)&& jR.contains(Second))
			stroke(255,0,0);
		line(R1.x, R1.y, R2.x, R2.y);
		popStyle();

	}
	void drawSkeletonPrime(int userId){
		// to get the 3d joint data
		/*
	  PVector jointPos = new PVector();
	  context.getJointPositionSkeleton(userId,SimpleOpenNI.SKEL_NECK,jointPos);
	  println(jointPos);
		 */
		strokeWeight(2);
		stroke(0);
		drawLimb(userId, Skeleton.HEAD.get(), Skeleton.NECK.get());

		Skeleton.setDefaultLeft();
		drawLimb(userId, Skeleton.NECK.get(), Skeleton.SHOULDER.get());
		drawLimb(userId, Skeleton.SHOULDER.get(), Skeleton.ELBOW.get());
		drawLimb(userId, Skeleton.ELBOW.get(), Skeleton.HAND.get());

		Skeleton.setDefaultRight();
		drawLimb(userId, Skeleton.NECK.get(), Skeleton.SHOULDER.get());
		drawLimb(userId, Skeleton.SHOULDER.get(), Skeleton.ELBOW.get());
		drawLimb(userId, Skeleton.ELBOW.get(), Skeleton.HAND.get());

		drawLimb(userId, Skeleton.SHOULDER.left(), Skeleton.TORSO.get());
		drawLimb(userId, Skeleton.SHOULDER.right(), Skeleton.TORSO.get());

		Skeleton.setDefaultLeft();
		drawLimb(userId, Skeleton.TORSO.get(), Skeleton.HIP.get());
		drawLimb(userId, Skeleton.HIP.get(), Skeleton.KNEE.get());
		drawLimb(userId, Skeleton.KNEE.get(), Skeleton.FOOT.get());

		
		Skeleton.setDefaultRight();
		drawLimb(userId, Skeleton.TORSO.get(), Skeleton.HIP.get());
		drawLimb(userId, Skeleton.HIP.get(), Skeleton.KNEE.get());
		drawLimb(userId, Skeleton.KNEE.get(), Skeleton.FOOT.get());  
		noStroke();
	}
	// draw the skeleton with the selected joints
	void drawSkeleton(int userId)
	{
		// to get the 3d joint data
		/*
	  PVector jointPos = new PVector();
	  context.getJointPositionSkeleton(userId,SimpleOpenNI.SKEL_NECK,jointPos);
	  println(jointPos);
		 */
		strokeWeight(2);
		stroke(0);
		context.drawLimb(userId, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_NECK);

		context.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_LEFT_SHOULDER);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_LEFT_ELBOW);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);

		context.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_RIGHT_SHOULDER);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_ELBOW);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);

		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_TORSO);

		context.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_LEFT_HIP);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_LEFT_KNEE);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_LEFT_FOOT);

		context.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_RIGHT_HIP);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_HIP, SimpleOpenNI.SKEL_RIGHT_KNEE);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, SimpleOpenNI.SKEL_RIGHT_FOOT);  
		noStroke();
	}

	// -----------------------------------------------------------------
	// SimpleOpenNI events

	public void keyPressed(){
		if (key == 'u'){
			unitMode = !unitMode;
		}
		if (key == 'p'){
			togglePlayBack();
		}
		if (key == 'c'){
			jR.clear();
		}
		if (key == 32){ //space
			if (selectorActive){
				deactivateSelector();
			}
			else if (Recording){
				System.out.println("Recording End");
				Recording = false;
			}
			else if (!Recording){
				if (jR.isEmpty()){
					System.out.println("Recording Start");
					Recording = true;
				}
				else{
					processRecording();
				}
			}
		}
		if (key == 's'){
			if(!gesture.isEmpty()){
				LogRecord log = new LogRecord(Level.INFO, gesture.toString()) ;
				XMLFormatter xF = new XMLFormatter();

				//				FileOutputStream fos;
				try {
					BufferedWriter out = new BufferedWriter(new FileWriter("geture.xml"));
					out.write(xF.format(log));
					out.flush();
					out.close();
					//					fos = new FileOutputStream("gesture.tmp");
					//					ObjectOutputStream oos = new ObjectOutputStream(fos);
					//					oos.writeObject(gesture);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//			    
				////				gesture.lastElement().save("Gesture.xml",gesture);
			}
			//xmlGestureParser.save("Gesture.xml", gesture);
			//xmlGestureParser.save("Gesture.xml", jR);
		}
		if (key == 'l'){
			gesture.clear();
			FileInputStream fos;
			try {
				fos = new FileInputStream("gesture.tmp");
				ObjectInputStream oos = new ObjectInputStream(fos);
				gesture = (Vector<GestureController>) oos.readObject();
				System.out.println("Gestures Loaded: "+gesture.size());
				System.out.println(gesture);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		if (key == '/'){
			if (selectorActive){
				deactivateSelector();
			}
			else{
				activateSelector();
			}
		}
	}
	private GestureController compressRecord(CompressionType type){
		GestureController g;
		
		log.record(jR);
		g = log.generateGesture(CompressionType.NONE);
		if (g == null)
			return g;
		
		log.clear();
		g.Name = "Gesture "+gesture.size()+ " (generated)";
		System.out.println(g);
		return g;
	}
	private void processRecording(){
		GestureController g;
		if ((compressionMask & CompressionType.NONE.getMask()) != 0){
			g = compressRecord(CompressionType.NONE);
			if (g != null){
				gesture.add(g);
				System.out.println("Gesture "+gesture.size()+" generated");
			}
			else
				return;
		}

		if ((compressionMask & CompressionType.SIMPLE.getMask()) != 0){
			g = compressRecord(CompressionType.SIMPLE);
			if (g != null){
				gesture.add(g);
				System.out.println("Gesture "+gesture.size()+" generated");
			}
			else
				return;
		}

		if ((compressionMask & CompressionType.AVG.getMask()) != 0){
			g = compressRecord(CompressionType.AVG);
			if (g != null){
				gesture.add(g);
				System.out.println("Gesture "+gesture.size()+" generated");
			}
			else
				return;
		}

		if ((compressionMask & CompressionType.DBL_AVG.getMask()) != 0){
			g = compressRecord(CompressionType.DBL_AVG);
			if (g != null){
				gesture.add(g);
				System.out.println("Gesture "+gesture.size()+" generated");
			}
			else
				return;
		}

//		jR.clear();
	}
	public void onNewUser(int userId)
	{
		println("onNewUser - userId: " + userId);
		println("  start pose detection");

		if(autoCalib)
			context.requestCalibrationSkeleton(userId,true);
		else    
			context.startPoseDetection("Psi",userId);
	}

	public void onLostUser(int userId)
	{
		println("onLostUser - userId: " + userId);
	}

	public void onExitUser(int userId)
	{
		println("onExitUser - userId: " + userId);
	}

	public void onReEnterUser(int userId)
	{
		println("onReEnterUser - userId: " + userId);
	}

	public void onStartCalibration(int userId)
	{
		println("onStartCalibration - userId: " + userId);
	}

	public void onEndCalibration(int userId, boolean successfull)
	{
		println("onEndCalibration - userId: " + userId + ", successfull: " + successfull);

		if (successfull) 
		{ 
			println("  User calibrated ");
			context.startTrackingSkeleton(userId); 
		} 
		else 
		{ 
			println("  Failed to calibrate user !!!");
			println("  Start pose detection");
			context.startPoseDetection("Psi",userId);
		}
	}

	public void onStartPose(String pose,int userId)
	{
		println("onStartPose - userId: " + userId + ", pose: " + pose);
		println(" stop pose detection");

		context.stopPoseDetection(userId); 
		context.requestCalibrationSkeleton(userId, true);

	}

	public void onEndPose(String pose,int userId)
	{
		println("onEndPose - userId: " + userId + ", pose: " + pose);
	}
	public int width(){
//		return 500;
		return context.depthWidth();
	}
	public int height(){
//		return 500;
		return context.depthHeight();
	}
}