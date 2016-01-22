package util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>Implements a basic IP based access filter.</p>
 * 
 * <p>Configuration format example:</p> 
 * 
 * <p>'192.0.2.1,198.51.100.0/24,2001:db8:1::1,2001:db8:2::/64'</p>
 * 
 * <p>IPv4 host: 192.0.2.1<br>
 * IPv4 network: 198.51.100.0/24 (netmask: 255.255.255.0)</p>
 * 
 * <p>IPv6 host: 2001:db8:1::1<br>
 * IPv6 network: 2001:db8:2::/64</p>
 * 
 * @author Reijer Copier
 *
 */
public class InetFilter {
	
	/**
	 * Helper class, used in the first stage
	 * of the configuration parser.
	 */
	private static class ConfigElement {
		
		String host;
		
		String maskLength;
		
		ConfigElement(String host) {
			this(host, null);
		}
		
		ConfigElement(String host, String maskLength) {
			this.host = Objects.requireNonNull(host);
			this.maskLength = maskLength;
		}
		
		String getHost() {
			return host;
		}
		
		Optional<String> getMaskLength() {
			return Optional.ofNullable(maskLength);
		}
	}
	
	/**
	 * A single FilterElement of a configured {@link InetFilter}.
	 * 
	 * @author Reijer Copier
	 *
	 */
	public static class FilterElement {
		
		// redundant and only used by toString
		// isAllowed uses mask instead 
		private final int maskLength;
		
		private final byte[] address, mask;
		
		private FilterElement(byte[] address, byte[] mask, int maskLength) {
			this.address = Objects.requireNonNull(address);
			this.mask = Objects.requireNonNull(mask);
			this.maskLength = maskLength;
			
			if(address.length != mask.length) {
				throw new IllegalArgumentException("address and mask length don't match");
			}
			
			for(int i = 0; i < address.length; i++) {
				this.address[i] &= this.mask[i];
			}
		}
		
		private boolean isAllowed(byte[] address) {
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
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("FilterElement [");
			
			try {
				InetAddress inetAddress = InetAddress.getByAddress(address);
				sb
					.append(inetAddress.getHostAddress())
					.append("/")
					.append(maskLength);
			} catch(UnknownHostException e) {
				// should never happen
				sb.append("unknown");
			}
			
			sb.append("]");
			return sb.toString();
		}
	}
	
	/**
	 * Used by the configuration parser to report on errors.
	 * 
	 */
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

	/**
	 * Construct a new {@link InetFilter}.
	 * 
	 * @param config the configuration of the filter.
	 */
	public InetFilter(String config) {
		try {
			this.config = Arrays.asList(config.split(",")).stream()
				.map(String::trim) // ignore whitespace
				.filter(configElement -> !configElement.isEmpty()) // ignore empty elements
				.map(configElement -> { 
					// separate address and mask length
					final String[] elementSplit = configElement.split("/");
					if(elementSplit.length == 2) {
						return new ConfigElement(elementSplit[0], elementSplit[1]);
					} else {
						return new ConfigElement(configElement);
					}
				})
				.map(configElement -> {
					try {
						// parse address
						final byte[] address = InetAddress.getByName(configElement.getHost()).getAddress();
						final int addressLength = address.length * 8;
						
						final int maskLength = configElement.getMaskLength()
							.map(maskLengthString -> {
								// parse mask length
								try {
									int maskLengthInt = Integer.parseInt(maskLengthString);
									if(maskLengthInt > addressLength) {
										throw new ConfigException("mask length larger than address length: " + maskLengthInt);
									}
									
									return maskLengthInt;
								} catch(NumberFormatException e) {
									throw new ConfigException("invalid mask length", e);
								}
							})
							.orElse(addressLength); // default mask length (i.e. address is a host address)
						
						// generate mask based on configured mask length
						final byte[] mask = new byte[address.length];
						Arrays.fill(mask, (byte)0);
						
						int pos = 0, bitsNeeded = maskLength;
						while(bitsNeeded >= 8) {
							mask[pos++] = (byte)0xff;
							bitsNeeded -= 8;
						}
						
						if(bitsNeeded > 0) {
							mask[pos] = (byte)(0xff << (8 - bitsNeeded));
						}
						
						return new FilterElement(address, mask, maskLength);
					} catch(UnknownHostException e) {
						throw new ConfigException("invalid host or ip address", e);
					}
				})
				.collect(Collectors.toList());
		} catch(ConfigException e) {
			throw new IllegalArgumentException("Invalid filter configuration", e);
		}
	}
	
	public boolean isAllowed(InetAddress inetAddress) {
		byte[] address = inetAddress.getAddress();
		for(FilterElement element : config) {
			if(element.isAllowed(address)) {
				return true;
			}
		}
		
		return false;
	}
	
	public List<FilterElement> getFilterElements() {
		return Collections.unmodifiableList(config);
	}
}
