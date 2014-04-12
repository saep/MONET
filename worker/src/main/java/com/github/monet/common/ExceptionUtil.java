package com.github.monet.common;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {

	public static String stacktraceToString(Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		return sw.toString();
	}

}
