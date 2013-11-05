package Controller;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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
		String tb = new String();
		tb += '\t';
		
		content +="<?xml version=\"1.0\"?>"+'\n';
		content +="<root>"+'\n';
		content += createElement("epsilon",tb,GestureController.getTolerance().toString());
		write(wr,content);
		
		for(GestureController c : g){
			write(wr,c.toXML(tb));
		}
		
		content ="</root>";
		write(wr,content);

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
		String tb = new String();
		tb += '\t';
		
		content +="<?xml version=\"1.0\"?>"+'\n';
		content +="<root>"+'\n';
		content +=tb+"<epsilon>"+GestureController.getTolerance()+"</epsilon>";
		content += g.toXML(tb);
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
	protected static String createElement(String tag,String indent, String text){
		if (indent == null)
			indent = "";
		
		String element = new String();
		element += indent+"<"+tag+">"+'\n';
		element += indent+text+'\n';
		element += indent+"</"+tag+">"+'\n';
		return element;
	}
}
