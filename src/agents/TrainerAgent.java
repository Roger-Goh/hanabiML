package agents;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
      
      features.clear();
      features.add(s.getScore()); //adding feature to single object
      
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
          if(c!=null && c.getValue()==playable(s,c.getColour())){
            //flip coin
            if(Math.random()>0.5){//give colour hint
              boolean[] col = new boolean[hand.length];
              for(int k = 0; k< col.length; k++){
                col[k]=c.getColour().equals((hand[k]==null?null:hand[k].getColour()));
              }
              return new Action(index,toString(),ActionType.HINT_COLOUR,hintee,col,c.getColour());
            }
            else{//give value hint
              boolean[] val = new boolean[hand.length];
              for(int k = 0; k< val.length; k++){
                val[k]=c.getValue() == (hand[k]==null?-1:hand[k].getValue());
              }
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
          return new Action(index,toString(),ActionType.HINT_COLOUR,hintee,col,c.getColour());
        }
        else{//give value hint
          boolean[] val = new boolean[hand.length];
          for(int k = 0; k< val.length; k++){
            if (hand[k] == null) continue;
            val[k]=c.getValue() == (hand[k]==null?-1:hand[k].getValue());
          }
          return new Action(index,toString(),ActionType.HINT_VALUE,hintee,val,c.getValue());
        }

      }
    
    return null;
  }

}