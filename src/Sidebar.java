
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import processing.core.PApplet;


public class Sidebar {
	int width = 250;
	int textHeight = 45;
	int topBuffer = 15;
	
	private PApplet screen;
	private Vector<String> content;
	private Map<String, String> data;
	

	Sidebar(PApplet p){
		screen = p;
		content = new Vector<String>();
		data = new HashMap<String, String>();
		screen.resize(screen.width+this.width, screen.height);
	}
	void update(String key, String value){
		if (!data.containsKey(key)){
			content.add(key);
		}
		data.put(key, value);
	}
	void clear(String key){
		content.remove(key);
		data.remove(key);
	}
	void draw(){
		screen.pushStyle();
		screen.pushMatrix();
		
		screen.textSize(12);
		screen.textAlign(PApplet.LEFT);
		screen.fill(0,0,100);
		
		screen.stroke(0,0,0);
		screen.strokeWeight(2);
		screen.rect(screen.width-this.width, 0, screen.width, screen.height);
		screen.fill(255);
		for (int i=0;i<content.size();i++)
			screen.text(content.get(i)+": "+data.get(content.get(i)).toString(), 
					screen.width-this.width+5, topBuffer+(i*textHeight), width, textHeight);
		screen.popMatrix();
		screen.popStyle();
	}
}
