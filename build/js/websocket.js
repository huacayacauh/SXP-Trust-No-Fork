/**
 * JavaScript for open and close WebSocket
 *            for send message to Java
 *            for catch message from Java
 * @author Michael DUBUIS
 */
var webSocket;
var serverAdress = "ws://localhost:8080/EchoChamber/serv";

// open a socket to communicate with the server
function openSocket(){
	if(webSocket !== undefined && webSocket.readyState !== WebSocket.CLOSED){
		writeResponse("WebSocket OK.");
		return;
	}
	webSocket = new WebSocket(serverAdress);
	webSocket.onmessage = function(event){
		serverReply(event.data);
	};
	webSocket.onclose = function(event){
	};
}

//Distributes to the proper function
function serverReply(data){
	data = JSON.parse(data);
	switch(data.query){
	case "registration":	registration(data.content); break;
	case "accountUpdated":	accountUpdated(data.content); break;
	case "login":			login(data.content); break;
	case "logout":			logout(data.content); break;
	case "itemRemoved":		itemRemoved(data.content); break;
	
	case "displayHome":		displayHome(data.content); break;
	case "displayItem":		displayItem(data.content); break;
	case "displayAccount":	displayAccount(data.content); break;
	case "displayContrat":	displayContrat(data.content); break;
	default: alert(data.query+" unknow !");
	}
}