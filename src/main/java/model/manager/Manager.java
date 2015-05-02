package model.manager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import model.advertisement.AbstractAdvertisement;
import model.item.Category;
import model.item.Item;
import model.network.NetworkInterface;
import model.network.communication.service.ServiceListener;
import model.network.search.Search;
import model.user.User;
import net.jxta.discovery.DiscoveryService;

import org.jdom2.Element;

import util.StringToElement;

public class Manager extends AbstractAdvertisement implements ServiceListener<Manager> {
	
	private static final int RECURRING_ACCOUNT_NUMBER = 5;
	private static final long RECURRING_ACCOUNT_TIMEOUT = 3000;
	
	private HashMap<String, User> users;	// The string key is the user's public key in hexadecimal
	private ArrayList<Item> items;			// list of items handled by this manager.
	private NetworkInterface network;
	private User currentUser;				// User logged
	
	/**
	 * Create a manager based on a String that is XML formated.
	 * @param XML
	 */
	public Manager(String XML, NetworkInterface network) {
		super(XML);
		this.network = network;
	}

	public Manager(NetworkInterface network) {
		super();
		this.network = network;
	}

	/**
	 * to add an user in this instance of manager
	 * if user is already in the manager, this function check if this user is more recent
	 * @param u - User to add
	 */
	public void addUser(User u){
		if(u == null){
			System.err.println(this.getAdvertisementName()+" : This User is empty !");
			return;
		}
		if(!u.checkSignature(u.getKeys())){
			System.err.println(this.getAdvertisementName()+" : Bad Signature for "+u.getNick());
			return;
		}
		String key = u.getKeys().getPublicKey().toString(16);
		if(users.containsKey(key)){
			User existUser = users.get(key);
			if(existUser.getDate() <= u.getDate()){
				System.err.println(this.getAdvertisementName()+" : User "+u.getNick()+" is already registred !");
				return;
			}
			users.remove(key);
			users.put(key, u);
		}else
			users.put(key, u);
	}
	
	/**
	 * to add a item in this instance of manager
	 * if owner of the item isn't registered in this instance of manger, function will fail
	 * @param i - Item to add
	 */
	public void addItem(Item i){
		if(i == null){
			System.err.println(this.getAdvertisementName()+" : This Item is empty !");
			return;
		}
		String owner = i.getOwner();
		if(owner.isEmpty()){
			System.err.println(this.getAdvertisementName()+" : No owner found !");
			return;
		}
		if(!users.containsKey(owner)){
			System.err.println(this.getAdvertisementName()+" : Owner unknown for "+i.getTitle());
			return;
		}
		if(!i.checkSignature(users.get(owner).getKeys())){
			System.err.println(this.getAdvertisementName()+" : Bad Signature for "+i.getTitle());
			return;
		}
		if(items.contains(i)){
			System.err.println(this.getAdvertisementName()+" : Item "+i.getTitle()+" is already registred !");
			return;
		}
		// End exceptions
		items.add(i);
	}
	
	/**
	 * Get an XML string representing all the users that are saved on this device.
	 * @return A string, XML formated
	 */
	private String getUsersXML() {
		StringBuffer s = new StringBuffer();
		for(User u: users.values()) {
			s.append(u);
		}
		return s.toString();
	}
	
	/**
	 * Get an XML string representing all the items that are saved on this device.
	 * @return A string, XML formated
	 */
	private String getItemsXML() {
		StringBuffer s = new StringBuffer();
		for(Item i: items) {
			s.append(i); 
		}
		return s.toString();
	}
	
	/**
	 * Load all the users in this element
	 * @param e an element that contains users in XML format.
	 */
	private void loadUsers(Element e) {
		Element root = StringToElement.getElementFromString(e.getValue(), e.getName());
		for(Element u: root.getChildren()) {
			addUser(new User(u));
		}
	}
	
	/**
	 * Load all the items in this element
	 * @param e an element that contains items in XML format.
	 */
	private void loadItems(Element e) {
		Element root = StringToElement.getElementFromString(e.getValue(), e.getName());
		for(Element i: root.getChildren()) {
			addItem(new Item(i));
		}
	}
	
	///////////////////////////////////////////////// ADVERTISEMENT \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	@Override
	protected boolean handleElement(Element e) {
		switch(e.getName()) {
		case "users": loadUsers(e); break;
		case "items": loadItems(e); break;
		default: return false;
		}
		return true;
	}

	@Override
	protected String getAdvertisementName() {
		return Manager.class.getSimpleName();
	}

	@Override
	protected void setKeys() {
		users = new HashMap<String, User>();
		items = new ArrayList<Item>();
		currentUser = null;
		addKey("users", false);
		addKey("items", false);
	}
	
	@Override
	protected void putValues() {
		addValue("users", getUsersXML());
		addValue("items", getItemsXML());
	}

	/////////////////////////////////////////////// SERVICE LISTENER \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	@Override
	public void messageEvent(Manager m) {
		// TODO Tests
        Element elements;
    	// Add all Users
		elements = null;
    	elements  = StringToElement.getElementFromString(m.getUsersXML(), "users");
    	for (Element element : elements.getChildren()) {
			this.addUser(new User(element));
		}
		// Add all Items
		elements = null;
		elements  = StringToElement.getElementFromString(m.getItemsXML(), "items");
		for (Element element : elements.getChildren()) {
			this.addItem(new Item(element));
		}
	}
	
