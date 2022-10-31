/**
 * This class defines the properties of the Red Agent
 * They are trying to get the Green Agents to not vote
 * in the elections, and must do so while balancing the
 * between making big claims (high message potencies) and 
 * losing followers.
 */
import java.util.*;
public class redAgent {
	int messagePotency; 
    int roundNum;
    //Agent's certainty
    double persuasivity;
    double epsilon;
    double learningRate;
    //used to calculate rewards
    double predictedCertaintySwing;
    //store information from past rounds
    ArrayList<archive> archive;
    //data structure to store q values
    ArrayList<statePair> qTable = new ArrayList<statePair>();
	// Future Reward discounted rate
    double gamma;    
    
    //Constructor
	public redAgent() {
    	this.gamma = 0.9;
    	this.learningRate = .2;
    	this.epsilon = .9;
    	Random rand = new Random();
    	this.messagePotency = 0;
    	this.predictedCertaintySwing = 0; 
        this.roundNum = 0;
        this.archive = new ArrayList<archive>();
        int rangeMin = 6;
        int rangeMax = 10;
        this.persuasivity = rangeMin + (rangeMax - rangeMin) * rand.nextDouble();
    }
    
    //helper function to determine if two double is approximately equal to each other.
	private boolean approximatelyEqual(double desiredValue, double actualValue, double level) {
        double diff = Math.abs(desiredValue - actualValue);        
        return diff < level;                                   
    }
    
    //helper function to tell the agent whether it has encountered a brand new state or now.
	private int approximateState(double[] state, ArrayList<statePair> qTable, int greenSize) {
		int size = qTable.size();
    	// no recorded state yet
		if(size == 0)
			return -1;
		else {
			//loop through all recorded states and compared it to the current state the agent is in
			for (int i = 0; i < size; i++) {
				statePair sample = qTable.get(i);
				if(approximatelyEqual(sample.state[0], state[0],.05) 
						&& approximatelyEqual(sample.state[1], state[1],.05) 
						&& sample.state[2]== state[2]) {
					//if found a similar state the index is returned
					return i;
					
				}
			}
			return -1;
		}
    }
    
    //helper function to determine the number of noneVoter
	public double countOpinion(greenAgent[] greenTeam) {
		double wontVote = 0;
		int length = greenTeam.length;
		for(int i =0; i < length; i++) {
			if(!greenTeam[i].willVote) {
				wontVote++;
			}
		}
    	return wontVote;
    }
    
    //helper function to count the number of followers
	private int countRedFollowers(greenAgent[] greenTeam) {
		int num = 0;
		int length = greenTeam.length;
		for(int i =0; i < length; i++) {
			if(greenTeam[i].redFollower) {
				num++;
			}
		}
    	return num;
    }
    
