package net.fishbulb.jcod.util.extra;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Replacement utility class for apache commons
 */
public final class ExtraUtils {

	private ExtraUtils() {
	}

	// Nullity tests
	
	public static boolean isEmpty(final String string) {
		return string == null || string.length() == 0;
	}

	public static boolean isEmpty(final Collection<?> collection) {
		return collection == null || collection.size() == 0;
	}

	// Iterators

	/**
	 * Cyclic iterator. Doesn't support remove() operation.
	 * @param iterable
	 * @return
	 */
	public static <T> Iterator<T> cycle(final Iterable<T> iterable) {
		return new Iterator<T>() {
			Iterator<T> iterator = new Iterator<T>() {
				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public T next() {
					return null;
				}
			};

			@Override
			public boolean hasNext() {
				if (!iterator.hasNext()) {
					iterator = iterable.iterator();
				}
				return iterator.hasNext();
			}

			@Override
			public T next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				return iterator.next();
			}

			@Override
			public void remove() {
				throw new RuntimeException("not implemented");
			}
		};
	}

	/**
	 * Cyclic iterator. Doesn't support remove() operation.
	 * @param elements
	 * @return
	 */
	public static <T> Iterator<T> cycle(T... elements) {
		return cycle(Arrays.asList(elements));
	}
}
