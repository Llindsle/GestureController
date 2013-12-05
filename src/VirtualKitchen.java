import processing.core.PApplet;
import processing.core.*;


public class VirtualKitchen {

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
PApplet parent;
	
	VirtualKitchen(PApplet P){
		parent = P;
		setup();
	}
	public void setup(){
//		  size(context.depthWidth(), context.depthHeight());
		parent.size(500,500);
		  potx = 100;
		  poty = 300;
		  lidx = 300;
		  lidy = 300;
		  spoonx = 200;
		  spoony = 200;
		  knifex = 400;
		  knifey = 400;
		  parent.textSize(28);
		  parent.textAlign(parent.CENTER);
	}
	public void draw(){
		PVector projLeftHand = new PVector(-1,-1,-1);
		PVector projRightHand = new PVector(parent.mouseX,parent.mouseY, 0);
		int i=0;
		drawContext(i, projLeftHand, projRightHand);
//		println("?");
	}
	void drawContext(int i, PVector left, PVector right)
	{
	  parent.background(0, 255, 0);

	  stove();
	  counter();
	  light();
	  chicken(50, 50);

	  parent.fill(0);
	  String title = "Virtual Kitchen 2.1";
	  parent.text(title, parent.width/2, parent.height - 50);

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
	      parent.println("  ACTION: Pick up " + heldObject);
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
	      parent.println("  ACTION: Pick up " + heldObject);
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
	      parent.println("  ACTION: Pick up " + heldObject);
	    }
	    else if (left.x > spoonx && left.x < spoonx+10 && left.y > spoony && left.y < spoony + 50 &&
	      !somethingInHand && pause > pauseTimer)
	    {
	      rightHand = false;
	      holdSpoon = true;
	      somethingInHand = true;
	      pause = 0;
	      heldObject = "spoon";
	      parent.println("  ACTION: Pick up " + heldObject);
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
	      parent.println("  ACTION: Pick up " + heldObject);
	    }
	    else if (left.x > knifex && left.x < knifex+40 && left.y > knifey && left.y < knifey + 10 &&
	      !somethingInHand && pause > pauseTimer)
	    {
	      rightHand = false;
	      holdKnife = true;
	      somethingInHand = true;
	      pause = 0;
	      heldObject = "knife";
	      parent.println("  ACTION: Pick up " + heldObject);
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
	      parent.println("  ACTION: Pick up " + heldObject);
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
	      parent.println("  ACTION: Pick up " + heldObject);
	    }
	  }

	  pause++;

	  if (pause > pauseTimer && somethingInHand)
	  {
	    dropObject();
	  }

	  location = "screen";
	  //set objects down on mouseclick
	  if(parent.mousePressed && somethingInHand)
	  {
	    holdPot = false;
	    holdLid = false;
	    holdSpoon = false;
	    holdKnife = false;
	    somethingInHand = false;
	    pause = 0;
	    parent.println("  ACTION: Set down " + heldObject + " on " + location);
	  }
	}

	void dropObject()
	{
	  if ((object[0] >= 0 && object[0] <= 200) || (object[0] >= parent.width - 150 && object[0] <= parent.width))
	  {
	    if (object[0] >= 0 && object[0] <= 200)
	      location = "counter";
	    if (object[0] >= parent.width - 150 && object[0] <= parent.width)
	      location = "stove";

	    if (object[1] >= 2*parent.height/3 && object[1] <= 2*parent.height/3 + 10)
	    {
	      holdPot = false;
	      holdSpoon = false;
	      holdLid = false;
	      holdKnife = false;
	      somethingInHand = false;
	      parent.println("  ACTION: Set down " + heldObject + " on " + location);

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
	        parent.println("GESTURE: Cover Pot");
	      }
	    }
	  }
	}

	void knife(float x, float y)
	{
	  knifex = x;
	  knifey = y;
	  parent.fill(0);
	  parent.rect(x, y, 40, 10);
	  parent.triangle(x+40, y, x+120, y, x+40, y+30);
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
	  parent.fill(0);
	  parent.arc(x, y+10, 100, 40, parent.PI, parent.TWO_PI);
	  parent.ellipse(x, y-10, 20, 20);
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
	  parent.fill(0);
	  //main part of pot
	  parent.rect(x, y, 100, 50);
	  //bottom of pot
	  parent.ellipse(x+50, y+50, 100, 20);
	  //handle
	  parent.quad(x, y, x, y+10, x-75, y+10, x-75, y);
	  parent.fill(200);
	  //top of pot
	  parent.ellipse(x+50, y, 100, 20);
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
	  parent.fill(0);
	  parent.rect(x, y, 10, 50);
	  parent.ellipse(x+5, y+50, 20, 30);
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
	  parent.fill(150);
	  parent.textSize(16);
	  parent.rect(parent.width, parent.height, -150, -parent.height/3);
	  parent.fill(0);
	  parent.text("Stove", parent.width-75, parent.height-parent.height/3/2);

	  parent.fill(255, 0, 0);
	  parent.beginShape();
	  parent.vertex(parent.width-75, parent.height-parent.height/3);
	  parent.vertex(parent.width-60, parent.height-parent.height/3-5);
	  parent.vertex(parent.width-55, parent.height-parent.height/3-30);
	  parent.vertex(parent.width-67, parent.height-parent.height/3-15);
	  parent.vertex(parent.width-75, parent.height-parent.height/3-30);
	  parent.vertex(parent.width-82, parent.height-parent.height/3-15);
	  parent.vertex(parent.width-95, parent.height-parent.height/3-30);
	  parent.vertex(parent.width-90, parent.height-parent.height/3-5);
	  parent.endShape(parent.CLOSE);
	  parent.textSize(32);
	}

	void counter()
	{
	  parent.fill(200);
	  parent.textSize(16);
	  parent.rect(0, parent.height, 200, -parent.height/3);
	  parent.fill(0);
	  parent.text("Counter", 100, parent.height-parent.height/3/2);

	  parent.textSize(32);
	}

	void light()
	{
		parent.fill(0);
		parent.rect(parent.width/2-5, 0, 10, 100);
		parent.fill(255, 255, 0);
		parent.ellipse(parent.width/2, 100, 30, 30);

		parent.fill(0);
		parent.arc(parent.width/2, 100, 120, 50, parent.PI, parent.TWO_PI);
	}

	void chicken(float x, float y)
	{
		parent.pushMatrix();
		parent.scale((float) .5);
		parent.translate(x, y);

		parent.noFill();
		parent.stroke(0);
	  parent.strokeWeight(10);
	  parent.rect(-20, -20, 130, 130);

	  parent.noStroke();

	  parent.fill(255, 197, 3);
	  parent.ellipse(70, 10, 45, 45); // head
	  parent.ellipse(25, 32, 70, 70); // body
	  parent.fill(0);
	  parent.ellipse(73, 7, 12, 12); // left eye
	  parent.fill(255, 95, 3);
	  parent.triangle(90, 10, 110, 23, 85, 26);
	  parent.rect(20, 65, 8, 35); // left leg
	  parent.rect(30, 65, 6, 30); // right leg
	  parent. fill(255, 175, 0);
	  parent.rect(20, 10, 40, 40); // left arm
	  parent.strokeWeight(2);
	  parent.popMatrix();
	}
	public static void main(String[] args){
		PApplet.main(new String[] { "--present", "VirtualKitchen" });
	}
}
