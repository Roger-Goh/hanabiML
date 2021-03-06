package agents;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import hanabAI.Action;
import hanabAI.ActionType;
import hanabAI.Agent;
import hanabAI.Card;
import hanabAI.Colour;
import hanabAI.IllegalActionException;
import hanabAI.State;


/**
 * A simple reflex agent for playing Hanabi.
 * The agent uses the following rules:
 * - Play a card if it is definitely able to be played.
 * - Discard a card if it is definitely not required.
 * - with probability 0.1*fuse play a card that matches the colour or number of a required card,
 *   with probability 0.4 give a colour hint to the next player with a required card,
 *   with probability 0.4 give a number hint to the next player with a required card,
 *   otherwise discard a random card.
 *@author Tim French 
 **/
public class TrainerAgent implements Agent{

  private Colour[] colours;
  private int[] values;
  private boolean firstAction = true;
  private int numPlayers;
  private int index;
  
  //trainer assets
  public List<Integer> features = new ArrayList<Integer>();
  public List<Integer> labels = new ArrayList<Integer>();
  
  public List<String> masterList = new ArrayList<String>();
  public String masterListToString;
  public String labelsToString;

  /**
   * Default constructor, does nothing.
   * **/
  public TrainerAgent(){}

  /**
   * Initialises variables on the first call to do action.
   * @param s the State of the game at the first action
   **/
  public void init(State s){
    numPlayers = s.getPlayers().length;
    if(numPlayers>3){
      colours = new Colour[4];
      values = new int[4];
    }
    else{
      colours = new Colour[5];
      values = new int[5];
    }
    index = s.getNextPlayer();
    
//    //example
//    System.out.println(index);
//    features.add(index);
//    features.add(2);
//    System.out.println(features.toString());
//    masterList.add(features.toString());
//    masterList.add("[3]");
//    System.out.println(masterList.toString());
    
    firstAction = false;
  }

  /**
   * Returns the name BaseLine.
   * @return the String "BaseLine"
   * */
  public String toString(){return "Trainer";}

  public String getFeatures(){return masterListToString;}
  public String getLabels(){return labelsToString;}

  /**
   * Performs an action given a state.
   * Assumes that they are the player to move.
   * The strategy will 
   * a) play a card if a card is known to be playable,
   * b) discard a card if a card is known to be useless
   * c) give a number hint to the next player with a playable card (0.1 per hint token)
   * d) give a colour hint to the next player with a playable card (0.1 per hint token)
   * e) play a potential card (0.1 per fuse token)
   * f) discard an unknown card
   * g) discard a known card
   * @param s the current state of the game.
   * @return the action the player takes.
   **/ 
  public Action doAction(State s){
    if(firstAction){
      init(s);
    } 
    //Assume players index is sgetNextPlayer()
    index = s.getNextPlayer();
    
    //get any hints
    try{
      getHints(s);
      
      //add features of the game state
      features.clear();	//new object/fruit
      features.add(s.getOrder()); //adding feature to single object/fruit
      features.add(s.getScore()); 
      features.add(s.getFuseTokens());
      features.add(s.getHintTokens());
      //gets fireworks currently played
      String[] fireworksColours = {"BLUE","GREEN","RED","WHITE","YELLOW"};
      for(String fireworksColour:fireworksColours) {
	      Stack<Card> fireworks = s.getFirework(Colour.valueOf(fireworksColour)); //stack of blue fireworks
	      if(fireworks.isEmpty()) {
	    	  features.add(0);
	      }else {
	    	  features.add(s.getFirework(Colour.valueOf(fireworksColour)).peek().getValue()); //the value on top of the stack, 0 for no cards
	      }
      }
      //get hints of my hand, 0 if unknown
      //colour hints
      for(Colour colour:colours) {
    	 if(colour==null) {features.add(0);}else{ //we don't know the colour of this card
	    	 switch (colour.toString()){
	    	 	case "Blue": features.add(1);
				   			break; 
	    	 	case "Green": features.add(2);
	   						break; 
	    	 	case "Red": features.add(3);
	   						break; 
	    	 	case "White": features.add(4);
	   						break; 
	    	 	case "Yellow": features.add(5);
	   						break; 
	    	 	default: System.out.println("invalid");
	   						break; 
	    	 }
    	 }
     }
      //value hints
      for(int value:values) {
    	  features.add(value);
      }
      
      //teammates' hands
      for(int i=0;i<s.getPlayers().length;i++) {
    	  if(i!=index) {
    		  Card[] playerHand = s.getHand(i);
    		  //colours
    		  for(Card card:playerHand) {
    			  if(card==null) {features.add(0);}else{ //we don't know the colour of this card
    			    	 switch (card.getColour().toString()){
    			    	 	case "Blue": features.add(1);
    						   			break; 
    			    	 	case "Green": features.add(2);
    			   						break; 
    			    	 	case "Red": features.add(3);
    			   						break; 
    			    	 	case "White": features.add(4);
    			   						break; 
    			    	 	case "Yellow": features.add(5);
    			   						break; 
    			    	 	default: System.out.println("invalid");
    			   						break; 
    			    	 }
    		    	 }
    		  }
    		  //values
    		  for(Card card:playerHand) {
    			  if(card==null) {features.add(0);}else {
    				  features.add(card.getValue());
    			  }
    		  }
    	  }
      }
      
      Action a = playKnown(s);
      if(a==null) a = discardKnown(s);
      if(a==null) a = hint(s);
      if(a==null) a = playGuess(s);
      if(a==null) a = discardGuess(s);
      if(a==null) a = hintRandom(s);

      masterList.add("{"+features.toString().substring(1,features.toString().length()-1)+"}"); //formats object with features into array declaration format
      masterListToString = masterList.toString(); //whole list of objects/fruits
      masterListToString=masterListToString.substring(1, masterListToString.length()-1); //removes [] brackets again
      masterListToString+=','; //adds a comma so our features list is ongoing, appendable
      return a;
    }
    catch(IllegalActionException e){
      e.printStackTrace();
      throw new RuntimeException("Something has gone very wrong");
    }
  }
  
