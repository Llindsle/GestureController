package controller;


public class Tst {
	void run(){
		GestureController tG = testGesture();
		System.out.println(tG);
	}
	GestureController testGesture(){
		GestureController gC = new GestureController();
		Pair p = new Pair(0,1);
		for (int i=0;i<5;i++)
			gC.addPoint(p, new Euclidean(1,1,1), new Euclidean(0,i+2,0));
		return gC;
	}
	public static void main(String args[]){
		new Tst().run();
	}
}
