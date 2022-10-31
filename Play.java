import java.util.*;

public class Play{
    // number of days in the election campaign
    int ElectionDay;
    // the day into the election campaign
    int day;
    // our agents
    greenAgent[] greenTeam;
    greyAgent[] greyTeam;
    redAgent redAgent;
    blueAgent2 blue;
    //chance of connections
    double prob;

    /**
     * constructor that creates a random network
     * @param days the number of days the election runs for
     * @param nGreen the number of green agents on the green team
     * @param nGrey the number of grey agents on the grey team
     * @param prob the probability of connection between two green agents
     * @param prop the proportion of grey agents on blue team
     * @param humanBlue determine if blue is played by a human
     * @param humanRed determine if red is played by a human
     */
    public Play (int days, int nGreen, int nGrey, double prob, double prop, boolean humanBlue, boolean humanRed){
    	this.prob = prob;
    	this.ElectionDay = days;
    	if (nGreen > 40000 || this.prob > 0.1) {
    		System.out.println("Either your connection probability (Max 0.1)"
    				+ " or your number of green nodes (max 40000) is too big");
			System.exit(0);
    	}
    	if (nGreen <= 0 || this.ElectionDay <= 0) {
    		System.out.println("Either day until election or "
    				+ "your number of green nodes is negative");
			System.exit(0);
    	}
    	this.redAgent = new redAgent();
        this.blue = new blueAgent2(days, nGreen, nGrey);
        // Initialize array of grey team members
        this.greyTeam = new greyAgent[nGrey];
        // Initialize array of green team members 
        this.greenTeam = new greenAgent[nGreen];
        for(int i = 0; i < nGreen; i++)
            greenTeam[i] = new greenAgent(i);
    	Random rand = new Random();
  	
    	// Populating the Grey and Green Team
    	for(int i = 0; i < nGrey; i++) {
    		double probability = rand.nextDouble();
    		if(probability <= prop)
    			greyTeam[i] = new greyAgent(i, 0,blue.persuasivity);
    		else
    			greyTeam[i] = new greyAgent(i, 1,blue.persuasivity);
    	}
    	int[][] pairsConnected = new int[nGreen][nGreen];
        for(int i = 0; i < nGreen; i++){
        	int size =  0;
            for(int j = 0; j < nGreen; j++){
            	if(size > greenTeam[i].LimittingFactor)
            		break;
            	if( greenTeam[j].neighbors.size() > greenTeam[j].LimittingFactor)
            		continue;
            	if(rand.nextDouble() <= this.prob 
            		&& pairsConnected[i][j] == 0 
            		&& i != j){
                    size++;
            		greenTeam[i].neighbors.add(greenTeam[j].id);
                    greenTeam[j].neighbors.add(greenTeam[i].id);
                    pairsConnected[i][j] = 1;
                    pairsConnected[j][i] = 1;
                }
            }
        }
        //assign family and social scores to Green team
        for (int i = 0; i < nGreen; i++) {
        	greenTeam[i].assignFamily();
        	greenTeam[i].socialScore = greenTeam[i].assignScore(nGreen);
        }
        this.day = 0;
    }

    // Helper function to determine who goes first
    public int whoGoesFirst() {
    	Random ran = new Random(); 
        int nxt = ran.nextInt(2);
        if (nxt == 0) {
        	System.out.println("Blue goes after Green");
        	return nxt;
        }
        System.out.println("Red goes after Green");
        return nxt;
    }
    
   // Deploying the correct Red agent (AI or Human) 
    private void DeployRed (boolean humanRed) {
    	if(humanRed) 
    		redAgent.redHumanTurn(greenTeam);
    	else
    		redAgent.redTurn(greenTeam, blue);
    }
    
    // Deploying the correct Blue agent (AI or Human) 
    private void DeployBlue (boolean humanBlue) {
    	if(humanBlue) 
    		blue.blueHumanTurn(greenTeam, greyTeam);
    	else
    		blue.blueTurn(greenTeam, greyTeam, redAgent);
    }
    
    //Run the simulation and determine who is the winner
    public void GamePlay(boolean humanBlue, boolean humanRed){
        int goFirst = whoGoesFirst();
		for (int i = 0; i < this.ElectionDay; i++) {
			day++;
            System.out.println("Day " + day);
        	greenAgent.greenTurn(greenTeam, blue, redAgent);
        	if(goFirst == 0) {
        		if (blue.energy > 0) {
        			DeployBlue (humanBlue); 
        		}
        		else {
        			System.out.println("Blue out of energy, red wins");
        			return;
        		}
        		DeployRed (humanRed);
        	}
        	else{
        		DeployRed (humanRed);
    			if (blue.energy > 0) {
        			DeployBlue (humanBlue); 
        		}
        		else {
        			System.out.println("Blue out of energy, red wins");
        			return;
        		}
        		
        	}
    	}
    	double percent = blue.countOpinion(greenTeam) / greenTeam.length;
    	if(percent > .5)
    		System.out.println("blue win");
    	else {
    		System.out.println("red win");
    	}
    }
    
    public static void printUsage(){
        System.out.println("Usage: java Play <days in the election (ideally 50 or above)> \n"
        		+ "<size of green team> <size of grey team> \n"
        		+ "<network % density (max 0.1)> \n"
        		+ "<% of greys on blue team> \n"
        		+ "<Human Blue Player?> \n"
        		+ "<Human Red Player?>");
    }

    public static void main(String[] args){
    	// print usage information
        if(args.length < 7){
            printUsage();
            System.exit(0);
        }
    	
        String s1= args[5];  
    	String s2= args[6];  
    	boolean b1=Boolean.parseBoolean(s1);  
    	boolean b2=Boolean.parseBoolean(s2);  
    	// the game initial state of the game;
        Play game = new Play(Integer.valueOf(args[0]), Integer.valueOf(args[1]), 
        		Integer.valueOf(args[2]), Double.valueOf(args[3]), Double.valueOf(args[4]), b1, b2);
        long start = System.nanoTime();
        game.GamePlay(b1, b2);
        long end = System.nanoTime();
        long execution = end - start;
        System.out.println("Execution time: " + execution + " nanoseconds");
    }
}