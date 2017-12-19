package agent;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.Deadline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.bidding.BidDetails;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.Value;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;
import negotiator.persistent.PersistentDataContainer;
import negotiator.utility.AbstractUtilitySpace;

public class Agent extends AbstractNegotiationParty {

	private double MINIMUM_BID_UTILITY = 0.7;
	private double CHILL_TIME = 0.8;
	
	private Bid lastReceivedBid = null;
	private AgentID lastReceivedID = null;
	private String opponentType = null;
	// A map of all opponents and their bid history. Check Opponent class.
	private HashMap<AgentID, Opponent> enemies = new HashMap<AgentID, Opponent>();
	

	@Override
	public void init(NegotiationInfo info) {

		super.init(info);

		//System.out.println("Discount Factor is " + info.getUtilitySpace().getDiscountFactor());
		//System.out.println("Reservation Value is " + info.getUtilitySpace().getReservationValueUndiscounted());

		// if you need to initialize some variables, please initialize them
		// below

	}


	public Action chooseAction(List<Class<? extends Action>> validActions) {
		
		Bid BestWeCanDo = this.getHighestUtilityBid();
		Bid randBid = null;
		try {
			randBid = this.getRandomBid();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (lastReceivedBid == null || !validActions.contains(Accept.class)) {
			// If we are first, offer our best bid.
			return new Offer(getPartyId(), BestWeCanDo);
		} 
		else {
			// Accept any bids better than ours.
			if(getUtility(lastReceivedBid) >= getUtility(BestWeCanDo)) {
				return new Accept(getPartyId(), lastReceivedBid);
			}
			// Check if opponent's type is decided and change strategy.
			else {
				if(opponentType != null) {
					if(opponentType.equals("boulware")) {
						// TODO
						return new Offer(this.getPartyId(), BestWeCanDo);
					}
					else if(opponentType.equals("conceder")) {
						// TODO
						return new Offer(this.getPartyId(), BestWeCanDo);
					}
					else if(opponentType.equals("co-op")){
						// TODO
						return new Offer(this.getPartyId(), BestWeCanDo);
					}
					else {
						// TODO
						return new Offer(this.getPartyId(), BestWeCanDo);
					}
				}
				// When time is running out chill a bit.
				if(this.timeToChill()) {
					
					// Accept any bid above a minimum threshold.
					if(this.getUtility(lastReceivedBid) >= MINIMUM_BID_UTILITY) 
						return new Accept(getPartyId(), lastReceivedBid);
					
					// Offer the opponents' best bid if greater than MINIMUM_BID_UTILITY.
					// TODO
					Bid opBestBid = null;
					if(this.enemies.get(lastReceivedID) != null)
						opBestBid = (Bid)((BidDetails) this.enemies.get(lastReceivedID).getBidHistory().getBestBidDetails()).getBid();
					if(this.getUtility(opBestBid) >= MINIMUM_BID_UTILITY) {
						return new Offer(this.getPartyId(), opBestBid);
					}
					
					// If all fails, offer a random(above a certain threshold) bid.
					return new Offer(this.getPartyId(), randBid);
				}
				
				return new Offer(this.getPartyId(), BestWeCanDo);
			}
		}
	}
	
	// Wrapper for getMaxUtilityBid, so we can handle the exception.
	private Bid getHighestUtilityBid() {
		try {
			Bid bid = utilitySpace.getMaxUtilityBid();
			return bid;
		} catch (Exception e) {
			e.printStackTrace();
            return null;
		}
	}

	// Example code that generates a random bid above a certain utility, check ExampleAgent for more.
	private Bid getRandomBid() throws Exception {
		HashMap<Integer, Value> values = new HashMap<Integer, Value>();
		
		List<Issue> issues = utilitySpace.getDomain().getIssues();
		Random randomnr = new Random();
		double limit = this.getTimeLine().getTime() >= 0.78 ? Math.exp(0.78-this.getTimeLine().getTime()) : 0.95;
		//System.out.println(limit);

		Bid bid = null;
		do {
			for (Issue lIssue : issues) {
				IssueDiscrete lIssueDiscrete = (IssueDiscrete) lIssue;
				int optionIndex = randomnr.nextInt(lIssueDiscrete.getNumberOfValues());
				values.put(lIssue.getNumber(), lIssueDiscrete.getValue(optionIndex));
			}
			bid = new Bid(utilitySpace.getDomain(), values);
//		} while (getUtility(bid) < MINIMUM_BID_UTILITY);
		} while(getUtility(bid) < limit);

		return bid;
	}
	
	// Checks whether a certain point in time(CHILL_TIME) has passed.
	private Boolean timeToChill() {
		if(this.getTimeLine().getTime() < CHILL_TIME )
			return false;
		return true;
	}

	public void analyzeHistory() {

		BidHistory bh = this.enemies.get(lastReceivedID).getBidHistory();
		BidDetails bid_d1 = bh.getWorstBidDetails();
		double util1 = bid_d1.getMyUndiscountedUtil();
		double util2 = bh.getAverageUtility();
		double dif = util2-util1;
		//System.out.println(util1 + "  " + util2 + "  " + dif);
		
		double offset = util1 + dif;
		BidHistory worstBidSet = bh.filterBetweenUtility(util1-0.01, util2);
		double worstBidProbability = (double)worstBidSet.size() / (double)bh.size();
		System.out.println(bh.size() +" , "+worstBidProbability);
		//System.out.println(worstBidSet.size() );
		//System.out.println(bh.size() );
	}
	
	// Receive a message from the server, set globals and add it to the opponent's bid history.
	public void receiveMessage(AgentID sender, Action action) {
		super.receiveMessage(sender, action);
		if (action instanceof Offer) {
			lastReceivedBid = ((Offer) action).getBid();
			lastReceivedID = sender;
			BidDetails lastReceivedBidDetails = new BidDetails(lastReceivedBid, getUtility(lastReceivedBid), this.getTimeLine().getTime());
			
			// Add the last received bid to the history of its agent.
			if(this.enemies.get(lastReceivedID) == null)
				this.enemies.put(lastReceivedID, new Opponent(lastReceivedID));
			this.enemies.get(lastReceivedID).addToHistory(lastReceivedBidDetails);
		}
		else if(action instanceof Accept) {
			//System.out.println("ACCEPTANCE");
			lastReceivedBid = ((Accept) action).getBid();
			lastReceivedID = sender;
			this.enemies.get(lastReceivedID).acceptanceThreshold = this.getUtility(lastReceivedBid);
			this.enemies.get(lastReceivedID).acceptedBid = lastReceivedBid;
			//System.out.println(this.enemies.get(lastReceivedID).acceptanceThreshold);
		}
		double time = this.getTimeLine().getTime();
		if (time>0.01) analyzeHistory();
	}


	public String getDescription() {
		return "FART";
	}

}