  //updates colours and values from hints received
  public void getHints(State s){
    try{
      State t = (State) s.clone();
      for(int i = 0; i<Math.min(numPlayers-1,s.getOrder());i++){
        Action a = t.getPreviousAction();
        if((a.getType()==ActionType.HINT_COLOUR || a.getType() == ActionType.HINT_VALUE) && a.getHintReceiver()==index){
          boolean[] hints = t.getPreviousAction().getHintedCards();
          for(int j = 0; j<hints.length; j++){
            if(hints[j]){
              if(a.getType()==ActionType.HINT_COLOUR) 
                colours[j] = a.getColour();
              else
                values[j] = a.getValue();  
            }
          }
        } 
        t = t.getPreviousState();
      }
    }
    catch(IllegalActionException e){e.printStackTrace();}
  }

  //returns the value of the next playable card of the given colour
  public int playable(State s, Colour c){
    java.util.Stack<Card> fw = s.getFirework(c);
    if (fw.size()==5) return -1;
    else return fw.size()+1;
  }

  //plays the first card known to be playable.
  public Action playKnown(State s) throws IllegalActionException{
    for(int i = 0; i<colours.length; i++){
      if(colours[i]!=null && values[i]==playable(s,colours[i])){
        colours[i] = null;
        values[i] = 0;
        
        //training label
        switch(i) {
        	case 0: labels.add(6);
        	break;
        	case 1: labels.add(7);
        	break;
        	case 2: labels.add(8);
        	break;
        	case 3: labels.add(9);
        	break;
        	case 4: labels.add(10);
        	break;
        }
        labelsToString=labels.toString();
        
        return new Action(index, toString(), ActionType.PLAY,i);
      }
    }
    return null;
  }

  //discards the first card known to be unplayable.
  public Action discardKnown(State s) throws IllegalActionException{
    if (s.getHintTokens() != 8) {
      for(int i = 0; i<colours.length; i++){
        if(colours[i]!=null && values[i]>0 && values[i]<playable(s,colours[i])){
          colours[i] = null;
          values[i] = 0;
          
        //training label
          switch(i) {
	      	case 0: labels.add(1);
	      	break;
	      	case 1: labels.add(2);
	      	break;
	      	case 2: labels.add(3);
	      	break;
	      	case 3: labels.add(4);
	      	break;
	      	case 4: labels.add(5);
	      	break;
	      }
          labelsToString=labels.toString();
          
          return new Action(index, toString(), ActionType.DISCARD,i);
        }
      }
    }
    return null;
  }

