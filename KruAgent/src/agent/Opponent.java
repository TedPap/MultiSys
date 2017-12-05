package agent;

import java.util.ArrayList;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.utility.UtilitySpace;

public class Opponent {
	public AgentID opponentID;
	public ArrayList<Bid> bidHistory = new ArrayList<Bid>();
	
	// Constructor.
	public Opponent(AgentID id) {
		this.opponentID = id;
	}
	
	// Get the opponent's bid history.
	public ArrayList<Bid> getHistory() {
		return this.bidHistory;
	}
	
	// Add a bid to the opponent's bid history.
	public void addToHistory(Bid bid) {
		this.bidHistory.add(bid);
	}

}
