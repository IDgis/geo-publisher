package nl.idgis.publisher.provider.messages;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Connect implements Serializable {
	
	private static final long serialVersionUID = 3942969994183376964L;
	
	private final InetSocketAddress address;
	
	public Connect(InetSocketAddress address) {
		this.address = address;
	}	
	
	public InetSocketAddress getAddress() {
		return address;
	}

	@Override
	public String toString() {
		return "Connect [address=" + address + "]";
	}
}
