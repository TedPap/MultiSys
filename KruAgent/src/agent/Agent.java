package agent;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.BidIterator;
import negotiator.Deadline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.SortedOutcomeSpace;
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

	// TODO
	private static double MINIMUM_BID_UTILITY = 0.9;
	private static double MINIMUM_BIDMAP_UTILITY = 0.5;
	private static double CHILL_TIME = 0.6;
	private static double ANALIZE_HISTORY_THRESHOLD = 0.3;
	
	private double bidLimit = 1.0;
	private int boulwares = 0;
	private int coops = 0;
	private int conceders = 0;
	private SortedOutcomeSpace outcomeSpace;
	private Bid BestWeCanDo;
	
	private Bid lastReceivedBid = null;
	private AgentID lastReceivedID = null;
	// A map of all opponents and their bid history. Check Opponent class.
	private HashMap<AgentID, Opponent> enemies = new HashMap<AgentID, Opponent>();
	

	@Override
	public void init(NegotiationInfo info) {

		super.init(info);

		//System.out.println("Discount Factor is " + info.getUtilitySpace().getDiscountFactor());
		//System.out.println("Reservation Value is " + info.getUtilitySpace().getReservationValueUndiscounted());

		// if you need to initialize some variables, please initialize them
		// below
		
		outcomeSpace = new SortedOutcomeSpace(info.getUtilitySpace());
		BestWeCanDo = this.getHighestUtilityBid();

	}


	public Action chooseAction(List<Class<? extends Action>> validActions) {
		
		Bid randBid = null;
		try {
			randBid = this.getRandomBid();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(lastReceivedBid == null || !validActions.contains(Accept.class)) {
			// If we are first, offer our best bid.
			return new Offer(getPartyId(), BestWeCanDo);
		} 
		else {

			Bid bid = getNextBid();
			
			// Check if opponent's type is decided and change strategy.
			if(this.enemies.get(lastReceivedID).opponentType != null) {
				System.out.println(this.enemies.get(lastReceivedID).opponentType);
				
				if(boulwares >= 1 && conceders+coops == 0) {
					// TODO
					// With this we win or get a draw against boulware opponents(caduceus) in 1v1.
					return new Offer(this.getPartyId(), BestWeCanDo);
				}
				else if(boulwares >= 1 && conceders+coops >= 1) {
					// TODO
					//return new Offer(this.getPartyId(), BestWeCanDo);
				}
				else {
					// TODO
					//return new Offer(this.getPartyId(), BestWeCanDo);
				}
			}
			// When time is running out chill a bit.
			if(this.timeToChill()) {
				
				// Accept any bid above a minimum threshold.
				if(this.getUtility(lastReceivedBid) >= MINIMUM_BID_UTILITY) 
					return new Accept(getPartyId(), lastReceivedBid);
				
				// Offer the opponents' best bid if greater than MINIMUM_BID_UTILITY.
				Bid opBestBid = null;
				double bestBidVal = 0;
				Bid tempBestBid = null;
				for(Entry<AgentID, Opponent> entry : this.enemies.entrySet()) {
					tempBestBid = (Bid)((BidDetails) entry.getValue().getBidHistory().getBestBidDetails()).getBid();
					if(this.getUtility(tempBestBid) > bestBidVal) {
						bestBidVal = this.getUtility(tempBestBid);
						opBestBid = tempBestBid;
					}
				}
				if(this.getUtility(opBestBid) >= MINIMUM_BID_UTILITY) {
					return new Offer(this.getPartyId(), opBestBid);
				}
				
				// Return next bid closest to the bidLimit
				if(bid != null)
					return new Offer(this.getPartyId(), bid);
				
				// If all fails, offer a random(above a certain threshold) bid.
				return new Offer(this.getPartyId(), randBid);
			}
		}
		return new Offer(this.getPartyId(), BestWeCanDo);
	}
	
	
	// Example code that generates a random bid above a certain utility, check ExampleAgent for more.
	private Bid getRandomBid() throws Exception {
		HashMap<Integer, Value> values = new HashMap<Integer, Value>();
		
		List<Issue> issues = utilitySpace.getDomain().getIssues();
		Random randomnr = new Random();
		
		double limit = this.getTimeLine().getTime() >= 0.78 ? Math.exp(0.68-this.getTimeLine().getTime()) : 0.95;

		Bid bid = null;
		do {
			for(Issue lIssue : issues) {
				IssueDiscrete lIssueDiscrete = (IssueDiscrete) lIssue;
				int optionIndex = randomnr.nextInt(lIssueDiscrete.getNumberOfValues());
				values.put(lIssue.getNumber(), lIssueDiscrete.getValue(optionIndex));
			}
			bid = new Bid(utilitySpace.getDomain(), values);
		} while(getUtility(bid) < limit);

		return bid;
	}
	
	
	// Receive a message from the server, set globals and add it to the opponent's bid history.
	public void receiveMessage(AgentID sender, Action action) {
		super.receiveMessage(sender, action);
		if(action instanceof Offer) {
			lastReceivedBid = ((Offer) action).getBid();
			lastReceivedID = sender;
			BidDetails lastReceivedBidDetails = new BidDetails(lastReceivedBid, getUtility(lastReceivedBid), this.getTimeLine().getTime());
			
			// Add the last received bid to the history of its agent and initialize agent's bidMap.
			if(this.enemies.get(lastReceivedID) == null) {
				this.enemies.put(lastReceivedID, new Opponent(lastReceivedID));
				initEnemyBidMap();
			}
			this.enemies.get(lastReceivedID).addToHistory(lastReceivedBidDetails);
			
			//Fictitious Play. Increase num of times the bid has been offered
			if(getUtility(lastReceivedBid) > MINIMUM_BID_UTILITY ) {
				int i = this.enemies.get(lastReceivedID).bidMap.get(lastReceivedBid).intValue();
				i++;
				this.enemies.get(lastReceivedID).bidMap.replace(lastReceivedBid, new Integer(i));
			}
		}
		else if(action instanceof Accept) {
			lastReceivedBid = ((Accept) action).getBid();
			lastReceivedID = sender;
			this.enemies.get(lastReceivedID).acceptanceThreshold = this.getUtility(lastReceivedBid);
			this.enemies.get(lastReceivedID).acceptedBid = lastReceivedBid;
		}
		
		double time = this.getTimeLine().getTime();
		if(time > ANALIZE_HISTORY_THRESHOLD)
			analyzeHistory();
	}

	
	public void analyzeHistory() {

		BidHistory bh = this.enemies.get(lastReceivedID).getBidHistory();
		BidDetails bid_d1 = bh.getWorstBidDetails();
		double util1 = getUtility(bid_d1.getBid());
		double util2 = bh.getAverageUtility();
		
		BidHistory belowAvgBidSet = bh.filterBetweenUtility(util1-0.001, util2+0.0001);
		double belowAvgBidProbability = (double)belowAvgBidSet.size() / (double)bh.size();
		
		if(this.enemies.get(lastReceivedID).opponentType == null) {
			if(belowAvgBidProbability >= 0.65) {
				this.enemies.get(lastReceivedID).setType("boulware");
				boulwares += 1;
			}
			else if(belowAvgBidProbability < 0.65 && belowAvgBidProbability >= 0.4) {
				this.enemies.get(lastReceivedID).setType("co-op");
				coops += 1;
			}
			else {
				this.enemies.get(lastReceivedID).setType("conceder");
				conceders += 1;
			}
		}
		
		System.out.println("ID: "+lastReceivedID+" bidhist size: "+bh.size() +" P="+belowAvgBidProbability);
		// System.out.println(belowAvgBidSet.size() );
		// System.out.println(bh.size() );
		// System.out.println((double)belowAvgBidSet.size() );
		// System.out.println((double)bh.size() );
	}
	
	
	//Initialize the Fictitious Play bid map of last enemy.
	public void initEnemyBidMap() {
		BidIterator bi = new BidIterator(this.utilitySpace.getDomain());
		while(bi.hasNext()) {
			Bid bid = bi.next();
			if(getUtility(bid) > MINIMUM_BIDMAP_UTILITY) {
				this.enemies.get(lastReceivedID).bidMap.put(bid, new Integer(0));
			}
		}
	}
	
	
	public Bid getNextBid() {
		if(this.getTimeLine().getTime() > CHILL_TIME + 0.2D) {
			this.bidLimit = this.getTimeLine().getTime() >= CHILL_TIME ? Math.exp(CHILL_TIME+0.2D-this.getTimeLine().getTime()) : 0.95;
			if(this.bidLimit >= MINIMUM_BID_UTILITY-0.15)
				return outcomeSpace.getBidNearUtility(this.bidLimit).getBid();
		}
		else {
			this.bidLimit = this.getTimeLine().getTime() >= CHILL_TIME ? Math.exp(CHILL_TIME-this.getTimeLine().getTime()) : 0.95;
			if(this.bidLimit >= MINIMUM_BID_UTILITY-0.15)
				return outcomeSpace.getBidNearUtility(this.bidLimit).getBid();
		}
		return BestWeCanDo;
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

	
	// Checks whether a certain point in time(CHILL_TIME) has passed.
	private Boolean timeToChill() {
		if(this.getTimeLine().getTime() < CHILL_TIME )
			return false;
		return true;
	}
	
	
	public String getDescription() {
		return "FART";
	}

}
