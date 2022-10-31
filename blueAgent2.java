/**
 * This class defines the properties of the Blue Agent
 * They are trying to get the Green Agents to vote
 * in the elections, and must invest their own
 * energy to do so. They also have the option to 
 * deploy a grey agent but it is risky as the
 * grey agent can betray them and work against
 * their agenda
 */
import java.util.*;
public class blueAgent2 {
	int messagePotency;
    int roundNum;
    double energy;
    int betrayed;
    int nGray;
    int nGrayUsed;
    double useGrayChance;
    double persuasivity;
    double epsilon;
    double learningRate;
    int day;
    double predictedCertaintySwing;
    ArrayList<archive> archive;
    ArrayList<statePair> qTable = new ArrayList<statePair>();
	double gamma;
    
    public blueAgent2(int day, int ngreen, int ngray) {
    	this.nGrayUsed = 0;
    	this.betrayed = 0;
    	this.useGrayChance = 0.2;
    	this.nGray = ngray;
    	this.gamma = 0.9;
    	this.learningRate = .2;
    	this.epsilon = .9;
    	this.day = day;
    	this.energy = (ngreen * day) / 3;
    	Random rand = new Random();
    	this.messagePotency = 0;
    	this.predictedCertaintySwing = 0; 
        this.roundNum = 0;
        this.archive = new ArrayList<archive>();
        //setting certainty
        int rangeMin = 2;
        int rangeMax = 6;
        persuasivity = rangeMin + (rangeMax - rangeMin) * rand.nextDouble();
    }
       
    private boolean approximatelyEqual(double desiredValue, double actualValue, double level) {
        double diff = Math.abs(desiredValue - actualValue);         //  1000 - 950  = 50
        return diff < level;                                   
    }
    
    private int approximateState(double[] state, ArrayList<statePair> qTable, int greenSize) {
		int size = qTable.size();
    	if(size == 0)
			return -1;
		else {
			for (int i = 0; i < size; i++) {
				statePair sample = qTable.get(i);
				if(approximatelyEqual(sample.state[0], state[0],.05) 
						&& approximatelyEqual(sample.state[1], state[1],.05) 
						&& sample.state[2] == state[2]) {
					return i;
					
				}
			}
			return -1;
		}
    }
    
    public double countOpinion(greenAgent[] greenTeam) {
		double Vote = 0;
		int length = greenTeam.length;
		for(int i =0; i < length; i++) {
			if(greenTeam[i].willVote) {
				Vote++;
			}
		}
    	return Vote;
    }
    
    
    private Double[] predictFutureReward(greenAgent[] greenTeam, int firstMess, int lastMess, double pursua) {
    	Double reward[] = new Double[lastMess - firstMess + 1];
    	double predictionScore[] = new double[lastMess - firstMess + 1];
    	double certaintyGain[] = new double[lastMess - firstMess + 1];
    	int index1 = 0;
    	for(int j = firstMess; j <= lastMess; j++) {
    		double pursuaBlue = pursua; 
    		double totalPredictedCertainty = 0.0;
    		double voterGained = 0.0;
        	double eCost = 0;
        	double certaintyUp = 0.0;
        	int length = greenTeam.length;
        	double results[] = messageAffect(j,pursuaBlue, length);
        	pursuaBlue = results[0];
        	eCost = results[1];
        	for (int i = 0; i < length; i++) {
                 if (greenTeam[i].willVote) {
                	 double predictedCertainty = greenTeam[i].predictCertainty(1.0);
                     if(predictedCertainty > 10)
                    	 predictedCertainty = 10;
                     if(predictedCertainty < 0)
                    	 predictedCertainty = 0;
                     certaintyUp += 0.95*(pursuaBlue *(1.0 + j / 5.0));
                     totalPredictedCertainty +=predictedCertainty;
                     predictedCertainty =  greenTeam[i].assignScore(length) * 15 +  (predictedCertainty) - .95*(pursuaBlue *(1.0 + j / 5.0));               	 
                     if(predictedCertainty <= 0) {
                		 voterGained++;
                	 }
                 }
                 else {
                	 certaintyUp += .6*((j * (j/4.0)) + (pursuaBlue / 5.0));
                 }
                 eCost+=.5;
        	 }
        	predictionScore[index1] = voterGained *10 - eCost * 3; 
        	certaintyGain[index1] = certaintyUp - totalPredictedCertainty;
        	index1++;
    	}
    	
    		int length = predictionScore.length;
    		for (int i = 0; i < length ; i++) {
            	double value = certaintyGain[i] / 2.0 + predictionScore[i];
            	reward[i] = value;
                }
    		return reward;
    	}
    