  //gives hint of first playable card in next players hand
  //flips a coin to determine whether it is a colour hint or value hint
  //return null if no hint token left, or no playable cards
  public Action hint(State s) throws IllegalActionException{
    if(s.getHintTokens()>0){
      for(int i = 1; i<numPlayers; i++){
        int hintee = (index+i)%numPlayers;
        Card[] hand = s.getHand(hintee);
        for(int j = 0; j<hand.length; j++){
          Card c = hand[j];
          if(c!=null && c.getValue()==playable(s,c.getColour())){//goes through teammates hand to see if any cards are playable
            //flip coin
            if(Math.random()>0.5){//give colour hint
              boolean[] col = new boolean[hand.length]; 
              for(int k = 0; k< col.length; k++){
                col[k]=c.getColour().equals((hand[k]==null?null:hand[k].getColour()));
              }
              
              //training label
                switch(c.getColour().toString()) {
	    	 	case "Blue": switch(hintee) {
					    	 	case 0: 
					    	 		labels.add(11);
					    	 		break;
					    	 	case 1:
					    	 		labels.add(12);
					    	 		break;
					    	 	case 2:
					    	 		labels.add(13);
					    	 		break;
					    	 	case 3:
					    	 		labels.add(14);
					    	 		break;
					    	 	case 4:
					    	 		labels.add(15);
					    	 		break;
	    	 				}
	   						 break; 
			 	case "Green": switch(hintee) {
					    	 	case 0: 
					    	 		labels.add(16);
					    	 		break;
					    	 	case 1:
					    	 		labels.add(17);
					    	 		break;
					    	 	case 2:
					    	 		labels.add(18);
					    	 		break;
					    	 	case 3:
					    	 		labels.add(19);
					    	 		break;
					    	 	case 4:
					    	 		labels.add(20);
					    	 		break;
								}
							 break; 
			 	case "Red":  switch(hintee) {
					    	 	case 0: 
					    	 		labels.add(21);
					    	 		break;
					    	 	case 1:
					    	 		labels.add(22);
					    	 		break;
					    	 	case 2:
					    	 		labels.add(23);
					    	 		break;
					    	 	case 3:
					    	 		labels.add(24);
					    	 		break;
					    	 	case 4:
					    	 		labels.add(25);
					    	 		break;
								}
							 break; 
			 	case "White":  switch(hintee) {
					    	 	case 0: 
					    	 		labels.add(26);
					    	 		break;
					    	 	case 1:
					    	 		labels.add(27);
					    	 		break;
					    	 	case 2:
					    	 		labels.add(28);
					    	 		break;
					    	 	case 3:
					    	 		labels.add(29);
					    	 		break;
					    	 	case 4:
					    	 		labels.add(30);
					    	 		break;
								}
							 break; 
			 	case "Yellow":  switch(hintee) {
						    	 	case 0: 
						    	 		labels.add(31);
						    	 		break;
						    	 	case 1:
						    	 		labels.add(32);
						    	 		break;
						    	 	case 2:
						    	 		labels.add(33);
						    	 		break;
						    	 	case 3:
						    	 		labels.add(34);
						    	 		break;
						    	 	case 4:
						    	 		labels.add(35);
						    	 		break;
									}
							 break; 
			 	default: System.out.println("invalid");
							 break; 
			 }
                labelsToString=labels.toString(); 
                //col/val are the cards affected in the teammates hand in the form of a bool array
//              for(boolean card:col) {
//                  System.out.println(card);
//              }
//                boolean[] col = new boolean[hand.length]; 
//                for(int k = 0; k< col.length; k++){
//                  col[k]=ENUMColour.equals((hand[k]==null?null:hand[k].getColour()));
//                }
              return new Action(index,toString(),ActionType.HINT_COLOUR,hintee,col,c.getColour());
            }
            else{//give value hint
              boolean[] val = new boolean[hand.length];
              for(int k = 0; k< val.length; k++){
                val[k]=c.getValue() == (hand[k]==null?-1:hand[k].getValue());
              }
              
              //training label
              switch(c.getValue()) {
	    	 	case 1: switch(hintee) {
					    	 	case 0: 
					    	 		labels.add(36);
					    	 		break;
					    	 	case 1:
					    	 		labels.add(37);
					    	 		break;
					    	 	case 2:
					    	 		labels.add(38);
					    	 		break;
					    	 	case 3:
					    	 		labels.add(39);
					    	 		break;
					    	 	case 4:
					    	 		labels.add(40);
					    	 		break;
	    	 				}
	   						 break; 
			 	case 2: switch(hintee) {
					    	 	case 0: 
					    	 		labels.add(41);
					    	 		break;
					    	 	case 1:
					    	 		labels.add(42);
					    	 		break;
					    	 	case 2:
					    	 		labels.add(43);
					    	 		break;
					    	 	case 3:
					    	 		labels.add(44);
					    	 		break;
					    	 	case 4:
					    	 		labels.add(45);
					    	 		break;
								}
							 break; 
			 	case 3:  switch(hintee) {
					    	 	case 0: 
					    	 		labels.add(46);
					    	 		break;
					    	 	case 1:
					    	 		labels.add(47);
					    	 		break;
					    	 	case 2:
					    	 		labels.add(48);
					    	 		break;
					    	 	case 3:
					    	 		labels.add(49);
					    	 		break;
					    	 	case 4:
					    	 		labels.add(50);
					    	 		break;
								}
							 break; 
			 	case 4:  switch(hintee) {
					    	 	case 0: 
					    	 		labels.add(51);
					    	 		break;
					    	 	case 1:
					    	 		labels.add(52);
					    	 		break;
					    	 	case 2:
					    	 		labels.add(53);
					    	 		break;
					    	 	case 3:
					    	 		labels.add(54);
					    	 		break;
					    	 	case 4:
					    	 		labels.add(55);
					    	 		break;
								}
							 break; 
			 	case 5:  switch(hintee) {
						    	 	case 0: 
						    	 		labels.add(56);
						    	 		break;
						    	 	case 1:
						    	 		labels.add(57);
						    	 		break;
						    	 	case 2:
						    	 		labels.add(58);
						    	 		break;
						    	 	case 3:
						    	 		labels.add(59);
						    	 		break;
						    	 	case 4:
						    	 		labels.add(50);
						    	 		break;
									}
							 break; 
			 	default: System.out.println("invalid");
							 break; 
			 }
                labelsToString=labels.toString();
                
              return new Action(index,toString(),ActionType.HINT_VALUE,hintee,val,c.getValue());
            }
          }
        }
      }
    }
    return null;
  }

