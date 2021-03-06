// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.ini;

import static org.apache.juneau.ini.ConfigFileFormat.*;
import static org.apache.juneau.ini.ConfigUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

import org.apache.juneau.annotation.*;

/**
 * Defines a section in a config file.
 */
public final class Section implements Map<String,String> {

	private ConfigFileImpl configFile;
	String name;   // The config section name, or "default" if the default section.  Never null.

	// The data structures that make up this object.
	// These must be kept synchronized.
	private LinkedList<String> lines = new LinkedList<>();
	private List<String> headerComments = new LinkedList<>();
	Map<String,String> entries;

	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private boolean readOnly;

	/**
	 * Constructor.
	 */
	public Section() {
		this.entries = new LinkedHashMap<>();
	}

	/**
	 * Constructor with predefined contents.
	 *
	 * @param contents Predefined contents to copy into this section.
	 */
	public Section(Map<String,String> contents) {
		this.entries = new LinkedHashMap<>(contents);
	}

	Section setReadOnly() {
		// This method is only called once from ConfigFileImpl constructor.
		this.readOnly = true;
		this.entries = Collections.unmodifiableMap(entries);
		return this;
	}

	/**
	 * Sets the config file that this section belongs to.
	 *
	 * @param configFile The config file that this section belongs to.
	 * @return This object (for method chaining).
	 */
	@ParentProperty
	public Section setParent(ConfigFileImpl configFile) {
		this.configFile = configFile;
		return this;
	}