    private int messageToPush(greenAgent[] greenTeam, double pursua, int indexOfState, boolean newState, double energy) {
    	statePair current = this.qTable.get(indexOfState);
    	Random rand = new Random();
    	if(checkUseGrey(this.energy, greenTeam.length)) {  
    		return 6;
    	}
    	if(takeRisk(greenTeam, energy)) {
    		int min = 4;
     		int max = 6;
     		return rand.nextInt(max - min) + min;
    	}
    	
    	if(!newState) {
        	double probability = rand.nextDouble();
        	int zeroIndex = hasZeros(current.qValues);
        	if(probability <= this.epsilon && zeroIndex != -1) {
        		if(this.epsilon > .06) 
         			this.epsilon -= .05;
        		return zeroIndex + 1;
         	}
        	if (probability <= this.epsilon) {
        		int min = 1;
         		int max = 5;
         		if(this.epsilon > .06) 
         			this.epsilon -= .05;
         		return rand.nextInt(max - min) + min;
        	}
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
    
    
    private int hasZeros(double[] qValues) {
		for(int i =0; i < 5; i++) {
			if (qValues[i] == 0.0)
				return i;
		}		
		return -1;
	}
    
	private boolean takeRisk(greenAgent[] greenTeam, double energy) {
    	double size = greenTeam.length;
    	double percentVote = countOpinion(greenTeam) / size ;
    	//double percentEnergy = followers / size;
		if((percentVote < .4 || percentVote > .6) && energy > (0.5*size*this.day))
			return true;
    	return false;
    }
    
    private double[] messageAffect(int Potency, double persuaBlue, int size) {
    	double certainty = persuaBlue;
    	double eCost = 0;
    	double cost[] = {0, 0};
    	switch (Potency) {
	        case 5:
	        	certainty += 8;
	        	eCost = size; 
	            break;
	        case 4:
	        	certainty += 5;
	        	eCost = size*.6; 
	            break;
	        case 3:
	        	certainty += 4;
	        	eCost = size*.2; 
	            break;
	        case 2:
	        	certainty += 2;
	        	eCost = -size*.5; 
	            break;
	        default:
	        	certainty += 1;
	        	eCost = -size; 
	        }
    	cost[0] = certainty;
    	cost[1] = eCost;
    	return cost;
	}  
    
    public void blueHumanTurn(greenAgent[] greenTeam, greyAgent[] greyTeam) { 
    	System.out.println("Your Team's Turn");
    	Scanner s = new Scanner(System.in);
    	double VoterThisRound = countOpinion(greenTeam);
    	boolean useGray = false;
    	double energyCostThisTurn = 0;
		this.energy += VoterThisRound / 2;
    	double certaintyThisTurn = persuasivity;
    	archive current = new archive();
    	current.roundNum = this.roundNum;
    	if(this.roundNum == 0) {
	    	System.out.println("first round: No info from previous turn to display");
	    	System.out.println("current Voter: " + VoterThisRound);
	        System.out.println("current Energy: " + this.energy);
    	}
    	else {	
    		double actualGain = VoterThisRound - (this.archive.get(roundNum - 1).Voter);
    		double actualLoss = (this.archive.get(roundNum - 1).eCost) - VoterThisRound / 2;
        	System.out.println("Energy lost prev round (negative loss means gain): " + actualLoss);
            System.out.println("Voter gain prev round: " + actualGain);
            System.out.println("current Voter: " + VoterThisRound);
            System.out.println("current Energy: " + this.energy);
    	}
    	current.Voter = VoterThisRound;
    	if(this.nGrayUsed >= this.nGray)
    		System.out.println("You have run out of Grey Agents.");
    	else {
	    	while (true) {
	             try {
	                 System.out.println("Do you want to use a grey agent? (y/n)");
	                 System.out.println("Be Warned that they might betray you.");
	                 char nextChar = s.next().charAt(0);
	                 if (!(nextChar == 'y' || nextChar == 'Y' || nextChar == 'n' || nextChar == 'N')) {
	                     throw new IllegalArgumentException();
	                 }
	                 if (nextChar == 'y' || nextChar == 'Y') {
	                	 useGray = true;
	                	 this.messagePotency = 6;
	                 }
	                 break;
	             } catch (IllegalArgumentException e) {
	                 System.out.println("Input was not valid, please try again\n");
	             }
	        }
    	}
    	if(!useGray) {
	    	while (true) {
	               try {
	            	     System.out.println("Select a message certainty from below:");
		                 System.out.println("1 (Major Energy Gain) : Claim that to Vote blue is to protect free media, freedom of expression, freedom of speech.");
		                 System.out.println("2 (Morderate Energy Gain) : Claim that whatever Red promised, blue can do it, and do it better and with a clear moral compass.");
		                 System.out.println("3 (Slight Energy Loss) : Live Broadcast to discuss the vision that the blue party has for the future of the population");
		                 System.out.println("4 (Morderate Energy Loss) : Campaign to spread the democratic values of blue such as free media, freedom of expression, freedom of speech.");
		                 System.out.println("5 (Major Energy Loss):Campaign to expose The Red party as a group of liars and manipulators who will do all in their power to keep you citizens ignorant.");
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
	    	int length = greenTeam.length;
	    	current.messagePotency = this.messagePotency;
	    	double results[] = messageAffect(messagePotency, certaintyThisTurn, length);
			certaintyThisTurn = results[0];
	    	if(certaintyThisTurn > 10)
	    			certaintyThisTurn = 10;
	    	energyCostThisTurn = results[1];
	    	blueInteract(current,certaintyThisTurn, greenTeam, energyCostThisTurn, length); 	
	    	System.out.println("Sent out a Potency value of " + messagePotency + "\n");
    	}
    	else {
    		this.nGrayUsed++;
    		double results[] = new double[2];
    		int id = choseGrey(greyTeam);
    		if(greyTeam[id].worksFor == 0) {
    			 System.out.println("Deploy Grey Agent " + "\n");
    			 results = greyTeam[id].greyBlueTurn(greenTeam, current);
    		}
    		else {
    			this.betrayed++;
    			System.out.println("Deploy Grey Agent ");
    			System.out.println("Grey Agent has betrayed you " + "\n");
    			results = greyTeam[id].greyRedTurn(greenTeam, current);
    		}
    		predictedCertaintySwing = results[0];
    		this.energy -= results[1];
    		current.elevel = this.energy;
    		archive.add(current);
    		this.roundNum++;
    		
    	}
    }
    
    public void blueTurn(greenAgent[] greenTeam, greyAgent[] greyTeam, redAgent red) { 
		System.out.println("Blue Teams Turn");
		double VoterThisRound = countOpinion(greenTeam);
		this.energy += VoterThisRound / 2;
    	double certaintyThisTurn = persuasivity;
    	boolean newState = false;
    	double reward = 0;
    	double energyCostThisTurn = 0;
    	double[] currentState;
    	archive current = new archive();
    	int indexOfState = -1;
    	current.roundNum = this.roundNum;
    	if(this.roundNum == 0) {
    		newState = true;
    		currentState = evaluatestate(greenTeam, -1);
	    	indexOfState = 0;
	    	statePair newPair = new statePair(currentState);
	    	this.qTable.add(newPair);
	    	System.out.println("first round: No info from previous turn to display");
	    	System.out.println("current Voter: " + VoterThisRound);
    	}
    	else {	
    		double actualGain = VoterThisRound - (this.archive.get(roundNum - 1).Voter);
    		double actualLoss = (this.archive.get(roundNum - 1).eCost) - VoterThisRound / 2;
    		int indexOfPreState = this.archive.get(roundNum - 1).indexOfState;
    		predictedCertaintySwing -= 5 * ((this.archive.get(roundNum - 1).PredictedVoter) - actualGain); 
    		currentState = evaluatestate(greenTeam, red.archive.get(roundNum - 1).messagePotency);
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
        	reward =  actualGain * 10 - actualLoss * 3 + predictedCertaintySwing / 2;
        	Double[] predictedNextQ = predictedNextQ(qTable, currentState, indexOfState);
        	double qOld = this.qTable.get(indexOfPreState).qValues[preAction]; 
        	this.qTable.get(indexOfPreState).qValues[preAction] = qOld + this.learningRate * (reward + this.gamma * Collections.max(Arrays.asList(predictedNextQ)) - qOld);
        	System.out.println("Energy lost prev round (negative loss means gain): " + actualLoss);
            System.out.println("Voter gain prev round: " + actualGain);
            System.out.println("current Voter: " + VoterThisRound);
    	}
    	current.Voter = VoterThisRound;
    	current.indexOfState = indexOfState;	
    	this.predictedCertaintySwing = 0;
    	this.messagePotency = messageToPush(greenTeam, certaintyThisTurn, indexOfState, newState, this.energy);
    	current.messagePotency = this.messagePotency;
    	int length = greenTeam.length;
    	if (messagePotency!= 6) {
    		double results[] = messageAffect(messagePotency, certaintyThisTurn, length);
    		certaintyThisTurn = results[0];
        	if(certaintyThisTurn > 10)
        			certaintyThisTurn = 10;
        	energyCostThisTurn = results[1];
        	blueInteract(current,certaintyThisTurn, greenTeam, energyCostThisTurn, length); 	
        	System.out.println("Blue Sent out a Potency value of " + messagePotency + "\n");
    	}
    	else {
    		this.nGrayUsed++;
    		double results[] = new double[2];
    		int id = choseGrey(greyTeam);
    		if(greyTeam[id].worksFor == 0) {
    			 System.out.println("Deploy Grey Agent ");
    			 results = greyTeam[id].greyBlueTurn(greenTeam, current);
    		}
    		else {
    			this.betrayed++;
    			System.out.println("Deploy Grey Agent ");
    			System.out.println("Grey Agent has betrayed Blue " + "\n");
    			results = greyTeam[id].greyRedTurn(greenTeam, current);
    		}
    		predictedCertaintySwing = results[0];
    		this.energy -= results[1];
    		current.elevel = this.energy;
    		archive.add(current);
    		this.roundNum++;
    		
    	}
    }
    
	private int choseGrey(greyAgent[] greyTeam) {
		int size = greyTeam.length;
		Random rand = new Random();
		int chosen = -1;
		do {
		int randomNum = rand.nextInt(size);
		chosen = randomNum;
		}while(greyTeam[chosen].expired);
		greyTeam[chosen].expired = true;
		return chosen;
			
	}

	public void blueInteract(archive current, double certaintyThisTurn, greenAgent[] greenTeam, double energyCostThisTurn, int length) {
    	double certaintyUp = 0.0;
    	double PredictedVoterGain = 0;
    	double totalPredictedCertainty = 0.0;
    	for (int i = 0; i < length; i++) {
             if (!greenTeam[i].willVote) {
            	 double predictedCertainty = greenTeam[i].predictCertainty(1.0);
                 if(predictedCertainty > 10)
                	 predictedCertainty = 10;
                 if(predictedCertainty < 0)
                	 predictedCertainty = 0;
                 certaintyUp += (certaintyThisTurn * (1.0 + messagePotency  / 5.0)) * .95;
                 totalPredictedCertainty +=predictedCertainty;
            	 greenTeam[i].certainty = greenTeam[i].assignScore(length) * 15 +  (greenTeam[i].certainty)   - 0.95*(certaintyThisTurn *(1.0 + messagePotency / 5.0));           	 
            	 greenTeam[i].checkOpinion(certaintyThisTurn);
            	 if(!greenTeam[i].willVote) {
            		 PredictedVoterGain++;
            	 }
             }
            
             else {
            	 certaintyUp += .6 * ((messagePotency * (messagePotency/4.0)) + (certaintyThisTurn / 5.0));
            	 greenTeam[i].certainty += .6 * (messagePotency * (messagePotency/4.0)) + (certaintyThisTurn / 5.0);
            	 if(greenTeam[i].certainty > 10) {
            		 greenTeam[i].certainty = 10;
             	}
            	 
             }
             energyCostThisTurn+=.5;
    	 }
    	predictedCertaintySwing = certaintyUp - totalPredictedCertainty;
    	this.energy -= energyCostThisTurn;
    	current.elevel = this.energy;
    	current.eCost = energyCostThisTurn;
    	current.PredictedVoter = PredictedVoterGain;
    	archive.add(current);
    	this.roundNum++;
		
	}

	private boolean checkUseGrey(double currentEnergy, int size) {
    	if(this.nGrayUsed >= this.nGray)
    		return false;
		if(currentEnergy < size) {
    		return true;
    	}
    	Random rand = new Random();
    	if(rand.nextDouble() <= (this.useGrayChance - .05 * this.betrayed))
    		return true;
    	return false;
	}

	private Double[] predictedNextQ(ArrayList<statePair> qTable2, double[] state_next, int index) {
    	Double[] qs = new Double[6];
    	for (int i = 0; i < 6; i++)
    		qs[i]= qTable2.get(index).qValues[i];
    	return qs;
	}

	private double[] evaluatestate(greenAgent[] greenTeam2, double redPreAction) {
		double[] state = new double[3];
		double size = greenTeam2.length;
		state[0] = countOpinion(greenTeam2) / size;
		state[1] = this.energy / ((size * this.day) / 3);
		state[2] = redPreAction;
		return state;
	}
	
}