  //with probability 0.05 for each fuse token, play a random card 
  public Action playGuess(State s) throws IllegalActionException{
    java.util.Random rand = new java.util.Random();
    for(int i = 0; i<s.getFuseTokens(); i++){
      if(rand.nextDouble()<0.05){
        int cardIndex = rand.nextInt(colours.length);
        colours[cardIndex] = null;
        values[cardIndex] = 0;
        
      //training label
        switch(cardIndex) {
    	case 0: labels.add(6);
    	break;
    	case 1: labels.add(7);
    	break;
    	case 2: labels.add(8);
    	break;
    	case 3: labels.add(9);
    	break;
    	case 4: labels.add(10);
    	break;
    }
        labelsToString=labels.toString();
        
        return new Action(index, toString(), ActionType.PLAY, cardIndex);
      }
    }
    return null;
  }
  
  //discard a random card
  public Action discardGuess(State s) throws IllegalActionException{
    if (s.getHintTokens() != 8) {
      java.util.Random rand = new java.util.Random();
      int cardIndex = rand.nextInt(colours.length);
      colours[cardIndex] = null;
      values[cardIndex] = 0;
      
    //training label
      switch(cardIndex) {
    	case 0: labels.add(1);
    	break;
    	case 1: labels.add(2);
    	break;
    	case 2: labels.add(3);
    	break;
    	case 3: labels.add(4);
    	break;
    	case 4: labels.add(5);
    	break;
    }
      labelsToString=labels.toString();
      
      return new Action(index, toString(), ActionType.DISCARD, cardIndex);
    }
    return null;
  }

