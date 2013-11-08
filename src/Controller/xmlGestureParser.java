package controller;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import processing.core.PVector;

/**
 * Class to help save and load instances of other classes to an xml format
 * there may be a way to make java do this but I don't know it so I wrote 
 * this.
 * 
 * @author Levi Lindsley
 *
 */
public class xmlGestureParser {
	@SuppressWarnings("unused")
	private static boolean debug = true;

	//TODO add save function for JointRecorder
	//TODO add load function for GestureController & JointRecorder
	public static void save(String fileName, JointRecorder jR){
		BufferedWriter wr;
		try {
			wr = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			System.out.println("IOException: "+e.getMessage());
			e.printStackTrace();
			return;
		}
		String content = new String();
		
		content +="<?xml version=\"1.0\"?>"+'\n';
		content +="<root>"+'\n';
		content +=jR.toXML();
		content +="</root>"+'\n';
		write(wr, content);
		
	}
	public static void save(String fileName, List<GestureController> g){
		BufferedWriter wr;
		try {
			wr = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			System.out.println("IOException: "+e.getMessage());
			e.printStackTrace();
			return;
		}
		String content = new String();
		
		content +="<?xml version=\"1.0\"?>"+'\n';
		content +="<root>"+'\n';
		content += createElement("epsilon",GestureController.getTolerance().toString());
		write(wr,content);
		
		for(GestureController c : g){
			write(wr,c.toXML());
		}
		
		write(wr,"</root>");

	}
	public static void save(String fileName, GestureController g){
		BufferedWriter wr;
		try {
			wr = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			System.out.println("IOException: "+e.getMessage());
			e.printStackTrace();
			return;
		}
		
		String content = new String();
		
		content +="<?xml version=\"1.0\"?>"+'\n';
		content +="<root>"+'\n';
		content +="<epsilon>"+GestureController.getTolerance()+"</epsilon>";
		content += g.toXML();
		content +="</root>"; 
		write(wr,content);
	}
	
	private static void write(BufferedWriter wr, String s){
		try {
			wr.write(s);
			wr.flush();
		} catch (IOException e) {
			System.out.println("IOException: "+e.getMessage());
			e.printStackTrace();
		}
	}
	/**
	 * Creates a basic text element in xml format with beginning and end tags.
	 * @param tag : tag to open and close
	 * @param text : element body
	 * @return
	 * 		xml element in the form &lttag&gt text &lt/tag&gt
	 */
	protected static String createElement(String tag, String text){
		String element = new String();
		element += "<"+tag+">"+'\n';
		element += text+'\n';
		element += "</"+tag+">"+'\n';
		return element;
	}
	protected static String createPVectorElem(String tag, String text, PVector e){
		String elem = new String();
		elem += "<"+tag+">"+'\n';
		elem += text;
		Float val = e.x;
		elem += createElement("x", val.toString());
		val = e.y;
		elem += createElement("y", val.toString());
		val = e.z;
		elem += createElement("z", val.toString());
		elem += "</"+tag+">"+'\n';
		return elem;
	}
}
