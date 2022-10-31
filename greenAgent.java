/**
 * This class defines the properties of the Green Agent
 * They make up the majority of the population, and are
 * the main target that both the Red and the Blue is 
 * trying to win over. All the green agents are connected 
 * via a network, and they might or might not be neighbors/family
 * member of each other.
 */
import java.util.*;
public class greenAgent {
	double socialScore;
	// an adjacency list of the green agents that this agent can 'talk' to
    ArrayList<Integer> neighbors;
    HashSet<Integer> family;
    // true if the agent will be voting in the election, false otherwise
    boolean willVote;
    // higher uncertainty means higher probability the agents opinion will change (0 to 10)
    double certainty;
    // this agents unique id
    int id;
    // determines if the red team has 'lost' this green agent, i.e. if false red can no longer communicate
    boolean redFollower;
    int LimittingFactor;

    /**
     * constructor for green agent that sets random parameters for willVote and uncertainty.
     */
     public greenAgent(int id){
        this.neighbors = new ArrayList<Integer>();
        this.family = new HashSet<Integer>();
        Random rand = new Random();
        this.socialScore = 0;
        // exactly 50% of green agents start voting
        if(id % 2 == 0)
            willVote = true;
        else
            willVote = false;
        // choose double between 0 and 10
        int rangeMin=0;
        int rangeMax=10;
        this.certainty = rangeMin + (rangeMax - rangeMin) * rand.nextDouble();
        this.id = id;
        this.redFollower = true;
        int min = 1;
 		int max = 101;
 		this.LimittingFactor = rand.nextInt(max - min) + min;
     }
     
     
     public boolean unfollowed(int potency, double redPersua) {
    	double chance;
    	if(potency == 1 || potency == 2 || potency == 3)
    		chance = (this.certainty / 12.0) * ((potency-(potency / 2.0)) / 20.0);
    	else
    		chance = ((this.certainty / 12.0) * (potency / (17.0-potency)));
      	Random rand = new Random();
     	double probability = rand.nextDouble();
     	if(probability <= chance)
     		return true;
     	return false;
     	
     }
  
     public double predictCertainty(double interval) {
    	 Random rand = new Random();
    	 double rangeMin = this.certainty - interval;
         double rangeMax = this.certainty + interval;
         double predictedCertainty =  rangeMin + (rangeMax - rangeMin) * rand.nextDouble();
         return predictedCertainty;
     }
     
     private static boolean canInteract(greenAgent a, greenAgent b) {
         Random rand = new Random();
    	 double probability = rand.nextDouble();
    	 double interactScore = a.socialScore * b.socialScore;
    	 if(isFamily(a,b))
    		 interactScore += .5;
    	 if(probability <= interactScore)
    		 return true;
    	 return false;
    	 
     }
     
     private static boolean isFamily(greenAgent a, greenAgent b) {
     	
    	if(a.family.contains(b.id))
     		return true;
     	return false;
 	}
     
     
     public void assignFamily() {
    	Random rand = new Random();
    	int size = this.neighbors.size();
    	for (int i = 0; i < size; i++) {
    		double probability = rand.nextDouble();
    		if(probability <= 0.05)
    			this.family.add(this.neighbors.get(i));
    	}
     }
     
     public void checkOpinion (double potency) {
    	 if(this.certainty <= 0) {
    		 this.willVote = !this.willVote;
    	 	 this.certainty = (0 - this.certainty) + potency / 3;
    	 }
     }

	private static double opinionScore(greenAgent a) {
    	 double score = a.socialScore + (a.certainty / 10);
    	 return score;
     }
     
     public double assignScore(int networkSize) {
		double numNeighbors = this.neighbors.size();
		double score = (numNeighbors / networkSize) + (this.certainty / 20); 
		return score;
	}

	public static void greenTurn(greenAgent[] greenTeam, blueAgent2 blue, redAgent red){
		System.out.println("Green Team Turn");
		for(int i = 0; i < greenTeam.length; i++) {
			int size = greenTeam[i].neighbors.size();
			for (int j = 0; j < size; j++) {
				int NeighBorId = greenTeam[i].neighbors.get(j);
				greenAgent a = greenTeam[i];
				greenAgent b = greenTeam[NeighBorId];
				if(canInteract(a,b)) {
					if(a.willVote != b.willVote) {
						if(opinionScore(a) > opinionScore(b)) {
							double swing = (a.certainty - b.certainty) / 2;
							b.certainty -= (a.certainty / 2) + swing;
							if(b.certainty < 0) {
								b.willVote = a.willVote;
								b.certainty = 0 - b.certainty + a.certainty / 10; 
							}
						}
						else if(opinionScore(a) < opinionScore(b)) {
							double swing = (b.certainty - a.certainty) / 2;
							a.certainty -= (b.certainty / 2) + swing;
							if(a.certainty < 0) {
								a.willVote = b.willVote;
								a.certainty = 0 - a.certainty + a.certainty / 10; 
							}
						}
					}
					else {
							a.certainty += (b.certainty + a.certainty) / 50;
							b.certainty += (b.certainty + a.certainty) / 50;
							if(b.certainty > 10)
								b.certainty = 10;
							if(a.certainty > 10)
								a.certainty = 10;
						
					}
				}
			}
		}
		
		 System.out.println("current Voter: " + blue.countOpinion(greenTeam));
		 System.out.println("current NoneVoter: " + red.countOpinion(greenTeam) + "\n");
	}
}
