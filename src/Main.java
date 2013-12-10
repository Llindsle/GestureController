import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.List;
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
	
	/** Active SimpleOpenNI instance that is used to interact with kinect*/
	SimpleOpenNI  context;
	
	//Various flags
	boolean autoCalib=true;
	boolean debug = true;
	boolean Recording = false;
	boolean playback = false;
	boolean unitMode = false;
	boolean selectorActive = true;
	boolean kitchenActive = false;
	boolean ghost = true;
	
	/**Compression type used to compress gesture into different types to 
	 * view values and differences see {@link CompressionType}*/
	int compressionMask = 0xf;

	//
	//GestureController and friends
	//
	/**Vector of gesture controllers that hold all gestures to track*/
	Vector<GestureController> gesture;
	
	/**A gesture recorder instance, used for direct recording or parsing
	 * the JointRecorder*/
	GestureRecord log;
	
	/**A JointRecorder used as the primary recording method, also used
	 * for playback and drawn skeletons may be color coded depending on
	 * what is tracked by this.*/
	JointRecorder jR;

	//instances of other things that may draw on a PApplet
	VirtualKitchen vK;
	Sidebar s;
	SkeletalSelector skelSel;

 
	public void setup()
	{	
		context = new SimpleOpenNI(this);
		
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
		
		//create VirtualKitchen
		vK = new VirtualKitchen(this,s);
		skelSel = new SkeletalSelector(this, new Sidebar(this), width(), height());
		activateSelector();
		
		System.out.println("Setup complete");
	}
	public void draw()
	{
		//if the skeleton selector is active it gets priority
		//and is the only thing drawn so it is in focus and on top.
		if (selectorActive){
			skelSel.draw();
			return;
		}
		
		// update the cam
		context.update();
		// draw depthImageMap
		image(context.depthImage(),0,0);
		
		//if unitMode is active then draw whatever it wants
		//as opposed to the standard screen image.
		if (unitMode){
			unitDraw();
			return;
		}

		// draw the skeleton if it's available
		int[] userList = context.getUsers();
		for(int i=0;i<userList.length;i++)
		{
			if(context.isTrackingSkeleton(userList[i])){
				
				//get the things the virtualKitchen needs
				PVector leftHand = new PVector(), projLeftHand = new PVector();
				PVector rightHand = new PVector(), projRightHand = new PVector();
				context.getJointPositionSkeleton(userList[i], SimpleOpenNI.SKEL_LEFT_HAND, leftHand);
				context.getJointPositionSkeleton(userList[i], SimpleOpenNI.SKEL_RIGHT_HAND, rightHand);

				context.convertRealWorldToProjective(rightHand, projRightHand);
				context.convertRealWorldToProjective(leftHand, projLeftHand);

				if (kitchenActive)
					vK.drawContext(userList[i], projLeftHand, projRightHand,width(), height());

				if(playback){
					viewRecord();
					for (int j=0;j<gesture.size();j++)
						if (gesture.get(j).isComplete(jR, jR.getPlayBackTick())){
							System.out.println("R:"+gesture.get(j).Name);
						}
					if (ghost)
						drawSkeletonPrime(userList[i]);
				}
				 else
					  drawSkeletonPrime(userList[i]);

				//check the gesture for completion
				for (int j=0;j<gesture.size();j++)
					if (gesture.get(j).isComplete(context, userList[i])){
						System.out.println(gesture.get(j).Name);
						s.update("Completed Gesture", gesture.get(j).Name+'\n'+"At: "+System.currentTimeMillis()+"");
					}
				if (Recording){
					jR.record(context, userList[i]);
				}
			}
		}    
		drawSidebar();
	}
	void initHelp(){
		
	}
	private void drawSidebar(){
		s.update("Tracking Gestures", gesture.size()+"");
		s.update("jR", jR.toString());
		s.draw();
	}
	private void activateSelector(){
		jR.getJoints(skelSel.selected);
		skelSel.selectedPair.addAll(log.getFocus());
		jR.clearFocus();
		log = new GestureRecord();
		
		selectorActive = true;
	}
	private void deactivateSelector(){
		jR.addAll(skelSel.selected);
		
		Iterator<Pair> iter = skelSel.selectedPair.iterator();
		while (iter.hasNext()){
			Pair linePair = iter.next();
			log.addFocusJoints(linePair.getFirst(), linePair.getSecond());
		}
		
		selectorActive = false;
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
		if (log.focusSize() == 0)
			drawSkeleton(jR.playBack());
		else
			drawSkeleton(jR.playBack(log.getFocus()));
	}
	void drawSkeleton(List<PVector[]> points){
		if (points == null){
			togglePlayBack();
			return;
		}
		for (PVector[] V : points){
			pushStyle();
			stroke(255,255,255);
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
		
		//get joint data from context and convert to projection
		context.getJointPositionSkeleton(user, First, Joint);
		context.convertRealWorldToProjective(Joint, R1);
		context.getJointPositionSkeleton(user, Second, Joint);
		context.convertRealWorldToProjective(Joint, R2);
		
		pushStyle();
		
		//draw line between joints
		if (log.contains(new Pair(First, Second)))
			stroke(255,0,0);
		else
			stroke(0,0,255);
		line(R1.x, R1.y, R2.x, R2.y);
		popStyle();
		
		//draw joint
		if (jR.contains(Second)){
			fill(255,0,0);
			stroke(255,0,0);
		}
		else{
			fill(0);
			stroke(0);
		}
		ellipse(R2.x, R2.y, skelSel.jointSize, skelSel.jointSize);


	}
	//draw a colored skeleton with visible joints
	void drawSkeletonPrime(int userId){
		strokeWeight(2);
		stroke(0);
		drawLimb(userId, Skeleton.NECK.get(), Skeleton.HEAD.get());
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
	
	public void mousePressed(){
		if (selectorActive){
			skelSel.mousePressed();
			return;
		}
	}
	public void mouseClicked(){
		if (selectorActive){
			if(!skelSel.mouseClicked()){
				deactivateSelector();
			}
			return;
		}
	}
	public void mouseReleased(){
		if (selectorActive){
			skelSel.mouseReleased();
			return;
		}
	}
	public void keyPressed(){
		if (key == 8){ //backspace
			gesture.clear();
		}
		if (key == 'g'){
			ghost = !ghost;
		}
		if (key == 'k'){
			kitchenActive = !kitchenActive;
		}
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
				ObjectInputStream oin = new ObjectInputStream(fos);
				gesture = (Vector<GestureController>) oin.readObject();
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