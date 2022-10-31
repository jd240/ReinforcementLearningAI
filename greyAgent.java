/**
 * This class defines the properties of an agent on the grey class
 * The grey team can either work for the blue team or red team
 * but the blue and red does not know their loyalty until they are 
 * deploy
 */
public class greyAgent{
    // the team that the grey agent works for. 0: blue, 1: red
    int worksFor;
    int id;
    boolean expired;
    double persuasivity;
    
    //constructor for random grey agent
    public greyAgent(int id, int worksFor, double pursuaBlue){
        this.worksFor = worksFor;
        this.id = id;
        this.expired = false;
        this.persuasivity = pursuaBlue;
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
        	if(eCost > 0)
        		eCost = 0;
        	for (int i = 0; i < length; i++) {
                 if (greenTeam[i].willVote) {
                	 double predictedCertainty = greenTeam[i].predictCertainty(1.0);
                     if(predictedCertainty > 10)
                    	 predictedCertainty = 10;
                     if(predictedCertainty < 0)
                    	 predictedCertainty = 0;
                     certaintyUp += 1*(pursuaBlue *(1.0 + j / 5.0));
                     totalPredictedCertainty +=predictedCertainty;
                     predictedCertainty =  greenTeam[i].assignScore(length) * 15 +  (predictedCertainty) - .8*(pursuaBlue *(1.0 + j / 5.0));               	 
                     if(predictedCertainty <= 0) {
                		 voterGained++;
                	 }
                 }
                 else {
                	 certaintyUp += .6*(j * (j/4.0)) + (pursuaBlue / 5.0);
                 }
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
    
    public double[] greyBlueTurn(greenAgent[] greenTeam, archive current) {
        double [] values = new double[2];
    	double energyCostThisTurn = 0;
        double certaintyUp = 0.0;
    	double PredictedVoterGain = 0;
    	double totalPredictedCertainty = 0.0;
    	Double[] metric = predictFutureReward(greenTeam, 1 , 5, this.persuasivity);
		double max = metric[0];
		int index = 0;
        for (int i = 1; i < 5 ; i++) {
        	if (metric[i] > max){
                max = metric[i];
                index = i;
            }
        }
        int messagePotency = index + 1;
        double results[] = messageAffect(messagePotency, this.persuasivity, greenTeam.length);
		this.persuasivity = results[0];
    	if(this.persuasivity > 10)
    		this.persuasivity = 10;
    	energyCostThisTurn = results[1];
    	if(energyCostThisTurn > 0)
    		energyCostThisTurn = 0;
    	int length = greenTeam.length;
    	for (int i = 0; i < length; i++) {
             if (!greenTeam[i].willVote) {
            	 double predictedCertainty = greenTeam[i].predictCertainty(1.0);
                 if(predictedCertainty > 10)
                	 predictedCertainty = 10;
                 if(predictedCertainty < 0)
                	 predictedCertainty = 0;
                 certaintyUp += (this.persuasivity * (1.0 + messagePotency  / 5.0)) * 1;
                 totalPredictedCertainty +=predictedCertainty;
            	 greenTeam[i].certainty = greenTeam[i].assignScore(length) * 15 +  .8*(greenTeam[i].certainty)   - (this.persuasivity *(1.0 + messagePotency / 5.0));           	 
            	 greenTeam[i].checkOpinion(this.persuasivity);
            	 if(!greenTeam[i].willVote) {
            		 PredictedVoterGain++;
            	 }
             }
            
             else {
            	 certaintyUp += .6 * (messagePotency * (messagePotency/4.0)) + (this.persuasivity / 5.0);
            	 greenTeam[i].certainty += .6 * (messagePotency * (messagePotency/4.0)) + (this.persuasivity / 5.0);
            	 if(greenTeam[i].certainty > 10) {
            		 greenTeam[i].certainty = 10;
             	}
            	 
             }
    	 }
    	values[0] = certaintyUp - totalPredictedCertainty;
    	values[1] = energyCostThisTurn;
    	current.eCost = energyCostThisTurn;
    	current.PredictedVoter = PredictedVoterGain;
    	System.out.println("The grey agent sent out a Potency value of " + messagePotency + "\n");
		return values;
    }
    
    public double[] greyRedTurn(greenAgent[] greenTeam, archive current) {
    	double [] values = new double[2];
    	double PredictedNoneVoterGain = 0;
    	int messagePotency = 5;
    	double certaintyUp = 0.0;
    	double totalPredictedCertainty = 0.0;
    	int length = greenTeam.length;
    	for (int i = 0; i < length; i++) {
             if (greenTeam[i].willVote) {
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
            
             else {
            	 certaintyUp += (messagePotency * (messagePotency/4.0)) + (persuasivity / 5.0);
            	 greenTeam[i].certainty += (messagePotency * (messagePotency/4.0)) + (persuasivity / 5.0);
            	 if(greenTeam[i].certainty > 10) {
            		 greenTeam[i].certainty = 10;
             	}
            	 
             }
    	 }
    	values[0] = -(certaintyUp - totalPredictedCertainty);
    	values[1] = 0;
    	current.eCost = 0;
    	current.PredictedVoter = -PredictedNoneVoterGain;
    	System.out.println("The Spy sent out a Potency value of " + messagePotency + "\n");
		return values;
    }
}