	//helper function to predict the immediate rewards of all the actions available to the agent 
	//given the specific state that it is in
	private Double[] predictFutureReward(greenAgent[] greenTeam, int firstMess, int lastMess, double pursua) {
    	//list of predicted rewards
		Double reward[] = new Double[lastMess - firstMess + 1]; 
    	//metrics that the reward will be calculated from
		double predictionScore[] = new double[lastMess - firstMess + 1];
    	double certaintyGain[] = new double[lastMess - firstMess + 1];
    	int index1 = 0;
    	//loop over all the possible moves (1 to 5 message potency)
    	for(int j = firstMess; j <= lastMess; j++) {
    		//the current base certainty
    		double pursuaRed = pursua; 
    		double totalPredictedCertainty = 0.0;
    		double voterGained = 0.0;
        	int followerLost = 0;
        	double certaintyUp = 0.0;
        	int length = greenTeam.length;
    		pursuaRed += messageAffect(j);
        	if(pursuaRed > 10) {
        		pursuaRed = 10;
        	}
        	else if(pursuaRed < 0) {
        		pursuaRed = 0;
        	}    	
        	// predicting how the interaction with each green agent will go
        	for (int i = 0; i < length; i++) {
                 if (!greenTeam[i].redFollower) {
                     continue;
                 }
                 if (greenTeam[i].willVote) {
                	 if(greenTeam[i].unfollowed(j,pursuaRed))
                     {
                    	 followerLost++;
                    	 continue;
                     }
                	 // since the agent doesn't know Green's certainty, this function will
                	 // attempt to predict it within 10% of the actual value
                	 double predictedCertainty = greenTeam[i].predictCertainty(1.0);
                     if(predictedCertainty > 10)
                    	 predictedCertainty = 10;
                     if(predictedCertainty < 0)
                    	 predictedCertainty = 0;
                     
                     //adding to reward metric
                     certaintyUp += (pursuaRed *(1.0 + j / 5.0));
                     //predicting the certainty swing
                     totalPredictedCertainty +=predictedCertainty;
                     predictedCertainty =  greenTeam[i].assignScore(length) * 2 +  (predictedCertainty) - (pursuaRed *(1.0 + j / 5.0));               	 
                     if(predictedCertainty <= 0) {
                		 voterGained++;
                	 }
                 }
                 else {
                	 //adding to reward metric
                	 certaintyUp += (j * (j/4.0)) + (pursuaRed / 5.0);
                 }
                 
        	 }
        	//record the metrics for the move
        	predictionScore[index1] = voterGained *10 - (followerLost * 15); 
        	certaintyGain[index1] = certaintyUp - totalPredictedCertainty;
        	index1++;
    	}
    	
    		//calculate the predicted rewards for all move and return it
    		int length = predictionScore.length;
    		for (int i = 0; i < length ; i++) {
            	double value = predictionScore[i] + certaintyGain[i];
            	reward[i] = value;
                }
    		return reward;
    	}
    // Policy on how the agent should pick a move
    private int messageToPush(greenAgent[] greenTeam, double pursua, int indexOfState, boolean newState, double NoneVoterThisRound) {
    	statePair current = this.qTable.get(indexOfState);
    	Random rand = new Random();
    	//Will the agent take risk? if so push a 4 or 5 message
    	if(takeRisk(greenTeam, NoneVoterThisRound)) {
    		int min = 4;
     		int max = 6;
     		return rand.nextInt(max - min) + min;
    	}
    	
    	//is the current state an state already recorded in the q table?
    	if(!newState) {
        	double probability = rand.nextDouble();
        	//check if there are moves that has not been made before
        	int zeroIndex = hasZeros(current.qValues);
        	
        	//will the agent explore?
        	if(probability <= this.epsilon && zeroIndex != -1) {
        		if(this.epsilon > .06) 
         			this.epsilon -= .05;
        		return zeroIndex + 1;
         	}
        	if (probability <= this.epsilon) {
        		int min = 1;
         		int max = 4;
         		if(this.epsilon > .06) 
         			this.epsilon -= .05;
         		return rand.nextInt(max - min) + min;
        	}
        	
        	//if not exlore then it will exploit the move with max Q
        	double max = current.qValues[0];
    		int index = 0;
            for (int i = 1; i < 5; i++) {
            	if (current.qValues[i] > max){
                    max = current.qValues[i];
                    index = i;
                }
            }
        	return index + 1;
        }
    	
    		//if the state is brand new
    	    Double[] metric = predictFutureReward(greenTeam, 1 , 5, pursua);
    		double max = metric[0];
    		int index = 0;
            for (int i = 1; i < 5 ; i++) {
            	if (metric[i] > max){
                    max = metric[i];
                    index = i;
                }
            }
            return index + 1;
    	
    }
    
    // return the index of the first 0 found
    private int hasZeros(double[] qValues) {
		for(int i =0; i < 5; i++) {
			if (qValues[i] == 0.0)
				return i;
		}		
		return -1;
	}
    
	//function to determine if the agent willing to take risk
    private boolean takeRisk(greenAgent[] greenTeam, double followers) {
    	double size = greenTeam.length;
    	double percentNotVote = countOpinion(greenTeam) / size ;
    	double percentStillFollow = followers / size;
		if(percentNotVote > .6 && percentStillFollow > .5 && persuasivity > 6)
			return true;
    	return false;
    }
    
    //function to determine the affect of a message on the agent's certainty
    private double messageAffect(int Potency) {
    	double bonus = 0;
    	switch (Potency) {
	        case 5:
	        	bonus = -4;
	            break;
	        case 4:
	            bonus = -2;
	            break;
	        case 3:
	            bonus = 0;
	            break;
	        case 2:
	            bonus = 1;
	            break;
	        default:
	        	bonus = 1.5;
	        }
    	return bonus;
	}  
    
