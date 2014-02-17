package edu.kuacm.expo2014.parsers;

import java.io.InputStream;

public interface Parser<T> {
	T parse(InputStream is) throws Exception;
}