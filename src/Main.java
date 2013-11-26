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
	

float potx, poty, lidx, lidy, spoonx, spoony, knifex, knifey;
float[] object = new float[2];
boolean holdPot = false;
boolean holdLid = false;
boolean holdSpoon = false;
boolean holdKnife = false;
boolean lidOnPot = false;
boolean somethingInHand = false;
int pause = 0;
 int pauseTimer = 30;
String heldObject = "";
String location = "";
boolean rightHand = true;
int maxUsers = 5;


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
//	  jR.addAll();
	   
//	  gesture.add(new GestureController("Wave"));
//	  createWaveGesture(gesture.lastElement());
	  
	  log.addFocusJoints(SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);
//	  log.addFocusJoints(SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);
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

//	  stroke(255,0,0);
//	  strokeWeight(2);
	  smooth();
	 
	  size(context.depthWidth(), context.depthHeight()); 
	  potx = 100;
	  poty = 300;
	  lidx = 300;
	  lidy = 300;
	  spoonx = 200;
	  spoony = 200;
	  knifex = 400;
	  knifey = 400;
	  textSize(28);
	  textAlign(CENTER);
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
	  
//	  stroke (255,0,color);
//	  color = (color+1)%255;
	  // draw the skeleton if it's available
	  int[] userList = context.getUsers();
	  for(int i=0;i<userList.length;i++)
	  {
	    if(context.isTrackingSkeleton(userList[i])){
	        PVector leftHand = new PVector(), projLeftHand = new PVector();
	        PVector rightHand = new PVector(), projRightHand = new PVector();
	        context.getJointPositionSkeleton(i, SimpleOpenNI.SKEL_LEFT_HAND, leftHand);
	        context.getJointPositionSkeleton(i, SimpleOpenNI.SKEL_RIGHT_HAND, rightHand);

	        context.convertRealWorldToProjective(rightHand, projRightHand);
	        context.convertRealWorldToProjective(leftHand, projLeftHand);

	        drawContext(i, projLeftHand, projRightHand);
	        drawSkeleton(userList[i]);
	  	  if(playback){
			  viewRecord();
		      for (int j=0;j<gesture.size();j++)
		    	  if (gesture.get(j).isComplete(jR, jR.getPlayBackTick())){
		    		  System.out.println(gesture.get(j).Name);
		    	  }
		     return;
		  }
	  	 // else
	  		//  drawSkeleton(userList[i]);
	      
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
	void drawContext(int i, PVector left, PVector right)
	{
	  background(0, 255, 0);

	  stove();
	  counter();
	  light();
	  chicken(50, 50);

	  fill(0);
	  String title = "Virtual Kitchen 2.1";
	  text(title, width/2, height - 50);

	  if (holdPot)
	  {
	    if (rightHand)
	      pot(right.x+40, right.y);
	    else
	      pot(left.x+40, left.y);

	    if (lidOnPot)
	    {
	      if (rightHand)
	        lid(right.x+40+50, right.y-10);
	      else
	        lid(left.x+40+50, left.y-10);
	    }
	    object = getBottomOfPot();
	  }
	  else
	  {
	    pot(potx, poty);
	    if ( right.x < potx && right.x > potx-75 && right.y < poty && right.y > poty-10 &&
	      !somethingInHand && pause > pauseTimer)
	    {
	      rightHand = true;
	      holdPot = true;
	      somethingInHand = true;
	      heldObject = "pot";
	      if (lidOnPot)
	      {
	        heldObject = "covered pot";
	      }
	      println("  ACTION: Pick up " + heldObject);
	      pause = 0;
	    }
	    else if ( left.x < potx && left.x > potx-75 && left.y < poty && left.y > poty-10 &&
	      !somethingInHand && pause > pauseTimer)
	    {
	      rightHand = false;
	      holdPot = true;
	      somethingInHand = true;
	      heldObject = "pot";
	      if (lidOnPot)
	      {
	        heldObject = "covered pot";
	      }
	      println("  ACTION: Pick up " + heldObject);
	      pause = 0;
	    }
	  }

	  if (holdSpoon)
	  {
	    if (rightHand)
	      spoon(right.x, right.y);
	    else
	      spoon(left.x, left.y);
	    object = getBottomOfSpoon();
	  }
	  else
	  {
	    spoon(spoonx, spoony);
	    if (right.x > spoonx && right.x < spoonx+10 && right.y > spoony && right.y < spoony + 50 &&
	      !somethingInHand && pause > pauseTimer)
	    {
	      rightHand = true;
	      holdSpoon = true;
	      somethingInHand = true;
	      pause = 0;
	      heldObject = "spoon";
	      println("  ACTION: Pick up " + heldObject);
	    }
	    else if (left.x > spoonx && left.x < spoonx+10 && left.y > spoony && left.y < spoony + 50 &&
	      !somethingInHand && pause > pauseTimer)
	    {
	      rightHand = false;
	      holdSpoon = true;
	      somethingInHand = true;
	      pause = 0;
	      heldObject = "spoon";
	      println("  ACTION: Pick up " + heldObject);
	    }
	  }

	  if (holdKnife)
	  {
	    if (rightHand)
	      knife(right.x, right.y);
	    else
	      knife(left.x, left.y);
	    object = getBottomOfKnife();
	  }
	  else
	  {
	    knife(knifex, knifey);
	    if (right.x > knifex && right.x < knifex+40 && right.y > knifey && right.y < knifey + 10 &&
	      !somethingInHand && pause > pauseTimer)
	    {
	      rightHand = true;
	      holdKnife = true;
	      somethingInHand = true;
	      pause = 0;
	      heldObject = "knife";
	      println("  ACTION: Pick up " + heldObject);
	    }
	    else if (left.x > knifex && left.x < knifex+40 && left.y > knifey && left.y < knifey + 10 &&
	      !somethingInHand && pause > pauseTimer)
	    {
	      rightHand = false;
	      holdKnife = true;
	      somethingInHand = true;
	      pause = 0;
	      heldObject = "knife";
	      println("  ACTION: Pick up " + heldObject);
	    }
	  }

	  if (holdLid)
	  {
	    if (rightHand)
	      lid(right.x, right.y);
	    else
	      lid(left.x, left.y);
	    object = getBottomOfLid();
	  }
	  else
	  {
	    lid(lidx, lidy);
	    if (right.x > lidx-10 && right.x < lidx+10 && right.y > lidy-20 && right.y < lidy &&
	      !somethingInHand && pause > pauseTimer)
	    {
	      rightHand = true;
	      holdLid = true;
	      somethingInHand = true;
	      lidOnPot = false;
	      pause = 0;
	      heldObject = "lid";
	      println("  ACTION: Pick up " + heldObject);
	    }
	    else if (left.x > lidx-10 && left.x < lidx+10 && left.y > lidy-20 && left.y < lidy &&
	      !somethingInHand && pause > pauseTimer)
	    {
	      rightHand = false;
	      holdLid = true;
	      somethingInHand = true;
	      lidOnPot = false;
	      pause = 0;
	      heldObject = "lid";
	      println("  ACTION: Pick up " + heldObject);
	    }
	  }

	  pause++;

	  if (pause > pauseTimer && somethingInHand)
	  {
	    dropObject();
	  }

	  location = "screen";
	  //set objects down on mouseclick
	  if(mousePressed && somethingInHand)
	  {
	    holdPot = false;
	    holdLid = false;
	    holdSpoon = false;
	    holdKnife = false;
	    somethingInHand = false;
	    pause = 0;
	    println("  ACTION: Set down " + heldObject + " on " + location);
	  }
	}

	void dropObject()
	{
	  if ((object[0] >= 0 && object[0] <= 200) || (object[0] >= width - 150 && object[0] <= width))
	  {
	    if (object[0] >= 0 && object[0] <= 200)
	      location = "counter";
	    if (object[0] >= width - 150 && object[0] <= width)
	      location = "stove";

	    if (object[1] >= 2*height/3 && object[1] <= 2*height/3 + 10)
	    {
	      holdPot = false;
	      holdSpoon = false;
	      holdLid = false;
	      holdKnife = false;
	      somethingInHand = false;
	      println("  ACTION: Set down " + heldObject + " on " + location);

	      pause = 0;
	    }

	    //put lid on pot
	    if (holdLid)
	    {
	      if (object[0] > potx+40 && object[0] < potx+60 &&
	        object[1] > poty-5 && object[1] < poty+5)
	      {
	        holdLid = false;
	        lidOnPot = true;
	        somethingInHand = false;
	        pause = 0;
	        lid(potx+50, poty-10);
	        println("GESTURE: Cover Pot");
	      }
	    }
	  }
	}

	void knife(float x, float y)
	{
	  knifex = x;
	  knifey = y;
	  fill(0);
	  rect(x, y, 40, 10);
	  triangle(x+40, y, x+120, y, x+40, y+30);
	}

	float[] getBottomOfKnife()
	{
	  float[] temp = {
	    knifex+40, knifey+30
	  };
	  return temp;
	}

	void lid(float x, float y)
	{
	  lidx = x;
	  lidy = y;
	  fill(0);
	  arc(x, y+10, 100, 40, PI, TWO_PI);
	  ellipse(x, y-10, 20, 20);
	}

	float[] getBottomOfLid()
	{
	  float[] temp = {
	    lidx, lidy+10
	  };
	  return temp;
	}

	void pot(float x, float y)
	{
	  potx = x;
	  poty = y;
	  fill(0);
	  //main part of pot
	  rect(x, y, 100, 50);
	  //bottom of pot
	  ellipse(x+50, y+50, 100, 20);
	  //handle
	  quad(x, y, x, y+10, x-75, y+10, x-75, y);
	  fill(200);
	  //top of pot
	  ellipse(x+50, y, 100, 20);
	}

	float[] getBottomOfPot()
	{
	  float[] temp = {
	    potx+50, poty+60
	  };
	  return temp;
	}

	void spoon(float x, float y)
	{
	  spoonx = x;
	  spoony = y;
	  fill(0);
	  rect(x, y, 10, 50);
	  ellipse(x+5, y+50, 20, 30);
	}

	float[] getBottomOfSpoon()
	{
	  float temp[] = {
	    spoonx + 5, spoony + 65
	  };
	  return temp;
	}

	void stove()
	{
	  fill(150);
	  textSize(16);
	  rect(width, height, -150, -height/3);
	  fill(0);
	  text("Stove", width-75, height-height/3/2);

	  fill(255, 0, 0);
	  beginShape();
	  vertex(width-75, height-height/3);
	  vertex(width-60, height-height/3-5);
	  vertex(width-55, height-height/3-30);
	  vertex(width-67, height-height/3-15);
	  vertex(width-75, height-height/3-30);
	  vertex(width-82, height-height/3-15);
	  vertex(width-95, height-height/3-30);
	  vertex(width-90, height-height/3-5);
	  endShape(CLOSE);
	  textSize(32);
	}

	void counter()
	{
	  fill(200);
	  textSize(16);
	  rect(0, height, 200, -height/3);
	  fill(0);
	  text("Counter", 100, height-height/3/2);

	  textSize(32);
	}

	void light()
	{
	  fill(0);
	  rect(width/2-5, 0, 10, 100);
	  fill(255, 255, 0);
	  ellipse(width/2, 100, 30, 30);

	  fill(0);
	  arc(width/2, 100, 120, 50, PI, TWO_PI);
	}

	void chicken(float x, float y)
	{
	  pushMatrix();
	  scale((float) .5);
	  translate(x, y);

	  noFill();
	  stroke(0);
	  strokeWeight(10);
	  rect(-20, -20, 130, 130);

	  noStroke();

	  fill(255, 197, 3);
	  ellipse(70, 10, 45, 45); // head
	  ellipse(25, 32, 70, 70); // body
	  fill(0);
	  ellipse(73, 7, 12, 12); // left eye
	  fill(255, 95, 3);
	  triangle(90, 10, 110, 23, 85, 26);
	  rect(20, 65, 8, 35); // left leg
	  rect(30, 65, 6, 30); // right leg
	  fill(255, 175, 0);
	  rect(20, 10, 40, 40); // left arm
	  strokeWeight(2);
	  popMatrix();
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
//					log.record(jR);
//					g = log.generateGesture(CompressionType.AVG);
//					gesture.add(g);
//					log.clear();
//					gesture.lastElement().Name = "Gesture "+gesture.size()+ " (generated)";
//					System.out.println(gesture.lastElement());
//					System.out.println("Gesture "+gesture.size()+" generated");
//					
//					log.record(jR);
//					g = log.generateGesture(CompressionType.DBL_AVG);
//					gesture.add(g);
//					gesture.lastElement().Name = "Gesture "+gesture.size()+ " (generated)";
//					System.out.println(g);
//					System.out.println("Gesture "+gesture.size()+" generated");
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