	/**
	 * Sets the section name
	 *
	 * @param name The section name.
	 * @return This object (for method chaining).
	 */
	@NameProperty
	public Section setName(String name) {
		this.name = name;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Map methods
	//--------------------------------------------------------------------------------

	@Override /* Map */
	public void clear() {
		Set<String> changes = createChanges();
		writeLock();
		try {
			if (changes != null)
				for (String k : keySet())
					changes.add(getFullKey(name, k));
			entries.clear();
			lines.clear();
			headerComments.clear();
		} finally {
			writeUnlock();
		}
		signalChanges(changes);
	}

	@Override /* Map */
	public boolean containsKey(Object key) {
		return entries.containsKey(key);
	}

	@Override /* Map */
	public boolean containsValue(Object value) {
		return entries.containsValue(value);
	}

	@Override /* Map */
	public Set<Map.Entry<String,String>> entrySet() {

		// We need to create our own set so that entries are removed correctly.
		return new AbstractSet<Map.Entry<String,String>>() {
			@Override /* Set */
			public Iterator<Map.Entry<String,String>> iterator() {
				return new Iterator<Map.Entry<String,String>>() {
					Iterator<Map.Entry<String,String>> i = entries.entrySet().iterator();
					Map.Entry<String,String> i2;

					@Override /* Iterator */
					public boolean hasNext() {
						return i.hasNext();
					}

					@Override /* Iterator */
					public Map.Entry<String,String> next() {
						i2 = i.next();
						return i2;
					}

					@Override /* Iterator */
					public void remove() {
						Set<String> changes = createChanges();
						String key = i2.getKey(), val = i2.getValue();
						addChange(changes, key, val, null);
						writeLock();
						try {
							i.remove();
							removeLine(key);
						} finally {
							writeUnlock();
						}
						signalChanges(changes);
					}
				};
			}

			@Override /* Set */
			public int size() {
				return entries.size();
			}
		};
	}

	@Override /* Map */
	public String get(Object key) {
		String s = entries.get(key);
		return s;
	}

	@Override /* Map */
	public boolean isEmpty() {
		return entries.isEmpty();
	}

	@Override /* Map */
	public Set<String> keySet() {

		// We need to create our own set so that sections are removed correctly.
		return new AbstractSet<String>() {
			@Override /* Set */
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					Iterator<String> i = entries.keySet().iterator();
					String i2;

					@Override /* Iterator */
					public boolean hasNext() {
						return i.hasNext();
					}

					@Override /* Iterator */
					public String next() {
						i2 = i.next();
						return i2;
					}

					@Override /* Iterator */
					public void remove() {
						Set<String> changes = createChanges();
						String key = i2;
						String val = entries.get(key);
						addChange(changes, key, val, null);
						writeLock();
						try {
							i.remove();
							removeLine(key);
						} finally {
							writeUnlock();
						}
						signalChanges(changes);
					}
				};
			}

			@Override /* Set */
			public int size() {
				return entries.size();
			}
		};
	}

	@Override /* Map */
	public String put(String key, String value) {
		return put(key, value, false);
	}

	/**
	 * Sets the specified value in this section.
	 *
	 * @param key The section key.
	 * @param value The new value.
	 * @param encoded Whether this value should be encoded during save.
	 * @return The previous value.
	 */
	public String put(String key, String value, boolean encoded) {
		Set<String> changes = createChanges();
		String s = put(key, value, encoded, changes);
		signalChanges(changes);
		return s;
	}

	String put(String key, String value, boolean encoded, Set<String> changes) {
		writeLock();
		try {
			addLine(key, encoded);
			String prev = entries.put(key, value);
			addChange(changes, key, prev, value);
			return prev;
		} finally {
			writeUnlock();
		}
	}

	@Override /* Map */
	public void putAll(Map<? extends String,? extends String> map) {
		Set<String> changes = createChanges();
		for (Map.Entry<? extends String,? extends String> e : map.entrySet())
			put(e.getKey(), e.getValue(), false, changes);
		signalChanges(changes);
	}

	@Override /* Map */
	public String remove(Object key) {
		Set<String> changes = createChanges();
		String old = remove(key, changes);
		signalChanges(changes);
		return old;
	}

	String remove(Object key, Set<String> changes) {
		writeLock();
		try {
			String prev = entries.remove(key);
			addChange(changes, key.toString(), prev, null);
			removeLine(key.toString());
			return prev;
		} finally {
			writeUnlock();
		}
	}

	void removeLine(String key) {
		for (Iterator<String> i = lines.iterator(); i.hasNext();) {
			String k = i.next();
			if (k.startsWith("*") || k.startsWith(">")) {
				if (k.substring(1).equals(key)) {
					i.remove();
					break;
				}
			}
		}
	}

	@Override /* Map */
	public int size() {
		return entries.size();
	}

	@Override /* Map */
	public Collection<String> values() {
		return Collections.unmodifiableCollection(entries.values());
	}


	//--------------------------------------------------------------------------------
	// API methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if the specified entry is encoded.
	 *
	 * @param key The key.
	 * @return <jk>true</jk> if the specified entry is encoded.
	 */
	public boolean isEncoded(String key) {
		readLock();
		try {
			for (String s : lines)
				if (s.length() > 1)
					if (s.substring(1).equals(key))
						return s.charAt(0) == '*';
			return false;
		} finally {
			readUnlock();
		}
	}

	/**
	 * Adds header comments to this section.
	 *
	 * @see ConfigFile#addHeaderComments(String, String...) for a description.
	 * @param comments The comment lines to add to this section.
	 * @return This object (for method chaining).
	 */
	public Section addHeaderComments(List<String> comments) {
		writeLock();
		try {
			for (String c : comments) {
				if (c == null)
					c = "";
				if (! c.startsWith("#"))
					c = "#" + c;
				this.headerComments.add(c);
			}
			return this;
		} finally {
			writeUnlock();
		}
	}

	/**
	 * Removes all header comments from this section.
	 */
	public void clearHeaderComments() {
		writeLock();
		try {
			this.headerComments.clear();
		} finally {
			writeUnlock();
		}
	}

	/**
	 * Serialize this section.
	 *
	 * @param out What to serialize to.
	 * @param format The format (e.g. INI, BATCH, SHELL).
	 */
	public void writeTo(PrintWriter out, ConfigFileFormat format) {
		readLock();
		try {
			if (format == INI) {
				for (String s : headerComments)
					out.append(s).println();
				if (! name.equals("default"))
					out.append('[').append(name).append(']').println();
				for (String l : lines) {
					char c = (l.length() > 0 ? l.charAt(0) : 0);
					if (c == '>' || c == '*'){
						boolean encode = c == '*';
						String key = l.substring(1);
						String val = entries.get(key);
						if (val.indexOf('\n') != -1)
							val = val.replaceAll("(\\r?\\n)", "$1\t");
						if (val.indexOf('=') != -1)
							val = val.replace("=", "\\u003D");
						if (val.indexOf('#') != -1)
							val = val.replace("#", "\\u0023");
						out.append(key);
						if (encode)
							out.append('*');
						out.append(" = ");
						if (encode)
							out.append('{').append(configFile.getEncoder().encode(key, val)).append('}');
						else
							out.append(val);
						out.println();
					} else {
						out.append(l).println();
					}
				}

			} else if (format == BATCH) {
				String section = name.replaceAll("\\.\\/", "_");
				for (String l : headerComments) {
					l = trimComment(l);
					if (! l.isEmpty())
						out.append("rem ").append(l);
					out.println();
				}
				for (String l : lines) {
					char c = (l.length() > 0 ? l.charAt(0) : 0);
					if (c == '>' || c == '*') {
						String key = l.substring(1);
						String val = entries.get(key);
						out.append("set ");
						if (! name.equals("default"))
							out.append(section).append('_');
						out.append(key.replaceAll("\\.\\/", "_")).append(" = ").append(val).println();
					} else {
						l = trimComment(l);
						if (! l.isEmpty())
							out.append("rem ").append(l);
						out.println();
					}
				}

			} else if (format == SHELL) {
				String section = name.replaceAll("\\.\\/", "_");
				for (String l : headerComments) {
					l = trimComment(l);
					if (! l.isEmpty())
						out.append("# ").append(l);
					out.println();
				}
				for (String l : lines) {
					char c = (l.length() > 0 ? l.charAt(0) : 0);
					if (c == '>' || c == '*'){
						String key = l.substring(1);
						String val = entries.get(key).replaceAll("\\\\", "\\\\\\\\");
						out.append("export ");
						if (! name.equals("default"))
							out.append(section).append('_');
						out.append(key.replaceAll("\\.\\/", "_")).append('=').append('"').append(val).append('"').println();
					} else {
						l = trimComment(l);
						if (! l.isEmpty())
							out.append("# ").append(l);
						out.println();
					}
				}
			}
		} finally {
			readUnlock();
		}
	}


	//--------------------------------------------------------------------------------
	// Protected methods used by ConfigFile
	//--------------------------------------------------------------------------------

	/*
	 * Add lines to this section.
	 */
	Section addLines(Set<String> changes, String...l) {
		writeLock();
		try {
			if (l == null)
				l = new String[0];
			for (int i = 0; i < l.length; i++) {
				String line = l[i];
				if (line == null)
					line = "";
				if (isComment(line))
					this.lines.add(line);
				else if (isAssignment(line)) {
					// Key/value pairs are stored as either ">key" or "*key";
					String key = replaceUnicodeSequences(line.substring(0, line.indexOf('=')).trim());
					String val = replaceUnicodeSequences(line.substring(line.indexOf('=')+1).trim());
					boolean encoded = key.length() > 1 && key.endsWith("*");
					if (encoded) {
						key = key.substring(0, key.lastIndexOf('*'));
						String v = val.toString().trim();
						if (v.startsWith("{") && v.endsWith("}"))
							val = configFile.getEncoder().decode(key, v.substring(1, v.length()-1));
						else
							configFile.setHasBeenModified();
					}
					if (containsKey(key)) {
						entries.remove(key);
						lines.remove('*' + key);
						lines.remove('>' + key);
					}
					lines.add((encoded ? '*' : '>') + key);
					addChange(changes, key, entries.put(key, val), val);
				} else {
					this.lines.add(line);
				}
			}
			return this;
		} finally {
			writeUnlock();
		}
	}

	/*
	 * Remove all "#*" lines at the end of this section so they can
	 * be associated with the next section.
	 */
	List<String> removeTrailingComments() {
		LinkedList<String> l = new LinkedList<>();
		while ((! lines.isEmpty()) && lines.getLast().startsWith("#"))
			l.addFirst(lines.removeLast());
		return l;
	}


	//--------------------------------------------------------------------------------
	// Private methods
	//--------------------------------------------------------------------------------

	private void addLine(String key, boolean encoded) {
		for (Iterator<String> i = lines.iterator(); i.hasNext();) {
			String k = i.next();
			if ((k.startsWith("*") || k.startsWith(">")) && k.substring(1).equals(key)) {
				if (k.startsWith("*") && encoded || k.startsWith(">") && ! encoded)
					return;
				i.remove();
			}
		}
		lines.add((encoded ? "*" : ">") + key);
	}

	void readLock() {
		lock.readLock().lock();
	}

	void readUnlock() {
		lock.readLock().unlock();
	}

	void writeLock() {
		if (readOnly)
			throw new UnsupportedOperationException("Cannot modify read-only ConfigFile.");
		lock.writeLock().lock();
	}

	void writeUnlock() {
		lock.writeLock().unlock();
	}

	private static String trimComment(String s) {
		return s.replaceAll("^\\s*\\#\\s*", "").trim();
	}

	Set<String> createChanges() {
		return (configFile != null && configFile.getListeners().size() > 0 ? new LinkedHashSet<String>() : null);
	}

	void signalChanges(Set<String> changes) {
		if (changes != null && ! changes.isEmpty())
			for (ConfigFileListener l : configFile.getListeners())
				l.onChange(configFile, changes);
	}

	void addChange(Set<String> changes, String key, String oldVal, String newVal) {
		if (changes != null)
			if (! isEquals(oldVal, newVal))
				changes.add(getFullKey(name, key));
	}
}