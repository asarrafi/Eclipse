/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.core;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.internal.ReverseComparator;

public class Util {

	private static final class ExtensionFileFilter implements FileFilter {
		private final String ext;

		private ExtensionFileFilter(String ext) {
			this.ext = ext;
		}

		public boolean accept(File pathname) {
			return ext.equals(Util.getExtension(pathname));
		}
	}

	public static final char[] BASE16_CHARS = "0123456789ABCDEF".toCharArray();

	private static final int _1KB = 1024;
	private static final int _1MB = 1024 * 1024;
	private static final int _5KB = 5 * 1024;
	private static final int _5MB = 5 * 1024 * 1024;
	private static final int _1GB = 1024 * 1024 * 1024;

	private static final MessageFormat DATASIZE_FORMAT = new MessageFormat(
			"{0,number,#.0} {1}");

	private static final int MAX_DEPTH = 8;

	public static String join(String[] s, String delim) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < s.length; i++) {
			if (i > 0) {
				result.append(delim);
			}
			result.append(s[i]);
		}

		return result.toString();
	}

	public static String join(Object[] o, String delim) {
		if (o == null) {
			return "";
		}

		String[] toString = new String[o.length];
		for (int i = 0; i < toString.length; i++) {
			toString[i] = "" + o[i];
		}

		return join(toString, delim);
	}

	public static String join(String[] components, String delim, int start,
			int end) {
		String[] subarray = new String[end - start + 1];
		System.arraycopy(components, start, subarray, 0, subarray.length);
		return join(subarray, delim);
	}

	public static String[] ensureQuoted(Object[] obj) {
		String[] result = new String[obj.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = ensureQuoted(obj[i]);
		}

		return result;
	}

	public static String ensureQuoted(Object obj) {
		String str = obj == null ? "" : obj.toString();
		if (str.indexOf(' ') != -1 || str.indexOf('\t') != -1) {
			return '\"' + str + '\"';
		}

		return str;
	}

	public static String trimQuotes(Object str) {
		return trim(str, '\"');
	}

	static String trim(Object str, char ch) {
		if (str == null) {
			return null;
		}
		
		String result = str.toString();
		if (result.length() > 0 && result.charAt(0) == ch) {
			result = result.substring(1);
		}
		if (result.length() > 0 && result.charAt(result.length() - 1) == ch) {
			result = result.substring(0, result.length() - 1);
		}
		
		return result;
	}
	
	public static String fill(char c, int length) {
		char[] result = new char[length];
		Arrays.fill(result, c);
		return new String(result);
	}

	public static void unzip(File zip, File targetDir) throws IOException {
		ZipInputStream input = new ZipInputStream(new FileInputStream(zip));
		OutputStream currentOutput = null;
		targetDir.mkdirs();
		try {
			for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input
					.getNextEntry()) {
				File currentFile = new File(targetDir, entry.getName());
				if (!entry.isDirectory()) {
					long size = entry.getSize();
					int readBytes = 0;
					byte[] buffer = new byte[512];
					currentFile.getParentFile().mkdirs();

					currentOutput = new FileOutputStream(currentFile);
					int maxRead = Math.min(buffer.length, (int) size
							- readBytes);
					for (int read = input.read(buffer, 0, maxRead); maxRead > 0
							&& read != -1; read = input
							.read(buffer, 0, maxRead)) {
						currentOutput.write(buffer, 0, read);
						readBytes += read;
						maxRead = Math.min(buffer.length, (int) size
								- readBytes);
					}

					currentOutput.close();
				} else {
					currentFile.mkdirs();
				}

				currentFile.setLastModified(entry.getTime());
			}
		} finally {
			if (input != null) {
				input.close();
			}
			if (currentOutput != null) {
				currentOutput.close();
			}
		}

	}

	public static String[] parseCommandLine(String command) {
		ArrayList<String> result = new ArrayList<String>();
		StringBuffer current = new StringBuffer();
		char[] chars = command.toCharArray();
		boolean inQuote = false;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '\"') {
				inQuote = !inQuote;
			} else if (chars[i] == ' ') {
				if (!inQuote) {
					addIfNotEmpty(result, current);
					current = new StringBuffer();
				} else {
					current.append(chars[i]);
				}
			} else {
				current.append(chars[i]);
			}
		}

		addIfNotEmpty(result, current);

		return result.toArray(new String[result.size()]);
	}

	private static void addIfNotEmpty(ArrayList<String> result,
			StringBuffer current) {
		if (current.toString().trim().length() > 0) {
			result.add(current.toString());
		}
	}

	public static void mergeFiles(IProgressMonitor monitor, File[] src,
			File dest) throws IOException {
		dest.getParentFile().mkdirs();
		FileOutputStream output = new FileOutputStream(dest);
		try {
			mergeFiles(monitor, src, output);
		} finally {
			output.close();
		}
	}

	private static void mergeFiles(IProgressMonitor monitor, File[] src,
			OutputStream output) throws IOException {
		for (int i = 0; i < src.length; i++) {
			if (!src[i].exists()) {
				throw new FileNotFoundException(src[i].getAbsolutePath());
			}

			if (src[i].isDirectory()) {
				throw new IOException(src[i]
						+ " is a directory, cannot be copied");
			}

		}

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		monitor.beginTask("Copying...", src.length);

		for (int i = 0; i < src.length; i++) {
			monitor.setTaskName(MessageFormat.format("Copying {0}", src[i]
					.getName()));
			FileInputStream input = new FileInputStream(src[i]);
			try {
				byte[] buffer = new byte[65536];
				for (int read = input.read(buffer); read != -1; read = input
						.read(buffer)) {
					output.write(buffer, 0, read);
				}
			} finally {
				input.close();
			}
			monitor.worked(1);
		}
	}
	
	public static void copy(IProgressMonitor monitor, File src, File dest, FileFilter filter) throws IOException {
		if (src.isDirectory()) {
			copyDir(monitor, src, dest, filter);
		} else {
			copyFile(monitor, src, dest);
		}
	}

	public static void copyFile(IProgressMonitor monitor, File src, File dest)
			throws IOException {
		mergeFiles(monitor, new File[] { src }, dest);
	}

	public static void copyDir(IProgressMonitor monitor, File srcDir,
			File destDir, FileFilter filter) throws IOException {
		copyDir(monitor, srcDir, destDir, filter, 0);
	}

	public static void copyDir(IProgressMonitor monitor, File srcDir,
			File destDir, FileFilter filter, int depth) throws IOException {
		if (depth > MAX_DEPTH) {
			return;
		}

		destDir.mkdirs();

		File[] files = srcDir.listFiles();
		monitor.beginTask("Recursive copy", IProgressMonitor.UNKNOWN);
		for (int i = 0; i < files.length; i++) {
			File src = new File(srcDir, files[i].getName());
			if (filter == null || filter.accept(src)) {
				File dest = new File(destDir, files[i].getName());
				if (files[i].isDirectory()) {
					copyDir(new NullProgressMonitor(), src, dest, filter,
							depth + 1);
				} else {
					copyFile(new NullProgressMonitor(), src, dest);
				}
			}
		}
	}
	
	public static byte[] fromBase16(String data) {
		boolean extraByte = data.length() % 2 == 1;
		byte[] result = new byte[data.length() / 2 + (extraByte ? 1 : 0)];

		for (int i = 0; i < result.length; i++) {
			int value = Integer.parseInt(data.substring(2 * i, Math.min(
					2 * i + 2, data.length())), 16);
			result[i] = (byte) (value & 0xff);
		}

		return result;
	}

	public static String toBase16(byte[] data) {
		return toBase16(data, 0, data.length);
	}

	public static String toBase16(byte[] data, int offset, int length) {
		char[] result = new char[length * 2];
		for (int i = 0; i < length; i++) {
			result[2 * i] = BASE16_CHARS[(data[offset + i] >> 4) & 0xf];
			result[2 * i + 1] = BASE16_CHARS[data[offset + i] & 0xf];
		}

		return new String(result);
	}

	/**
	 * Returns a simple string representation of data size, eg "23 bytes",
	 * "2 KB", "11 MB", etc
	 * 
	 * @param size
	 * @return
	 */
	public static String dataSize(long size) {
		if (size < _5KB) {
			return size + " bytes";
		}

		String unit = "";
		float value = (float) size;

		if (size > _1GB) {
			unit = "GB";
			value = value / _1GB;
		} else if (size > _5MB) {
			unit = "MB";
			value = value / _1MB;
		} else {
			unit = "KB";
			value = value / _1KB;
		}

		return DATASIZE_FORMAT.format(new Object[] { value, unit },
				new StringBuffer(30), new FieldPosition(0)).toString();
	}

	public static String getExtension(File file) {
		int index = file.getName().lastIndexOf('.');
		if (index != -1) {
			return file.getName().substring(index + 1);
		} else {
			return "";
		}
	}

	public static String getNameWithoutExtension(File file) {
		String name = file.getName();
		int index = name.lastIndexOf('.');
		if (index == -1) {
			return name;
		} else {
			return name.substring(0, index);
		}
	}

	public static void main(String[] args) {
		System.err.println(Util.getExtension(new File(".a")));
		System.err.println(Util.getNameWithoutExtension(new File("a")));

		System.err
				.println(toBase16(fromBase16("0123456789abcdefedcba9876543210")));

	}

	public static void writeToFile(File file, String text) throws IOException {
		FileWriter output = new FileWriter(file);
		try {
			output.write(text);
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}

	public static boolean deleteFiles(File file, FileFilter filter,
			int maxDepth, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return false;
		}

		if (maxDepth < 0) {
			return true;
		}

		monitor.setTaskName(MessageFormat.format("Deleting {0}", file));

		if (file.isDirectory()) {
			boolean result = true;
			File[] filesToDelete = file.listFiles();
			for (int i = 0; i < filesToDelete.length; i++) {
				result &= deleteFiles(filesToDelete[i], filter, maxDepth - 1,
						monitor);
			}

			result &= file.delete();
			return result;
		} else {
			if (filter == null || filter.accept(file)) {
				return file.delete();
			} else {
				return true;
			}
		}
	}

	public static FileFilter getExtensionFilter(final String ext) {
		FileFilter filter = new ExtensionFileFilter(ext);
		return filter;
	}

	public static String replaceExtension(String filename, String newExtension) {
		int where = filename.lastIndexOf('.');
		if (where != -1) {
			filename = filename.substring(0, where);
		}

		return filename + (isEmpty(newExtension) ? "" : ".") + newExtension;
	}

	public static void safeClose(InputStream input) {
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				// Ignore.
			}
		}
	}

	public static void safeClose(OutputStream output) {
		if (output != null) {
			try {
				output.close();
			} catch (IOException e) {
				// Ignore.
			}
		}
	}

	public static void safeClose(Writer output) {
		if (output != null) {
			try {
				output.close();
			} catch (IOException e) {
				// Ignore.
			}
		}
	}
	
	public static void safeClose(Reader input) {
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				// Ignore.
			}
		}
	}

	public static String readFile(String filename) throws IOException {
		return readFile(filename, null);
	}

	public static String readFile(String filename, String enc)
			throws IOException {
		if (filename == null) {
			return null;
		}

		File file = new File(filename);
		if (!file.exists()) {
			throw new IOException(MessageFormat.format(
					"File ''{0}'' does not exist", filename));
		}

		ByteArrayOutputStream result = new ByteArrayOutputStream();
		mergeFiles(new NullProgressMonitor(), new File[] { file }, result);
		return enc == null ? new String(result.toByteArray()) : new String(
				result.toByteArray(), enc);
	}

	public static int readInt(InputStream input) throws IOException {
		// LE or BE?
		byte[] intBuf = new byte[4];

		int totalRead = 0;
		for (int read = input.read(intBuf, totalRead, 4 - totalRead); read != 4
				&& totalRead < 4; read = input.read(intBuf, totalRead,
				4 - totalRead)) {
			if (read < 1) {
				throw new EOFException();
			}
			totalRead += read;
		}

		int result = (intBuf[3] & 0xff) << 24;
		result |= (intBuf[2] & 0xff) << 16;
		result |= (intBuf[1] & 0xff) << 8;
		result |= (intBuf[0] & 0xff);

		return result;
	}

	public static short readShort(InputStream input) throws IOException {
		byte[] shortBuf = new byte[2];

		int totalRead = 0;
		for (int read = input.read(shortBuf, totalRead, 2 - totalRead); read != 2
				&& totalRead < 2; read = input.read(shortBuf, totalRead,
				2 - totalRead)) {
			if (read < 1) {
				throw new EOFException();
			}
			totalRead += read;
		}

		short result = (short) ((shortBuf[1] & 0xff) << 8);
		result |= (shortBuf[0] & 0xff);

		return result;
	}

	public static File relativeTo(File peer, String filename) {
		File filenameFile = new File(filename);
		if (filenameFile.isAbsolute()) {
			return filenameFile;
		}

		return new File(peer.getParent(), filename);
	}

	public static boolean isEmpty(String text) {
		return text == null || text.isEmpty();
	}

	public static boolean isEmptyDirectory(File file) {
		return !file.isDirectory() || file.list().length == 0;
	}

	/**
	 * A utility method for handling <code>equals</code> of
	 * <code>null</code> objects. 
	 * @param o1
	 * @param o2
	 */
	public static boolean equals(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		
		return o1.equals(o2);
	}
	
	/**
	 * Replaces parameters tagged with <code>%</code>s and returns
	 * the resolved string.
	 * @param input
	 * @param map
	 * @return
	 */
    public static String replace(String input, Map<String, String> map) {
        try {
			return innerReplace(input, new DefaultParameterResolver(map), 0).toString();
		} catch (ParameterResolverException e) {
			// The default should never throw this exception
			throw new RuntimeException(e);
		}
    }
    
	/**
	 * Replaces parameters tagged with <code>%</code>s and returns
	 * the resolved string.
	 * @param input
	 * @param map
	 * @return
	 * @throws ParameterResolverException 
	 */
    public static String replace(String input, ParameterResolver resolver) throws ParameterResolverException {
        return innerReplace(input, resolver, 0).toString();
    }
    
    private static StringBuffer innerReplace(String input, ParameterResolver map, int depth) throws ParameterResolverException {
        if (depth > 12) {
            throw new IllegalArgumentException("Cyclic parameters"); //$NON-NLS-1$
        }
        
        StringBuffer result = new StringBuffer();
        char[] chars = input.toCharArray();
        boolean inParam = false;
        int paramStart = 0;
        
        for (int i = 0; i < chars.length; i++) {
            if ('%' == chars[i]) {
                if (!inParam) {
                    paramStart = i;
                } else {
                    String paramName = input.substring(paramStart + 1, i);
                    if (paramName.length() > 0 && paramName.charAt(0) == '#') {
                        // TODO.
                    } else {
                        if (paramName.length() == 0) { // Escape pattern %% => %
                            result.append("%"); //$NON-NLS-1$
                        } else {
                            String paramValue = map.get(paramName);
                            if (paramValue != null) {
                                result.append(innerReplace(paramValue, map, depth + 1));
                            } else {
                                // Just leave as-is
                                result.append('%');
                                result.append(paramName);
                                result.append('%');
                            }
                        }
                    }
                }
                
                inParam = !inParam;
            } else if (!inParam) {
                result.append(chars[i]);
            }
        }
        
        return result;
    }

    /**
     * Creates a GET URL, given a baseURL and a map of parameters to send in the GET
     * @param service
     * @param params
     * @return A URL of the format <code>baseURL?param1=value1&param2=value2...</code>
     */
    public static String toGetUrl(String baseURL, Map<String, String> params) {
        StringBuffer paramsStr = new StringBuffer();
        if (params != null && !params.isEmpty()) {
            paramsStr.append("?"); //$NON-NLS-1$
            int paramCnt = 0;
            for (Map.Entry<String, String> param : params.entrySet()) {
                paramCnt++;
                if (paramCnt > 1) {
                    paramsStr.append("&"); //$NON-NLS-1$
                }

                paramsStr.append(URLEncoder.encode(param.getKey()) + "=" + URLEncoder.encode(param.getValue())); //$NON-NLS-1$
            }
        }

        return baseURL + paramsStr;
    }

    public static <T> Comparator<T> reverseComparator(Comparator<T> original) {
        return new ReverseComparator<T>(original);
    }

    /**
     * Returns a 'parent' key of a key using path separators. So, if the input key is A/B/C, this 
     * method returns A/B
     * @param key
     * @return <code>null</code> if <code>key</code> has no path separator
     */
    public static String getParentKey(String key) {
        Path permissionPath = new Path(key);
        return permissionPath.segmentCount() > 1 ? permissionPath.removeLastSegments(1).toPortableString() : null;
    }
	
}
