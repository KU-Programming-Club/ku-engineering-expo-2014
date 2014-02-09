package edu.kuacm.expo.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream which counts the total number of bytes read and notifies a listener.
 * 
 * @author Christophe Beyls
 * 
 */
public class ByteCountInputStream extends FilterInputStream {

	public interface ByteCountListener {
		void onNewCount(int byteCount);
	}

	private final ByteCountListener mListener;
	private final int mInterval;
	private int mCurrentBytes = 0;
	private int mNextStepBytes;

	public ByteCountInputStream(InputStream input, ByteCountListener listener, int interval) {
		super(input);
		if (input == null) {
			throw new IllegalArgumentException("input must not be null");
		}
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null");
		}
		if (interval <= 0) {
			throw new IllegalArgumentException("interval must be at least 1 byte");
		}
		this.mListener = listener;
		this.mInterval = interval;
		mNextStepBytes = interval;
		listener.onNewCount(0);
	}

	@Override
	public int read() throws IOException {
		int b = super.read();
		addBytes((b == -1) ? -1 : 1);
		return b;
	}

	@Override
	public int read(byte[] buffer, int offset, int max) throws IOException {
		int count = super.read(buffer, offset, max);
		addBytes(count);
		return count;
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public synchronized void mark(int readlimit) {
		throw new IllegalStateException();
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new IllegalStateException();
	}

	@Override
	public long skip(long byteCount) throws IOException {
		long count = super.skip(byteCount);
		addBytes((int) count);
		return count;
	}

	private void addBytes(int count) {
		if (count != -1) {
			mCurrentBytes += count;
			if (mCurrentBytes < mNextStepBytes) {
				return;
			}
			mNextStepBytes = mCurrentBytes + mInterval;
		}
		mListener.onNewCount(mCurrentBytes);
	}
}