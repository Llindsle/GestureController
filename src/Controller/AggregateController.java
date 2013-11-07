package controller;

import java.util.List;

/**
 * Objective: create a class that aggregates a list of gesture controllers
 * that represent the same gesture, use k-nearest neighbor to see how well a 
 * new gesture fits in this aggregate gesture. 
 * 
 * 1) Aggregate n gestures into a node
 * 2) Check gestures against nodes and determine 'distance' from the node
 * 3) For the nodes within k distance check how well gesture fits within node
 * 4) Place gesture within best fitting node
 * 		- May place a constraint on the best fit to throw or place into new
 * 			node gestures that are not close to any node.
 * 
 * This class is going to be a pain to get to work correctly within usable
 * limits but is the one that will allow the controllers to 'learn' if it 
 * works as intended. 
 * 
 * @author Levi Lindsley
 *
 */
public class AggregateController extends GestureController {
	
		/*Aggregate control into a standard/average/normalized gesture.
		 * This will be used to determine 'distance' for the k-nearest
		 * neighbor algorithm that will trigger bestFit() */
		public void aggregate(List<GestureController> control){
			
		}
		/*
		 * Estimate the distance from test to this
		 */
		public int distance(GestureController test){
			return -1;
		}
		/*Determines how well test fits into this. The aggregate node 
		 * that best fits test will have test placed into it.*/
		public int bestFit(GestureController test){
			return -1;
		}
}
