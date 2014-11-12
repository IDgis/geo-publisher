package nl.idgis.publisher.utils;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

public class EnumUtils {

	public static <T extends Collection<? extends Enum<?>>> Collection<String> enumsToStrings(final T enums) {
		return new AbstractCollection<String>() {

			@Override
			public Iterator<String> iterator() {
				final Iterator<? extends Enum<?>> enumIterator = enums.iterator();
				
				return new Iterator<String>() {

					@Override
					public boolean hasNext() {
						return enumIterator.hasNext();
					}

					@Override
					public String next() {
						return enumIterator.next().name();
					}

					@Override
					public void remove() {
						enumIterator.remove();
					}					
				};	
			}

			@Override
			public int size() {				
				return enums.size();
			}
		};
	}
}