  //gives random hint of a card in next players hand
  //flips a coin to determine whether it is a colour hint or value hint
  //return null if no hint token left
  public Action hintRandom(State s) throws IllegalActionException{
    if(s.getHintTokens()>0){
        int hintee = (index+1)%numPlayers;
        Card[] hand = s.getHand(hintee);

        java.util.Random rand = new java.util.Random();
        int cardIndex = rand.nextInt(hand.length);
        while(hand[cardIndex]==null) cardIndex = rand.nextInt(hand.length);
        Card c = hand[cardIndex];

        if(Math.random()>0.5){//give colour hint
          boolean[] col = new boolean[hand.length];
          for(int k = 0; k< col.length; k++){
            col[k]=c.getColour().equals((hand[k]==null?null:hand[k].getColour()));
          }
          
        //training label
          switch(c.getColour().toString()) {
  	 	case "Blue": switch(hintee) {
				    	 	case 0: 
				    	 		labels.add(11);
				    	 		break;
				    	 	case 1:
				    	 		labels.add(12);
				    	 		break;
				    	 	case 2:
				    	 		labels.add(13);
				    	 		break;
				    	 	case 3:
				    	 		labels.add(14);
				    	 		break;
				    	 	case 4:
				    	 		labels.add(15);
				    	 		break;
  	 				}
 						 break; 
		 	case "Green": switch(hintee) {
				    	 	case 0: 
				    	 		labels.add(16);
				    	 		break;
				    	 	case 1:
				    	 		labels.add(17);
				    	 		break;
				    	 	case 2:
				    	 		labels.add(18);
				    	 		break;
				    	 	case 3:
				    	 		labels.add(19);
				    	 		break;
				    	 	case 4:
				    	 		labels.add(20);
				    	 		break;
							}
						 break; 
		 	case "Red":  switch(hintee) {
				    	 	case 0: 
				    	 		labels.add(21);
				    	 		break;
				    	 	case 1:
				    	 		labels.add(22);
				    	 		break;
				    	 	case 2:
				    	 		labels.add(23);
				    	 		break;
				    	 	case 3:
				    	 		labels.add(24);
				    	 		break;
				    	 	case 4:
				    	 		labels.add(25);
				    	 		break;
							}
						 break; 
		 	case "White":  switch(hintee) {
				    	 	case 0: 
				    	 		labels.add(26);
				    	 		break;
				    	 	case 1:
				    	 		labels.add(27);
				    	 		break;
				    	 	case 2:
				    	 		labels.add(28);
				    	 		break;
				    	 	case 3:
				    	 		labels.add(29);
				    	 		break;
				    	 	case 4:
				    	 		labels.add(30);
				    	 		break;
							}
						 break; 
		 	case "Yellow":  switch(hintee) {
					    	 	case 0: 
					    	 		labels.add(31);
					    	 		break;
					    	 	case 1:
					    	 		labels.add(32);
					    	 		break;
					    	 	case 2:
					    	 		labels.add(33);
					    	 		break;
					    	 	case 3:
					    	 		labels.add(34);
					    	 		break;
					    	 	case 4:
					    	 		labels.add(35);
					    	 		break;
								}
						 break; 
		 	default: System.out.println("invalid");
						 break; 
		 }
          labelsToString=labels.toString();
          
          return new Action(index,toString(),ActionType.HINT_COLOUR,hintee,col,c.getColour());
        }
        else{//give value hint
          boolean[] val = new boolean[hand.length];
          for(int k = 0; k< val.length; k++){
            if (hand[k] == null) continue;
            val[k]=c.getValue() == (hand[k]==null?-1:hand[k].getValue());
          }
          
        //training label
          switch(c.getValue()) {
  	 	case 1: switch(hintee) {
				    	 	case 0: 
				    	 		labels.add(36);
				    	 		break;
				    	 	case 1:
				    	 		labels.add(37);
				    	 		break;
				    	 	case 2:
				    	 		labels.add(38);
				    	 		break;
				    	 	case 3:
				    	 		labels.add(39);
				    	 		break;
				    	 	case 4:
				    	 		labels.add(40);
				    	 		break;
  	 				}
 						 break; 
		 	case 2: switch(hintee) {
				    	 	case 0: 
				    	 		labels.add(41);
				    	 		break;
				    	 	case 1:
				    	 		labels.add(42);
				    	 		break;
				    	 	case 2:
				    	 		labels.add(43);
				    	 		break;
				    	 	case 3:
				    	 		labels.add(44);
				    	 		break;
				    	 	case 4:
				    	 		labels.add(45);
				    	 		break;
							}
						 break; 
		 	case 3:  switch(hintee) {
				    	 	case 0: 
				    	 		labels.add(46);
				    	 		break;
				    	 	case 1:
				    	 		labels.add(47);
				    	 		break;
				    	 	case 2:
				    	 		labels.add(48);
				    	 		break;
				    	 	case 3:
				    	 		labels.add(49);
				    	 		break;
				    	 	case 4:
				    	 		labels.add(50);
				    	 		break;
							}
						 break; 
		 	case 4:  switch(hintee) {
				    	 	case 0: 
				    	 		labels.add(51);
				    	 		break;
				    	 	case 1:
				    	 		labels.add(52);
				    	 		break;
				    	 	case 2:
				    	 		labels.add(53);
				    	 		break;
				    	 	case 3:
				    	 		labels.add(54);
				    	 		break;
				    	 	case 4:
				    	 		labels.add(55);
				    	 		break;
							}
						 break; 
		 	case 5:  switch(hintee) {
					    	 	case 0: 
					    	 		labels.add(56);
					    	 		break;
					    	 	case 1:
					    	 		labels.add(57);
					    	 		break;
					    	 	case 2:
					    	 		labels.add(58);
					    	 		break;
					    	 	case 3:
					    	 		labels.add(59);
					    	 		break;
					    	 	case 4:
					    	 		labels.add(50);
					    	 		break;
								}
						 break; 
		 	default: System.out.println("invalid");
						 break; 
		 }
          labelsToString=labels.toString();
          
          return new Action(index,toString(),ActionType.HINT_VALUE,hintee,val,c.getValue());
        }

      }
    
    return null;
  }

}