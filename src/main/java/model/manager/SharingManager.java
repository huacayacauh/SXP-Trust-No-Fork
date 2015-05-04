package model.manager;

import java.util.ArrayList;

import net.jxta.peer.PeerID;
import model.network.NetworkInterface;
import model.network.communication.Communication;
import model.network.search.RandomPeerFinder;
import model.network.search.Search;
import model.user.User;

/**
 * Manager for sharing ressource (users, items ..) when needed.
 * Also check data resiliance (replication) on the network.
 * @author Julien Prudhomme
 * @author Michael Dubuis
 * 
 */
public class SharingManager {
	private Manager manager; //local manager
	private NetworkInterface network; //Jxta network
	private int replications; //number of account replication on the network
	private int checkTime; //intervals for checking data's replication
	private boolean continueThread = false; //boolean to start/stop the thread.
	private Thread thread = null;
	private Communication com = null;
	
	
	/**
	 * Create a new Sharing manager.
	 * The sharing manager will automaticelly check if the datas (users accounts) are
	 * enough replicated on the network.
	 * Call startSharing to start the sharing thread.
	 * You should call the stopSharing method before exiting the application.
	 * @param manager The local ressource manager.
	 * @param network The network interface with Jxta.
	 * @param replications Number of account replication that would be enough on the network.
	 * @param checkTime The amount of time (in minutes) between 2 data replication
	 */
	public SharingManager(Manager manager, NetworkInterface network, Communication com, int replications, int checkTime) {
		this.manager = manager;
		this.network = network;
		this.replications = replications;
		this.com = com;
		this.checkTime = checkTime;
	}
	
	/**
	 * Start 
	 */
	public void startSharing() {
		if(thread != null) {
			throw new RuntimeException("Thread is already running");
		}
		this.continueThread = true;

		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				while(continueThread) {
						checkDataResilience();
					try {
						Thread.sleep(checkTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
			}
			
		});
		thread.start();
	}
	
	public void stopSharing() {
		continueThread = false;
		if(thread == null) return;
		while(thread.isAlive()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		thread = null;
	}
	
	/**
	 *  Check user data replication on the network.
	 * @param publicKey The user to check.
	 */
	private void checkUserResilience(String publicKey) {
		try {
			Search<User> search = new Search<User>(network.getGroup("users").getDiscoveryService(), "publicKey", true);
			// Wait 3 seconds or "replications" results
			search.search(publicKey, 3000, this.replications);
			ArrayList<Search<User>.Result> results = search.getResultsWithPeerID();
			User user = manager.whoIs(publicKey);
			long maxDate = user.getLastUpdated();
			for (Search<User>.Result r : results) {
				if(!r.result.checkSignature(r.result.getKeys())){
					results.remove(r);
				}else{
					if(Long.compare(maxDate, r.result.getLastUpdated()) < 0){
						// If a result is more recent than mine
						maxDate = r.result.getLastUpdated();
						user = r.result;
						if(!manager.getCurrentUser().equals(user))
							manager.addUser(user);
						else{
							// FATAL ERROR : FAILLE DE SECURITE (QUELQU'UN A REUSSI A MODIFIER MON COMPTE)
						}
					}
				}
			}
			for (Search<User>.Result r : results) {
				if(r.result.getLastUpdated() < maxDate){
					// TODO service "updaterUsers"
					com.sendMessage(manager.UserItemXMLString(publicKey), "TransmitAccountService", r.peerID);
				}
			}
			if((results.size() - this.replications) > 0){
				RandomPeerFinder finder = new RandomPeerFinder(network);
				finder.findPeers(3000, (results.size() - this.replications));
				PeerID[] randomPeers = new PeerID[finder.getResults().size()]; 
				randomPeers = finder.getResults().toArray(randomPeers);
				com.sendMessage(manager.UserItemXMLString(publicKey), "TransmitAccountService", randomPeers);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void checkDataResilience() {
		for(User u: manager.getUsers()) {
			checkUserResilience(u.getKeys().getPublicKey().toString(16));
		}
	}
	
}
