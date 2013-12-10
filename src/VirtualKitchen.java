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
	int potposition = 0;
	int knifeposition = 0;
	int chopcounter = 0;
	int knifepause = 0;
	boolean knifeoncounter = false;
	
	PApplet screen;
	Sidebar sidebar;
	int height;
	int width;

	VirtualKitchen(PApplet p, Sidebar s){
		screen = p;
		sidebar = s;
		potx = 100;
		poty = 300;
		lidx = 300;
		lidy = 300;
		spoonx = 200;
		spoony = 200;
		knifex = 400;
		knifey = 400;
		screen.textSize(28);
		screen.textAlign(screen.CENTER);
	}
	public void draw(){
		PVector projLeftHand = new PVector(-1,-1,-1);
		PVector projRightHand = new PVector(screen.mouseX,screen.mouseY, 0);
		drawContext(0, projLeftHand, projRightHand, screen.width, screen.height);
	}
	void drawContext(int i, PVector left, PVector right, int w, int h)
	{
		screen.pushMatrix();
		width = w;
		height = h;
		
		screen.background(0, 255, 0);
		screen.noStroke();
		
		stove();
		counter();
		light();
		chicken(50, 50);

		screen.fill(0);
		String title = "Virtual Kitchen 2.1";
		screen.text(title, width/2, height - 50);

		if (holdPot)
		{
			if (rightHand)
				pot(right.x+40, right.y);
			else
				pot(left.x+40, left.y);

			if (lidOnPot)
			 {
			 if (rightHand)
			 lid(right.x+40+50+potposition, right.y-10);
			 else
			 lid(left.x+40+50+potposition, left.y-10);
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
				pickup();
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
				pickup();
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
				pickup();
			}
			else if (left.x > spoonx && left.x < spoonx+10 && left.y > spoony && left.y < spoony + 50 &&
					!somethingInHand && pause > pauseTimer)
			{
				rightHand = false;
				holdSpoon = true;
				somethingInHand = true;
				pause = 0;
				heldObject = "spoon";
				pickup();
			}
		}

		if (holdKnife)
		{
			if (rightHand)
				 knife(right.x-20, right.y-5);
			else
				 knife(left.x-20, left.y-5);
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
				pickup();
			}
			else if (left.x > knifex && left.x < knifex+40 && left.y > knifey && left.y < knifey + 10 &&
					!somethingInHand && pause > pauseTimer)
			{
				rightHand = false;
				holdKnife = true;
				somethingInHand = true;
				pause = 0;
				heldObject = "knife";
				pickup();
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
				pickup();
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
				pickup();
			}
		}

		pause++;

		if (pause > pauseTimer && somethingInHand)
		{
			dropObject();
		}

		location = "screen";
		//set objects down on mouseclick
		if(screen.mousePressed && somethingInHand)
		{
			holdPot = false;
			holdLid = false;
			holdSpoon = false;
			holdKnife = false;
			somethingInHand = false;
			pause = 0;
			drop();
		}
		
		screen.popMatrix();
	}
	void pickup(){
		PApplet.println("	ACTION: Pick up "+heldObject);
		sidebar.update("ACTION", "Pick up "+heldObject);
		sidebar.clear("GESTURE");
	}
	void drop(){
		PApplet.println("	ACTION: Set down "+heldObject+" on "+location);
		sidebar.update("ACTION", "Set down "+heldObject+" on "+location);
		sidebar.clear("GESTURE");
	}
	void gesture(String g){
		PApplet.println("	GESTURE: "+g);
		sidebar.clear("ACTION");
		sidebar.update("GESTURE", g);
	}
	void dropObject()
	{

	  if ((object[0] >= 0 && object[0] <= 200) || (object[0] >= width - 150 && object[0] <= width))
	  {
	    if (object[0] >= 0 && object[0] <= 200)
	      location = "counter";
	    if (object[0] >= width - 150 && object[0] <= width)
	      location = "stove";

	    if (holdKnife)
	    {
	      //if the knife touches the top of the counter - start chopping gesture
	      if (!knifeoncounter && object[1] >= 2*height/3 && object[1] <= 2*height/3 + 10)
	      {
	        chopcounter++;
	        knifeoncounter = true;
	        if (chopcounter >= 5)
	        {
	          holdKnife = false;
	          somethingInHand = false;
	          knifeoncounter = false;
	          chopcounter = 0;
	         gesture("Chop something with " + heldObject + " on " + location);
	          pause = 0;
	        }
	      }
	      //bring the knife back up above the counter
	      else if (object[1] <= 2*height/3 && knifeoncounter)
	      {
	        knifeoncounter = false;
	      }
	      else if (object[1] >= 2*height/3 + 20)
	      {
	        //drop knife on top of counter/stove by dragging it past the top
	        //instead of chopping, go further down the screen to let go of it
	        knife(knifex, 2*height/3-30);
	        holdKnife = false;
	        somethingInHand = false;
	        chopcounter = 0;
	        pause = 0;
	        drop();
	      }
	    }
	    else if (object[1] >= 2*height/3 && object[1] <= 2*height/3 + 20)
	    {
	      holdPot = false;
	      holdSpoon = false;
	      holdLid = false;
	      holdKnife = false;
	      somethingInHand = false;
	      drop();

	      pause = 0;
	    }

	    //put lid on pot
	    if (holdLid)
	    {
	      if (object[0] > potx+40+potposition && object[0] < potx+60+potposition &&
	        object[1] > poty-5 && object[1] < poty+5)
	      {
	        holdLid = false;
	        lidOnPot = true;
	        somethingInHand = false;
	        pause = 0;
	        lid(potx+50+potposition, poty-10);
	        gesture("Cover Pot");
	      }
	    }
	  }
	  else
	  {
	    chopcounter = 0;
	  }
	}
	
	void knife(float x, float y)
	{
		knifex = x;
		knifey = y;
		screen.fill(0);
		screen.rect(x, y, 40, 10);
		if (x < width/2)
			knifeposition = -40;
		else
			knifeposition = 0;
		screen.triangle(x+40+knifeposition, y, x+120+knifeposition*5, y, x+40+knifeposition, y+30);
	}

	float[] getBottomOfKnife()
	{
		float[] temp = {
				knifex+40+knifeposition, knifey+30
		};
		return temp;
	}

	void lid(float x, float y)
	{
		lidx = x;
		lidy = y;
		screen.fill(0);
		screen.arc(x, y+10, 100, 40, screen.PI, screen.TWO_PI);
		screen.ellipse(x, y-10, 20, 20);
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
	 if (x < width/2)
	 potposition = -175;
	 else
	 potposition = 0;
	 potx = x;
	 poty = y;
	 screen.fill(0);
	 //main part of pot
	 screen.rect(x+potposition, y, 100, 50);
	 //bottom of pot
	 screen.ellipse(x+50+potposition, y+50, 100, 20);
	 //handle
	 screen.rect(x, y, -75, 10);
	 screen.fill(200);
	 //top of pot
	 screen.ellipse(x+50+potposition, y, 100, 20);
	}

	float[] getBottomOfPot()
	{
	 float[] temp = {
	 potx+50+potposition, poty+60
	 };
	 return temp;
	}

	void spoon(float x, float y)
	{
		spoonx = x;
		spoony = y;
		screen.fill(0);
		screen.rect(x, y, 10, 50);
		screen.ellipse(x+5, y+50, 20, 30);
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
		screen.fill(150);
		screen.textSize(16);
		screen.rect(width, height, -150, -height/3);
		screen.fill(0);
		screen.text("Stove", width-75, height-height/3/2);

		screen.fill(255, 0, 0);
		screen.beginShape();
		screen.vertex(width-75, height-height/3);
		screen.vertex(width-60, height-height/3-5);
		screen.vertex(width-55, height-height/3-30);
		screen.vertex(width-67, height-height/3-15);
		screen.vertex(width-75, height-height/3-30);
		screen.vertex(width-82, height-height/3-15);
		screen.vertex(width-95, height-height/3-30);
		screen.vertex(width-90, height-height/3-5);
		screen.endShape(screen.CLOSE);
		screen.textSize(32);
	}

	void counter()
	{
		screen.fill(200);
		screen.textSize(16);
		screen.rect(0, height, 200, -height/3);
		screen.fill(0);
		screen.text("Counter", 100, height-height/3/2);

		screen.textSize(32);
	}

	void light()
	{
		screen.fill(0);
		screen.rect(width/2-5, 0, 10, 100);
		screen.fill(255, 255, 0);
		screen.ellipse(width/2, 100, 30, 30);

		screen.fill(0);
		screen.arc(width/2, 100, 120, 50, screen.PI, screen.TWO_PI);
	}

	void chicken(float x, float y)
	{
		screen.pushMatrix();
		screen.scale((float) .5);
		screen.translate(x, y);

		screen.noFill();
		screen.stroke(0);
		screen.strokeWeight(10);
		screen.rect(-20, -20, 130, 130);

		screen.noStroke();

		screen.fill(255, 197, 3);
		screen.ellipse(70, 10, 45, 45); // head
		screen.ellipse(25, 32, 70, 70); // body
		screen.fill(0);
		screen.ellipse(73, 7, 12, 12); // left eye
		screen.fill(255, 95, 3);
		screen.triangle(90, 10, 110, 23, 85, 26);
		screen.rect(20, 65, 8, 35); // left leg
		screen.rect(30, 65, 6, 30); // right leg
		screen.fill(255, 175, 0);
		screen.rect(20, 10, 40, 40); // left arm
		screen.strokeWeight(2);
		screen.popMatrix();
	}

}
