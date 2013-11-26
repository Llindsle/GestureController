import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
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
	boolean       autoCalib=true;
	boolean debug = true;
	boolean Recording = false;
	boolean compression = false;
	boolean playback = false;
	boolean unitMode = false;
	
	Vector<GestureController> gesture;
	GestureRecord log;
	JointRecorder jR;
	int color = 0;

	public void setup()
	{	
	  context = new SimpleOpenNI(this);
	  gesture = new Vector<GestureController>();
	  log = new GestureRecord();
	  jR = new JointRecorder();
	  jR.addJoint(SimpleOpenNI.SKEL_LEFT_ELBOW);
	  jR.addJoint(SimpleOpenNI.SKEL_LEFT_HAND);
	  jR.addJoint(SimpleOpenNI.SKEL_LEFT_SHOULDER);
//	  jR.addJoint(SimpleOpenNI.SKEL_RIGHT_HAND);
//	  jR.addJoint(SimpleOpenNI.SKEL_RIGHT_ELBOW);
	  jR.addAll();
	   
//	  gesture.add(new GestureController("Wave"));
//	  createWaveGesture(gesture.lastElement());
	  
	   
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

	  stroke(255,0,0);
	  strokeWeight(2);
	  smooth();
	 
	  size(context.depthWidth(), context.depthHeight()); 
	  System.out.println("Setup complete");
	}
	public void draw()
	{
	  // update the cam
	  context.update();
	  
	  if (unitMode){
		  unitDraw();
		  return;
	  }
	  // draw depthImageMap
	  image(context.depthImage(),0,0);
	  
	  stroke (255,0,color);
	  color = (color+1)%255;
	  // draw the skeleton if it's available
	  int[] userList = context.getUsers();
	  for(int i=0;i<userList.length;i++)
	  {
	    if(context.isTrackingSkeleton(userList[i])){
	  	  if(playback){
			  viewRecord();
		      for (int j=0;j<gesture.size();j++)
		    	  if (gesture.get(j).isComplete(jR, jR.getPlayBackTick())){
		    		  System.out.println(gesture.get(j).Name);
		    	  }
		     return;
		  }
	  	  else
	  		  drawSkeleton(userList[i]);
	      
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
		    		  doodle.get(0).mult(-1000);
		    		  boringShape(doodle.get(0),j);
		    		  
		    		  stroke(0,255,0);
		    		  doodle.get(1).mult(-1000);
		    		  boringShape(doodle.get(1),j);
		    		  
		    		  stroke(0,0,255);
		    		  doodle.get(2).mult(-1000);
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
			jR.resetPlayBack();
			System.out.print("dis");
		}
		System.out.println("engaged");
	}
	void viewRecord(){
		drawSkeleton(jR.playBack());
	}
	void drawSkeleton(List<PVector[]> points){
		if (points == null){
			togglePlayBack();
			return;
		}
		for (PVector[] V : points){
			//not the best use as that third vector got the axe but I think that
			//is how simpleOpenNI draws
			line (V[0].x, V[0].y, V[1].x, V[1].y);
		}
	}
	void printSkeletalConst(){
		System.out.println("Head: "+SimpleOpenNI.SKEL_HEAD);
		System.out.println("Neck: "+SimpleOpenNI.SKEL_NECK);
		System.out.println("L Shoulder: "+SimpleOpenNI.SKEL_LEFT_SHOULDER);
		System.out.println("R Shoulder: "+SimpleOpenNI.SKEL_RIGHT_SHOULDER);
		System.out.println("L Elbow: "+SimpleOpenNI.SKEL_LEFT_ELBOW);
		System.out.println("R Elbow: "+SimpleOpenNI.SKEL_RIGHT_ELBOW);
		System.out.println("L Hand: "+SimpleOpenNI.SKEL_LEFT_HAND);
		System.out.println("R Hand: "+SimpleOpenNI.SKEL_RIGHT_HAND);
		System.out.println("Torso: "+SimpleOpenNI.SKEL_TORSO);
		System.out.println("L Hip: "+SimpleOpenNI.SKEL_LEFT_HIP);
		System.out.println("R Hip: "+SimpleOpenNI.SKEL_RIGHT_HIP);
		System.out.println("L Knee: "+SimpleOpenNI.SKEL_LEFT_KNEE);
		System.out.println("R Knee: "+SimpleOpenNI.SKEL_RIGHT_KNEE);
		System.out.println("L Foot: "+SimpleOpenNI.SKEL_LEFT_FOOT);
		System.out.println("R Foot: "+SimpleOpenNI.SKEL_RIGHT_FOOT);
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
			if (Recording){
				System.out.println("Recording End");
				Recording = false;
			}
			else if (!Recording){
				if (jR.isEmpty()){
					System.out.println("Recording Start");
					Recording = true;
				}
				else{
					GestureController g;
//					
					log.record(jR);
//					System.out.println(log);
					g = log.generateGesture(CompressionType.NONE);
					log.clear();
					gesture.add(g);
					gesture.lastElement().Name = "Gesture "+gesture.size()+ " (generated)";
					System.out.println(g);
					System.out.println("Gesture "+gesture.size()+" generated");
					
					log.record(jR);
					g = log.generateGesture(CompressionType.SIMPLE);
					log.clear();
					gesture.add(g);
					gesture.lastElement().Name = "Gesture "+gesture.size()+ " (generated)";
					System.out.println(g);
					System.out.println("Gesture "+gesture.size()+" generated");
//					
					log.record(jR);
					g = log.generateGesture(CompressionType.AVG);
					gesture.add(g);
					log.clear();
					gesture.lastElement().Name = "Gesture "+gesture.size()+ " (generated)";
					System.out.println(gesture.lastElement());
					System.out.println("Gesture "+gesture.size()+" generated");
//					
					log.record(jR);
					g = log.generateGesture(CompressionType.DBL_AVG);
					gesture.add(g);
					gesture.lastElement().Name = "Gesture "+gesture.size()+ " (generated)";
					System.out.println(g);
					System.out.println("Gesture "+gesture.size()+" generated");
//					
//					jR.clear();
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
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
}