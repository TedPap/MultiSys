package agent;

import java.util.ArrayList;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.bidding.BidDetails;
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.utility.UtilitySpace;

public class Opponent {
	public AgentID opponentID;
	public BidHistory bidHistory = new BidHistory();
	
	// Constructor.
	public Opponent(AgentID id) {
		this.opponentID = id;
	}
	
	// Get the opponent's bid history.
	public BidHistory getBidHistory() {
		return this.bidHistory;
	}
	
	// Add a bid to the opponent's bid history.
	public void addToHistory(BidDetails bid) {
		this.bidHistory.add(bid);
	}

}