	////////////////////////////////////////////////////// UTIL \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	/**
	 * Remove an user if he haven't item !
	 * @param user
	 * @return
	 */
	public boolean removeUserIfNotItem(User user){
		String userKey = user.getKeys().getPublicKey().toString(16);
		if(!users.containsKey(userKey))
			return false;
		for (Item i : items) {
			if(i.getOwner().equals(userKey))
				return false;
		}
		users.remove(userKey);
		return true;
	}
	
	/**
	 * Remove an user and items of him.
	 * @param user
	 * @return
	 */
	public boolean removeUserWithItems(User user){
		String userKey = user.getKeys().getPublicKey().toString(16);
		if(!users.containsKey(user.getKeys().getPublicKey().toString(16)))
			return false;
		boolean valid = true;
		for (Item i : items) {
			if(i.getOwner().equals(userKey))
				valid &= items.remove(i);
		}
		return (valid &= (users.remove(userKey)!=null));
	}
	
	/**
	 * Remove an item from the Manager
	 * @param item
	 * @return
	 */
	public boolean removeItem(Item item){
		return items.remove(item);
	}
	
	/**
	 * to remove all items with lifeTime is over
	 */
	public void cleanItems(){
		for(int i = 0; i <items.size();i++){
			if(!items.get(i).isAlive())
				removeItem(items.get(i));
		}
	}
	
	///////////////////////////////////////////////////// GETTERS \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	/**
	 * Return the user with this key
	 * @param key - String format
	 * @return
	 */
	public User whoIs(String key){
		return users.get(key);
	}
	
	/**
	 * Return the user with this key
	 * @param key - BigInteger format
	 * @return
	 */
	public User whoIs(BigInteger key){
		return users.get(key.toString(16));
	}
	
	/**
	 * Return user who has this item
	 * @param item
	 * @return
	 */
	public User whoHas(Item item){
		return users.get(item.getOwner());
	}
	

	
	/**
	 * Retrieve the corresponding user according to nickname and password.
	 * @param nickname
	 * @param password
	 */
	public boolean login(String nickname, String password) {
		User u = null; // TODO get user on network or local and login.
		// Check password
		if(!u.isPassword(password))
			return false;
		// Check privateKey decryption
		// TODO Not sure if decrypt the private Key now
		if(!u.decryptPrivateKey(password))
			return false;
		currentUser = u;
		currentUser.setPassWord(password);
		return currentUser != null;
	}
	
	
	
	public void logout() {
		currentUser.setClearPassword(null);
		currentUser = null;
	}
	
	
	private void publishUsers() {
		DiscoveryService discovery = network.getGroup("users").getDiscoveryService();
		for(User u: users.values()) {
			try {
				discovery.flushAdvertisement(u);
				discovery.publish(u); //"i have this user"
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void publishItems() {
		
	}
	
	/**
	 * Publish (advertise) users and item on network. Also check data resilience and send data to other
	 * peers if needed.
	 */
	public void publishManager() {
		publishUsers();
		publishItems();
	}
	
	/**
	 * to update recurrence of current account
	 */
	public void checkDataResilience() {
		Search<User> search = new Search<User>(network.getGroup("users").getDiscoveryService(), "publicKey", true);
		// Wait 3 seconds or 5 results
		search.search(currentUser.getKeys().getPublicKey().toString(16), RECURRING_ACCOUNT_TIMEOUT, RECURRING_ACCOUNT_NUMBER);
		
		ArrayList<User> recurrentUser = search.getResults();
		long maxDate = 0;
		for (User user : recurrentUser) {
			if(!user.checkSignature(user.getKeys())){
				recurrentUser.remove(user);
			}else{
				maxDate = Long.compare(maxDate, user.getLastUpdated()) >= 0 ? maxDate : user.getLastUpdated();
			}
		}
		for (User user : recurrentUser) {
			if(user.getLastUpdated() < maxDate){
				// TODO Mise a jour du peer qui a envoye ce compte OU envoie d'un arret de diffusion.
			}
		}
		for(int i = 0 ; i < (recurrentUser.size() - RECURRING_ACCOUNT_NUMBER) ; i++){
			// TODO Envoyer une copie du compte a un peer aleatoire
		}
	}
	
	////////////////////////////////////////////////// MAIN FOR TEST \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
	public static void main(String[] args) {
		Manager manager = new Manager(null);
		User user1 = new User("user1", "pass2", "name1", "firstname1", "email1", "phone1");
		User user2 = new User("user2", "pass2", "name2", "firstname2", "email2", "phone2");
		Item item1 = new Item(user1, "patate", new Category(Category.CATEGORY.Appliances), 
				"osef", null, "france", "???", 145L, 1000L, Item.TYPE.WISH);
		Item item2 = new Item(user2, "carotte", new Category(Category.CATEGORY.Appliances), 
				"osef", null, "france", "???", 145L, 1000L, Item.TYPE.WISH);
		manager.addUser(user1);
		manager.addUser(user2);
		manager.addItem(item1);
		manager.addItem(item2);
		
		Manager manager2 = new Manager(manager.toString(), null);
		if(manager2.toString().equals(manager.toString())) {
			System.out.println("ok !");
		}
	}
}


