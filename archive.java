public class archive {
	public int messagePotency;
	public int roundNum;
	public double NoneVoter;
	public double followers;
	public double PredictedNoneVoter;
	public double Voter;
	public double elevel;
	public double eCost;
	public double PredictedVoter;
	public int indexOfState;
	//public boolean useGray;
	public archive() {
		    this.messagePotency = 0;
		    this.roundNum = -1;
		    //this.useGray = false;
		    this.NoneVoter = 0;
		    this.followers = 0;
		    this.PredictedNoneVoter = 0;
		    this.Voter = 0;
		    this.elevel = 0;
		    this.eCost = 0;
		    this.PredictedVoter = 0;
		    this.indexOfState = -1;
		  }
	//public double effectiveMetric() {
	//	return (voterGained / (followerLost + 1));		
	//}
	
}
