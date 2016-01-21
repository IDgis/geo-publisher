package util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implements a basic IP based access filter.
 * 
 * @author Reijer Copier
 *
 */
public class InetFilter {
	
	private static class ConfigElement {
		
		String host;
		
		String prefix;
		
		ConfigElement(String host) {
			this(host, null);
		}
		
		ConfigElement(String host, String prefix) {
			this.host = Objects.requireNonNull(host);
			this.prefix = prefix;
		}
		
		String getHost() {
			return host;
		}
		
		Optional<String> getPrefix() {
			return Optional.ofNullable(prefix);
		}
	}
	
	private static class FilterElement {
		
		byte[] address, mask;
		
		FilterElement(byte[] address, byte[] mask) {
			this.address = Objects.requireNonNull(address);
			this.mask = Objects.requireNonNull(mask);
			
			if(address.length != mask.length) {
				throw new IllegalArgumentException("address and mask length don't match");
			}
			
			for(int i = 0; i < address.length; i++) {
				this.address[i] &= this.mask[i];
			}
		}
		
		boolean isAllowed(byte[] address) {
			if(address.length == this.address.length) {
				for(int i = 0; i < address.length; i++) {
					if((address[i] & this.mask[i]) != this.address[i]) {
						return false;
					}
				}
				
				return true;
			}
			
			return false;
		}
	}
	
	private static class ConfigException extends RuntimeException {

		private static final long serialVersionUID = 3010816705411803028L;
		
		ConfigException(String message) {
			super(message);
		}
		
		ConfigException(String message, Exception e) {
			super(message, e);
		}
	}
	
	private final List<FilterElement> config;

	public InetFilter(String config) {
		try {
			this.config = Arrays.asList(config.split(",")).stream()
				.map(configElement -> {
					String[] elementSplit = configElement.split("/");
					if(elementSplit.length == 2) {					
						return new ConfigElement(elementSplit[0], elementSplit[1]);
					} else {
						return new ConfigElement(configElement);
					}
				})
				.map(configElement -> {
					try {
						byte[] address = InetAddress.getByName(configElement.getHost()).getAddress();
						int addressLength = address.length * 8;
						
						int maskLength = configElement.getPrefix().map(prefix -> {
							try {
								int prefixInt = Integer.parseInt(prefix);
								if(prefixInt > addressLength) {
									throw new ConfigException("prefix larger than address length: " + prefix);
								}
								
								return prefixInt;
							} catch(NumberFormatException e) {
								throw new ConfigException("invalid prefix", e);
							}
						}).orElse(addressLength);
						
						byte[] mask = new byte[address.length];
						fillMask(mask, maskLength);
						
						return new FilterElement(address, mask);
					} catch(UnknownHostException e) {
						throw new ConfigException("invalid host or ip address", e);
					}
				})
				.collect(Collectors.toList());
		} catch(ConfigException e) {
			throw new IllegalArgumentException("Invalid filter configuration", e);
		}
	}

	private void fillMask(byte[] mask, int maskLength) {
		Arrays.fill(mask, (byte)0);
		
		int pos = 0;
		while(maskLength >= 8) {
			mask[pos++] = (byte)0xff;
			maskLength -= 8;
		}
		
		if(maskLength > 0) {
			mask[pos] = (byte)((byte)0xff << (8 - maskLength));
		}
	}
	
	public boolean isAllowed(InetAddress address) {
		for(FilterElement element : config) {
			if(element.isAllowed(address.getAddress())) {
				return true;
			}
		}
		
		return false;
	}
}