    //Support for human play
    public void redHumanTurn(greenAgent[] greenTeam) { 
    	System.out.println("Your Team's Turn");
    	Scanner s = new Scanner(System.in);
    			double NoneVoterThisRound = countOpinion(greenTeam);
		int totalFollowersThisRound = countRedFollowers(greenTeam);
		archive current = new archive();
		current.roundNum = this.roundNum;
		if(this.roundNum == 0) {
	    	System.out.println("first round: No info from previous turn to display");
	    	System.out.println("current noneVoter: " + NoneVoterThisRound);
	    	System.out.println("current Number of Followers: " + totalFollowersThisRound);
    	}
		else {
    		double actualGain = NoneVoterThisRound - (this.archive.get(roundNum - 1).NoneVoter);
    		double actualLoss = (this.archive.get(roundNum - 1).followers) - totalFollowersThisRound;
        	System.out.println("Followers lost prev round (negative mean a gain): " + actualLoss);
            System.out.println("nonVoter gain prev round: " + actualGain);
            System.out.println("current noneVoter: " + NoneVoterThisRound);
  
    	}
		current.NoneVoter = NoneVoterThisRound;
    	current.followers = totalFollowersThisRound;
    	while (true) {
	           try {
	                 System.out.println("Select a message certainty from below:");
	                 System.out.println("1 : Free education for all citizens");
	                 System.out.println("2 : Drastically reduce tertiary education fees");
	                 System.out.println("3 : Major Improvement to schools and univerities");
	                 System.out.println("4 : Blue sent an assassin after the Leader of the Red Party.");
	                 System.out.println("5 : Blue is involved in a scheme to funnel tax moneys into their higher's up pocket");
	                 System.out.println("Enter a number from 1 to 5 to indicate your choice.");
	                 this.messagePotency = s.nextInt();
	                 if (this.messagePotency < 1 || this.messagePotency > 5) {
	                     throw new IllegalArgumentException();
	                 } else {
	                     break;
	                 }
	             } catch (IllegalArgumentException e) {
	                 System.out.println("Your input was not in the correct range. Please try again\n");
	             }
	    }
    	current.messagePotency = this.messagePotency;
    	persuasivity += messageAffect(messagePotency);
    	if(persuasivity > 10) {
    		persuasivity = 10;
    	}
    	else if(persuasivity < 0) {
    		persuasivity = 0;
    	}    	 
    	redInteract(current, greenTeam);
        System.out.println("Sent out a Potency value of " + messagePotency + "\n");
	}	
    
    //Red Turn
    public void redTurn(greenAgent[] greenTeam, blueAgent2 blue) { 
		System.out.println("Red Teams Turn");
		double NoneVoterThisRound = countOpinion(greenTeam);
    	boolean newState = false;
    	double reward = 0;
    	double[] currentState;
    	//used to store the information of this round
    	archive current = new archive();
    	int indexOfState = -1;
    	current.roundNum = this.roundNum;
    	int totalFollowersThisRound = countRedFollowers(greenTeam);
    	System.out.println("Total Followers this round: " + totalFollowersThisRound);
    	// if this is the first round, create a new state for the q table
    	if(this.roundNum == 0) {
    		newState = true;
    		currentState = evaluatestate(greenTeam, -1);
	    	indexOfState = 0;
	    	statePair newPair = new statePair(currentState);
	    	this.qTable.add(newPair);
	    	System.out.println("first round: No info from previous turn to display");
	    	System.out.println("current noneVoter: " + NoneVoterThisRound);
    	}
    	//if not, update the Q value of the move the agent made from the previous turn
    	else {
    		double actualGain = NoneVoterThisRound - (this.archive.get(roundNum - 1).NoneVoter);
    		double actualLoss = (this.archive.get(roundNum - 1).followers) - totalFollowersThisRound;
    		int indexOfPreState = this.archive.get(roundNum - 1).indexOfState;
    		predictedCertaintySwing -= 5 * ((this.archive.get(roundNum - 1).PredictedNoneVoter) - actualGain); 
    		currentState = evaluatestate(greenTeam, blue.archive.get(roundNum - 1).messagePotency);
    		int preAction = this.archive.get(roundNum - 1).messagePotency - 1;
        	int size = qTable.size();
        	indexOfState = approximateState(currentState, qTable, greenTeam.length);
        	if(indexOfState == -1) {
        		newState = true;
        		statePair newPair = new statePair(currentState);
        		this.qTable.add(newPair);
        		indexOfState = size;
        		size++;
        	}
        	reward =  actualGain * 10 - actualLoss * 15 + predictedCertaintySwing;
        	Double[] predictedNextQ = predictedNextQ(qTable, currentState, indexOfState);
        	double qOld = this.qTable.get(indexOfPreState).qValues[preAction]; 
        	this.qTable.get(indexOfPreState).qValues[preAction] = qOld + this.learningRate * (reward + this.gamma * Collections.max(Arrays.asList(predictedNextQ)) - qOld);
        	System.out.println("Followers lost prev round (negative mean a gain): " + actualLoss);
            System.out.println("nonVoter gain prev round: " + actualGain);
            System.out.println("current noneVoter: " + NoneVoterThisRound);
  
    	}
    	//store information of this round
    	current.NoneVoter = NoneVoterThisRound;
    	current.indexOfState = indexOfState;
    	current.followers = totalFollowersThisRound;
    	
    	this.predictedCertaintySwing = 0;
    	//deciding which message to push
    	this.messagePotency = messageToPush(greenTeam, this.persuasivity, indexOfState, newState, totalFollowersThisRound);
    	current.messagePotency = this.messagePotency;
    	//adjust agent certainty
    	persuasivity += messageAffect(messagePotency);
    	if(persuasivity > 10) {
    		persuasivity = 10;
    	}
    	else if(persuasivity < 0) {
    		persuasivity = 0;
    	}    	 
    	
    	//interact with Green
    	redInteract(current, greenTeam);
        System.out.println("Sent out a Potency value of " + messagePotency + "\n");   	
    }
    
