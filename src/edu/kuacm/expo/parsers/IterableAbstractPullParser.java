package edu.kuacm.expo.parsers;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.xmlpull.v1.XmlPullParser;

/**
 * An abstract class for easy implementation of an iterable pull parser.
 * 
 * @author Christophe Beyls
 */
public abstract class IterableAbstractPullParser<T> extends AbstractPullParser<Iterable<T>> {

	private class ParserIterator implements Iterator<T> {

		private XmlPullParser mParser;
		private T mNext = null;

		public ParserIterator(XmlPullParser parser) {
			mParser = parser;
			try {
				if (parseHeader(parser)) {
					mNext = parseNext(parser);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean hasNext() {
			return mNext != null;
		}

		@Override
		public T next() {
			if (mNext == null) {
				throw new NoSuchElementException();
			}
			T current = mNext;
			try {
				mNext = parseNext(mParser);
				if (mNext == null) {
					parseFooter(mParser);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return current;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	@Override
	protected Iterable<T> parse(final XmlPullParser parser) throws Exception {
		return new Iterable<T>() {

			@Override
			public Iterator<T> iterator() {
				return new ParserIterator(parser);
			}

		};
	}

	/**
	 * @return true if the header was parsed successfully and the main items list has been reached.
	 * @throws Exception
	 */
	protected abstract boolean parseHeader(XmlPullParser parser) throws Exception;

	/**
	 * @return the next item, or null if no more items are found.
	 * @throws Exception
	 */
	protected abstract T parseNext(XmlPullParser parser) throws Exception;

	protected void parseFooter(XmlPullParser parser) throws Exception {
		while (!isEndDocument()) {
			parser.next();
		}
	}
}