package controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import processing.core.PVector;

/**
 * Interface specifying that implementing can save and load to/from xml format.
 * Contains an abstract helper class with static functions cause laziness.
 * 
 * @author Levi Lindsley
 *
 */
public interface xmlGestureParser<E> {
	public abstract class xmlStatics implements xmlGestureParser<Object>{
		/**
		 * Creates a basic text element in xml format with beginning and end tags.
		 * @param tag : tag to open and close
		 * @param text : element body
		 * @return
		 * 		xml element in the form &lttag&gt text &lt/tag&gt
		 */
		public static String createElement(String tag, String text){
			String element = new String();
			element += "<"+tag+">"+'\n';
			element += text+'\n';
			element += "</"+tag+">"+'\n';
			return element;
		}
		public static String createPVectorElem(String tag, String text, PVector e){
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
		public static String parseElement(Scanner xmlInput){
			xmlInput.next();
			String val = xmlInput.next();
			xmlInput.next();
			return val;
		}

		public static void write(BufferedWriter wr, String s){
			try {
				wr.write(s);
				wr.flush();
			} catch (IOException e) {
				System.out.println("IOException: "+e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	//TODO add load functions
	
	E load(Scanner xmlInput) throws UnsupportedOperationException;
	
	void save(String fileName, List<E> e);
	
	void save(String fileName);
	
	String toXML();
}