    //Interaction function
	private void redInteract(archive current, greenAgent[] greenTeam) {
		double PredictedNoneVoterGain = 0;
    	double certaintyUp = 0.0;
    	double totalPredictedCertainty = 0.0;
    	int length = greenTeam.length;
    	
    	//loop through all the green agent and interact
    	for (int i = 0; i < length; i++) {
    		 //if the agent is not a follower and does not decide to refollow Red, skip it
    		 if (!greenTeam[i].redFollower && !reFollow(greenTeam[i], greenTeam)) {
                 continue;
             }
    		 //if it will vote, try to sway it to Red's side
             if (greenTeam[i].willVote) {
            	 if(greenTeam[i].unfollowed(messagePotency,persuasivity))
                 {
                	 greenTeam[i].redFollower = false;
                	 continue;
                 }
            	 double predictedCertainty = greenTeam[i].predictCertainty(1.0);
                 if(predictedCertainty > 10)
                	 predictedCertainty = 10;
                 if(predictedCertainty < 0)
                	 predictedCertainty = 0;
                 certaintyUp += (persuasivity *(1.0 + messagePotency  / 5.0));
                 totalPredictedCertainty +=predictedCertainty;
            	 greenTeam[i].certainty = greenTeam[i].assignScore(length) * 5 +  (greenTeam[i].certainty)   - (persuasivity *(1.0 + messagePotency / 5.0));           	 
            	 greenTeam[i].checkOpinion(persuasivity);
            	 if(!greenTeam[i].willVote) {
            		 PredictedNoneVoterGain++;
            	 }
             }
            
             //if already not vote, increase certainty toward not voting
             else {
            	 certaintyUp += (messagePotency * (messagePotency/4.0)) + (persuasivity / 5.0);
            	 greenTeam[i].certainty += (messagePotency * (messagePotency/4.0)) + (persuasivity / 5.0);
            	 if(greenTeam[i].certainty > 10) {
            		 greenTeam[i].certainty = 10;
             	}
            	 
             }
    	 }
    	predictedCertaintySwing = certaintyUp - totalPredictedCertainty;
    	current.PredictedNoneVoter = PredictedNoneVoterGain;
    	//store all the information of this round.
    	archive.add(current);
    	this.roundNum++;
		
	}

	//helper function to determine if a green agent decide to refollow red
	private boolean reFollow(greenAgent a, greenAgent[] greenTeams) {
		int size = a.neighbors.size();
    	Random rand = new Random();
    	double probability = rand.nextDouble();
		double count = 0.0;
		for (int i = 0; i < size; i++) {
			int NeighBorId = a.neighbors.get(i);
			greenAgent neighbor = greenTeams[NeighBorId];
			if(neighbor.redFollower) {
				count++;
			}
		}
		double percent = count / size;
		if(percent > .5 && probability <= (0.1 + (percent - 0.5))) {
			a.redFollower = true;
			return true;
		}
		return false;
	}

	//extract the Q values of a given state
	private Double[] predictedNextQ(ArrayList<statePair> qTable2, double[] state_next, int index) {
    	Double[] qs = new Double[5];
    	for (int i = 0; i < 5; i++)
    		qs[i]= qTable2.get(index).qValues[i];
    	return qs;
	}
	
	//make a state of the game
	private double[] evaluatestate(greenAgent[] greenTeam2, double bluePreAction) {
		double[] state = new double[3];
		double size = greenTeam2.length;
		state[0] = countOpinion(greenTeam2) / size;
		state[1] = countRedFollowers(greenTeam2) / size;
		state[2] = bluePreAction;
		return state;
	}

	

}
