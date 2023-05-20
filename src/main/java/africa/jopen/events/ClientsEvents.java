package africa.jopen.events;

import africa.jopen.models.Client;

public class ClientsEvents {
	private Client client;
	
	
	public ClientsEvents(Client client) {
		this.client = client;
	}
	
	public Client getClient() {
		return client;
	}
}
