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
package org.apache.juneau;

import static org.apache.juneau.ClassMeta.ClassCategory.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ReflectionUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.URI;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.remoteable.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * A wrapper class around the {@link Class} object that provides cached information about that class.
 *
 * <p>
 * Instances of this class can be created through the {@link BeanContext#getClassMeta(Class)} method.
 *
 * <p>
 * The {@link BeanContext} class will cache and reuse instances of this class except for the following class types:
 * <ul>
 * 	<li>Arrays
 * 	<li>Maps with non-Object key/values.
 * 	<li>Collections with non-Object key/values.
 * </ul>
 *
 * <p>
 * This class is tied to the {@link BeanContext} class because it's that class that makes the determination of what is
 * a bean.
 *
 * @param <T> The class type of the wrapped class.
 */
@Bean(properties="innerClass,classCategory,elementType,keyType,valueType,notABeanReason,initException,beanMeta")
public final class ClassMeta<T> implements Type {

	/** Class categories. */
	enum ClassCategory {
		MAP, COLLECTION, CLASS, METHOD, NUMBER, DECIMAL, BOOLEAN, CHAR, DATE, ARRAY, ENUM, OTHER, CHARSEQ, STR, OBJ, URI, BEANMAP, READER, INPUTSTREAM, VOID, ARGS
	}

	final Class<T> innerClass;                              // The class being wrapped.

	private final Class<? extends T> implClass;             // The implementation class to use if this is an interface.
	private final ClassCategory cc;                         // The class category.
	private final Method fromStringMethod;                  // The static valueOf(String) or fromString(String) or forString(String) method (if it has one).
	private final Constructor<? extends T>
		noArgConstructor;                                    // The no-arg constructor for this class (if it has one).
	private final Constructor<T>
		stringConstructor,                                   // The X(String) constructor (if it has one).
		numberConstructor,                                   // The X(Number) constructor (if it has one).
		swapConstructor;                                     // The X(Swappable) constructor (if it has one).
	private final Class<?>
		swapMethodType,                                      // The class type of the object in the number constructor.
		numberConstructorType;
	private final Method
		swapMethod,                                          // The swap() method (if it has one).
		unswapMethod;                                        // The unswap() method (if it has one).
	private final Setter
		namePropertyMethod,                                  // The method to set the name on an object (if it has one).
		parentPropertyMethod;                                // The method to set the parent on an object (if it has one).
	private final boolean
		isDelegate,                                          // True if this class extends Delegate.
		isAbstract,                                          // True if this class is abstract.
		isMemberClass;                                       // True if this is a non-static member class.
	private final Object primitiveDefault;                  // Default value for primitive type classes.
	private final Map<String,Method>
		remoteableMethods,                                   // Methods annotated with @RemoteMethod.
		publicMethods;                                       // All public methods, including static methods.
	private final PojoSwap<?,?>[] childPojoSwaps;           // Any PojoSwaps where the normal type is a subclass of this class.
	private final ConcurrentHashMap<Class<?>,PojoSwap<?,?>>
		childSwapMap,                                        // Maps normal subclasses to PojoSwaps.
		childUnswapMap;                                      // Maps swap subclasses to PojoSwaps.
	private final PojoSwap<T,?>[] pojoSwaps;                // The object POJO swaps associated with this bean (if it has any).
	private final BeanFilter beanFilter;                    // The bean filter associated with this bean (if it has one).
	private final MetadataMap extMeta;                      // Extended metadata
	private final BeanContext beanContext;                  // The bean context that created this object.
	private final ClassMeta<?>
		elementType,                                         // If ARRAY or COLLECTION, the element class type.
		keyType,                                             // If MAP, the key class type.
		valueType;                                           // If MAP, the value class type.
	private final BeanMeta<T> beanMeta;                     // The bean meta for this bean class (if it's a bean).
	private final String
		typePropertyName,                                    // The property name of the _type property for this class and subclasses.
		notABeanReason,                                      // If this isn't a bean, the reason why.
		dictionaryName;                                      // The dictionary name of this class if it has one.
	private final Throwable initException;                  // Any exceptions thrown in the init() method.
	private final InvocationHandler invocationHandler;      // The invocation handler for this class (if it has one).
	private final BeanRegistry beanRegistry;                // The bean registry of this class meta (if it has one).
	private final ClassMeta<?>[] args;                      // Arg types if this is an array of args.

	private ReadWriteLock lock = new ReentrantReadWriteLock(false);
	private Lock rLock = lock.readLock(), wLock = lock.writeLock();

	/**
	 * Construct a new {@code ClassMeta} based on the specified {@link Class}.
	 *
	 * @param innerClass The class being wrapped.
	 * @param beanContext The bean context that created this object.
	 * @param implClass
	 * 	For interfaces and abstract classes, this represents the "real" class to instantiate.
	 * 	Can be <jk>null</jk>.
	 * @param beanFilter
	 * 	The {@link BeanFilter} programmatically associated with this class.
	 * 	Can be <jk>null</jk>.
	 * @param pojoSwap
	 * 	The {@link PojoSwap} programmatically associated with this class.
	 * 	Can be <jk>null</jk>.
	 * @param childPojoSwap
	 * 	The child {@link PojoSwap PojoSwaps} programmatically associated with this class.
	 * 	These are the <code>PojoSwaps</code> that have normal classes that are subclasses of this class.
	 * 	Can be <jk>null</jk>.
	 * @param delayedInit
	 * 	Don't call init() in constructor.
	 * 	Used for delayed initialization when the possibility of class reference loops exist.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	ClassMeta(Class<T> innerClass, BeanContext beanContext, Class<? extends T> implClass, BeanFilter beanFilter, PojoSwap<T,?>[] pojoSwaps, PojoSwap<?,?>[] childPojoSwaps) {
		this.innerClass = innerClass;
		this.beanContext = beanContext;

		wLock.lock();
		try {
			// We always immediately add this class meta to the bean context cache so that we can resolve recursive references.
			if (beanContext != null && beanContext.cmCache != null)
				beanContext.cmCache.put(innerClass, this);

			ClassMetaBuilder<T> builder = new ClassMetaBuilder(innerClass, beanContext, implClass, beanFilter, pojoSwaps, childPojoSwaps);

			this.cc = builder.cc;
			this.isDelegate = builder.isDelegate;
			this.fromStringMethod = builder.fromStringMethod;
			this.swapMethod = builder.swapMethod;
			this.unswapMethod = builder.unswapMethod;
			this.swapMethodType = builder.swapMethodType;
			this.parentPropertyMethod = builder.parentPropertyMethod;
			this.namePropertyMethod = builder.namePropertyMethod;
			this.noArgConstructor = builder.noArgConstructor;
			this.stringConstructor = builder.stringConstructor;
			this.swapConstructor = builder.swapConstructor;
			this.numberConstructor = builder.numberConstructor;
			this.numberConstructorType = builder.numberConstructorType;
			this.primitiveDefault = builder.primitiveDefault;
			this.publicMethods = builder.publicMethods;
			this.remoteableMethods = builder.remoteableMethods;
			this.beanFilter = beanFilter;
			this.pojoSwaps = builder.pojoSwaps.isEmpty() ? null : builder.pojoSwaps.toArray(new PojoSwap[builder.pojoSwaps.size()]);
			this.extMeta = new MetadataMap();
			this.keyType = builder.keyType;
			this.valueType = builder.valueType;
			this.elementType = builder.elementType;
			this.notABeanReason = builder.notABeanReason;
			this.beanMeta = builder.beanMeta;
			this.initException = builder.initException;
			this.typePropertyName = builder.typePropertyName;
			this.dictionaryName = builder.dictionaryName;
			this.invocationHandler = builder.invocationHandler;
			this.beanRegistry = builder.beanRegistry;
			this.isMemberClass = builder.isMemberClass;
			this.isAbstract = builder.isAbstract;
			this.implClass = builder.implClass;
			this.childUnswapMap = builder.childUnswapMap;
			this.childSwapMap = builder.childSwapMap;
			this.childPojoSwaps = builder.childPojoSwaps;
			this.args = null;
		} finally {
			wLock.unlock();
		}
	}

	/**
	 * Causes thread to wait until constructor has exited.
	 */
	final void waitForInit() {
		rLock.lock();
		rLock.unlock();
	}

	/**
	 * Copy constructor.
	 *
	 * <p>
	 * Used for creating Map and Collection class metas that shouldn't be cached.
	 */
	ClassMeta(ClassMeta<T> mainType, ClassMeta<?> keyType, ClassMeta<?> valueType, ClassMeta<?> elementType) {
		this.innerClass = mainType.innerClass;
		this.implClass = mainType.implClass;
		this.childPojoSwaps = mainType.childPojoSwaps;
		this.childSwapMap = mainType.childSwapMap;
		this.childUnswapMap = mainType.childUnswapMap;
		this.cc = mainType.cc;
		this.fromStringMethod = mainType.fromStringMethod;
		this.noArgConstructor = mainType.noArgConstructor;
		this.stringConstructor = mainType.stringConstructor;
		this.numberConstructor = mainType.numberConstructor;
		this.swapConstructor = mainType.swapConstructor;
		this.swapMethodType = mainType.swapMethodType;
		this.numberConstructorType = mainType.numberConstructorType;
		this.swapMethod = mainType.swapMethod;
		this.unswapMethod = mainType.unswapMethod;
		this.namePropertyMethod = mainType.namePropertyMethod;
		this.parentPropertyMethod = mainType.parentPropertyMethod;
		this.isDelegate = mainType.isDelegate;
		this.isAbstract = mainType.isAbstract;
		this.isMemberClass = mainType.isMemberClass;
		this.primitiveDefault = mainType.primitiveDefault;
		this.remoteableMethods = mainType.remoteableMethods;
		this.publicMethods = mainType.publicMethods;
		this.beanContext = mainType.beanContext;
		this.elementType = elementType;
		this.keyType = keyType;
		this.valueType = valueType;
		this.invocationHandler = mainType.invocationHandler;
		this.beanMeta = mainType.beanMeta;
		this.typePropertyName = mainType.typePropertyName;
		this.dictionaryName = mainType.dictionaryName;
		this.notABeanReason = mainType.notABeanReason;
		this.pojoSwaps = mainType.pojoSwaps;
		this.beanFilter = mainType.beanFilter;
		this.extMeta = mainType.extMeta;
		this.initException = mainType.initException;
		this.beanRegistry = mainType.beanRegistry;
		this.args = null;
	}

	/**
	 * Constructor for args-arrays.
	 */
	@SuppressWarnings("unchecked")
	ClassMeta(ClassMeta<?>[] args) {
		this.innerClass = (Class<T>) Object[].class;
		this.args = args;
		this.implClass = null;
		this.childPojoSwaps = null;
		this.childSwapMap = null;
		this.childUnswapMap = null;
		this.cc = ARGS;
		this.fromStringMethod = null;
		this.noArgConstructor = null;
		this.stringConstructor = null;
		this.numberConstructor = null;
		this.swapConstructor = null;
		this.swapMethodType = null;
		this.numberConstructorType = null;
		this.swapMethod = null;
		this.unswapMethod = null;
		this.namePropertyMethod = null;
		this.parentPropertyMethod = null;
		this.isDelegate = false;
		this.isAbstract = false;
		this.isMemberClass = false;
		this.primitiveDefault = null;
		this.remoteableMethods = null;
		this.publicMethods = null;
		this.beanContext = null;
		this.elementType = null;
		this.keyType = null;
		this.valueType = null;
		this.invocationHandler = null;
		this.beanMeta = null;
		this.typePropertyName = null;
		this.dictionaryName = null;
		this.notABeanReason = null;
		this.pojoSwaps = null;
		this.beanFilter = null;
		this.extMeta = new MetadataMap();
		this.initException = null;
		this.beanRegistry = null;
	}

	@SuppressWarnings({"unchecked","rawtypes","hiding"})
	private final class ClassMetaBuilder<T> {
		Class<T> innerClass;
		Class<? extends T> implClass;
		BeanContext beanContext;
		ClassCategory cc = ClassCategory.OTHER;
		boolean
			isDelegate = false,
			isMemberClass = false,
			isAbstract = false;
		Method
			fromStringMethod = null,
			swapMethod = null,
			unswapMethod = null;
		Setter
			parentPropertyMethod = null,
			namePropertyMethod = null;
		Constructor<T>
			noArgConstructor = null,
			stringConstructor = null,
			swapConstructor = null,
			numberConstructor = null;
		Class<?>
			swapMethodType = null,
			numberConstructorType = null;
		Object primitiveDefault = null;
		Map<String,Method>
			publicMethods = new LinkedHashMap<>(),
			remoteableMethods = new LinkedHashMap<>();
		ClassMeta<?>
			keyType = null,
			valueType = null,
			elementType = null,
			serializedClassMeta = null;
		String
			typePropertyName = null,
			notABeanReason = null,
			dictionaryName = null;
		Throwable initException = null;
		BeanMeta beanMeta = null;
		List<PojoSwap> pojoSwaps = new ArrayList<>();
		InvocationHandler invocationHandler = null;
		BeanRegistry beanRegistry = null;
		PojoSwap<?,?>[] childPojoSwaps;
		ConcurrentHashMap<Class<?>,PojoSwap<?,?>>
			childSwapMap,
			childUnswapMap;

		ClassMetaBuilder(Class<T> innerClass, BeanContext beanContext, Class<? extends T> implClass, BeanFilter beanFilter, PojoSwap<T,?>[] pojoSwaps, PojoSwap<?,?>[] childPojoSwaps) {
			this.innerClass = innerClass;
			this.beanContext = beanContext;

			this.implClass = implClass;
			this.childPojoSwaps = childPojoSwaps;
			this.childSwapMap = childPojoSwaps == null ? null : new ConcurrentHashMap<Class<?>,PojoSwap<?,?>>();
			this.childUnswapMap = childPojoSwaps == null ? null : new ConcurrentHashMap<Class<?>,PojoSwap<?,?>>();

			Class<T> c = innerClass;
			if (c.isPrimitive()) {
				if (c == Boolean.TYPE)
					cc = BOOLEAN;
				else if (c == Byte.TYPE || c == Short.TYPE || c == Integer.TYPE || c == Long.TYPE || c == Float.TYPE || c == Double.TYPE) {
					if (c == Float.TYPE || c == Double.TYPE)
						cc = DECIMAL;
					else
						cc = NUMBER;
				}
				else if (c == Character.TYPE)
					cc = CHAR;
				else if (c == void.class || c == Void.class)
					cc = VOID;
			} else {
				if (isParentClass(Delegate.class, c))
					isDelegate = true;

				if (c == Object.class)
					cc = OBJ;
				else if (c.isEnum())
					cc = ENUM;
				else if (c.equals(Class.class))
					cc = CLASS;
				else if (isParentClass(Method.class, c))
					cc = METHOD;
				else if (isParentClass(CharSequence.class, c)) {
					if (c.equals(String.class))
						cc = STR;
					else
						cc = CHARSEQ;
				}
				else if (isParentClass(Number.class, c)) {
					if (isParentClass(Float.class, c) || isParentClass(Double.class, c))
						cc = DECIMAL;
					else
						cc = NUMBER;
				}
				else if (isParentClass(Collection.class, c))
					cc = COLLECTION;
				else if (isParentClass(Map.class, c)) {
					if (isParentClass(BeanMap.class, c))
						cc = BEANMAP;
					else
						cc = MAP;
				}
				else if (c == Character.class)
					cc = CHAR;
				else if (c == Boolean.class)
					cc = BOOLEAN;
				else if (isParentClass(Date.class, c) || isParentClass(Calendar.class, c))
					cc = DATE;
				else if (c.isArray())
					cc = ARRAY;
				else if (isParentClass(URL.class, c) || isParentClass(URI.class, c) || c.isAnnotationPresent(org.apache.juneau.annotation.URI.class))
					cc = URI;
				else if (isParentClass(Reader.class, c))
					cc = READER;
				else if (isParentClass(InputStream.class, c))
					cc = INPUTSTREAM;
			}

			isMemberClass = c.isMemberClass() && ! isStatic(c);

			// Find static fromString(String) or equivalent method.
			// fromString() must be checked before valueOf() so that Enum classes can create their own
			//		specialized fromString() methods to override the behavior of Enum.valueOf(String).
			// valueOf() is used by enums.
			// parse() is used by the java logging Level class.
			// forName() is used by Class and Charset
			for (String methodName : new String[]{"fromString","fromValue","valueOf","parse","parseString","forName","forString"}) {
				if (fromStringMethod == null) {
					for (Method m : c.getMethods()) {
						if (isStatic(m) && isPublic(m) && isNotDeprecated(m)) {
							String mName = m.getName();
							if (mName.equals(methodName) && m.getReturnType() == c) {
								Class<?>[] args = m.getParameterTypes();
								if (args.length == 1 && args[0] == String.class) {
									fromStringMethod = m;
									break;
								}
							}
						}
					}
				}
			}

			// Special cases
			try {
				if (c == TimeZone.class)
					fromStringMethod = c.getMethod("getTimeZone", String.class);
				else if (c == Locale.class)
					fromStringMethod = LocaleAsString.class.getMethod("fromString", String.class);
			} catch (NoSuchMethodException e1) {}

			// Find swap() method if present.
			for (Method m : c.getMethods()) {
				if (isPublic(m) && isNotDeprecated(m) && ! isStatic(m)) {
					String mName = m.getName();
					if (mName.equals("swap")) {
						Class<?>[] pt = m.getParameterTypes();
						if (pt.length == 1 && pt[0] == BeanSession.class) {
							swapMethod = m;
							swapMethodType = m.getReturnType();
							break;
						}
					}
				}
			}
			// Find unswap() method if present.
			if (swapMethod != null) {
				for (Method m : c.getMethods()) {
					if (isPublic(m) && isNotDeprecated(m) && isStatic(m)) {
						String mName = m.getName();
						if (mName.equals("unswap")) {
							Class<?>[] pt = m.getParameterTypes();
							if (pt.length == 2 && pt[0] == BeanSession.class && pt[1] == swapMethodType) {
								unswapMethod = m;
								break;
							}
						}
					}
				}
			}

			for (Field f : getAllFields(c, true)) {
				if (f.isAnnotationPresent(ParentProperty.class)) {
					f.setAccessible(true);
					parentPropertyMethod = new Setter.FieldSetter(f);
				}
				if (f.isAnnotationPresent(NameProperty.class)) {
					f.setAccessible(true);
					namePropertyMethod = new Setter.FieldSetter(f);
				}
			}

			// Find @NameProperty and @ParentProperty methods if present.
			for (Method m : getAllMethods(c, true)) {
				if (m.isAnnotationPresent(ParentProperty.class) && m.getParameterTypes().length == 1) {
					m.setAccessible(true);
					parentPropertyMethod = new Setter.MethodSetter(m);
				}
				if (m.isAnnotationPresent(NameProperty.class) && m.getParameterTypes().length == 1) {
					m.setAccessible(true);
					namePropertyMethod = new Setter.MethodSetter(m);
				}
			}

			// Note:  Primitive types are normally abstract.
			isAbstract = Modifier.isAbstract(c.getModifiers()) && ! c.isPrimitive();

			// Find constructor(String) method if present.
			for (Constructor cs : c.getConstructors()) {
				if (isPublic(cs) && isNotDeprecated(cs)) {
					Class<?>[] args = cs.getParameterTypes();
					if (args.length == (isMemberClass ? 1 : 0) && c != Object.class && ! isAbstract) {
						noArgConstructor = cs;
					} else if (args.length == (isMemberClass ? 2 : 1)) {
						Class<?> arg = args[(isMemberClass ? 1 : 0)];
						if (arg == String.class)
							stringConstructor = cs;
						else if (swapMethodType != null && swapMethodType.isAssignableFrom(arg))
							swapConstructor = cs;
						else if (cc != NUMBER && (Number.class.isAssignableFrom(arg) || (arg.isPrimitive() && (arg == int.class || arg == short.class || arg == long.class || arg == float.class || arg == double.class)))) {
							numberConstructor = cs;
							numberConstructorType = getWrapperIfPrimitive(arg);
						}
					}
				}
			}

			primitiveDefault = ClassUtils.getPrimitiveDefault(c);

			for (Method m : c.getMethods())
				if (isPublic(m) && isNotDeprecated(m))
					publicMethods.put(getMethodSignature(m), m);

			Map<Class<?>,Remoteable> remoteableMap = findAnnotationsMap(Remoteable.class, c);
			if (! remoteableMap.isEmpty()) {
				Map.Entry<Class<?>,Remoteable> e = remoteableMap.entrySet().iterator().next();  // Grab the first one.
				Class<?> ic = e.getKey();
				Remoteable r = e.getValue();
				String methodPaths = r.methodPaths();
				String expose = r.expose();
				for (Method m : "DECLARED".equals(expose) ? ic.getDeclaredMethods() : ic.getMethods()) {
					if (isPublic(m)) {
						RemoteMethod rm = m.getAnnotation(RemoteMethod.class);
						if (rm != null || ! "ANNOTATED".equals(expose)) {
							String path = "NAME".equals(methodPaths) ? m.getName() : getMethodSignature(m);
							remoteableMethods.put(path, m);
						}
					}
				}
			}

			if (innerClass != Object.class) {
				noArgConstructor = (Constructor<T>)findNoArgConstructor(implClass == null ? innerClass : implClass, Visibility.PUBLIC);
			}

			if (beanFilter == null)
				beanFilter = findBeanFilter();

			if (swapMethod != null) {
				final Method fSwapMethod = swapMethod;
				final Method fUnswapMethod = unswapMethod;
				final Constructor<T> fSwapConstructor = swapConstructor;
				this.pojoSwaps.add(
					new PojoSwap<T,Object>(c, swapMethod.getReturnType()) {
						@Override
						public Object swap(BeanSession session, Object o) throws SerializeException {
							try {
								return fSwapMethod.invoke(o, session);
							} catch (Exception e) {
								throw new SerializeException(e);
							}
						}
						@Override
						public T unswap(BeanSession session, Object f, ClassMeta<?> hint) throws ParseException {
							try {
								if (fUnswapMethod != null)
									return (T)fUnswapMethod.invoke(null, session, f);
								if (fSwapConstructor != null)
									return fSwapConstructor.newInstance(f);
								return super.unswap(session, f, hint);
							} catch (Exception e) {
								throw new ParseException(e);
							}
						}
					}
				);
			}

			if (pojoSwaps != null)
				this.pojoSwaps.addAll(Arrays.asList(pojoSwaps));

			findPojoSwaps(this.pojoSwaps);

			try {

				// If this is an array, get the element type.
				if (cc == ARRAY)
					elementType = findClassMeta(innerClass.getComponentType());

				// If this is a MAP, see if it's parameterized (e.g. AddressBook extends HashMap<String,Person>)
				else if (cc == MAP) {
					ClassMeta[] parameters = findParameters();
					if (parameters != null && parameters.length == 2) {
						keyType = parameters[0];
						valueType = parameters[1];
					} else {
						keyType = findClassMeta(Object.class);
						valueType = findClassMeta(Object.class);
					}
				}

				// If this is a COLLECTION, see if it's parameterized (e.g. AddressBook extends LinkedList<Person>)
				else if (cc == COLLECTION) {
					ClassMeta[] parameters = findParameters();
					if (parameters != null && parameters.length == 1) {
						elementType = parameters[0];
					} else {
						elementType = findClassMeta(Object.class);
					}
				}

				// If the category is unknown, see if it's a bean.
				// Note that this needs to be done after all other initialization has been done.
				else if (cc == OTHER) {

					BeanMeta newMeta = null;
					try {
						newMeta = new BeanMeta(ClassMeta.this, beanContext, beanFilter, null);
						notABeanReason = newMeta.notABeanReason;

						// Always get these even if it's not a bean:
						beanRegistry = newMeta.beanRegistry;
						typePropertyName = newMeta.typePropertyName;

					} catch (RuntimeException e) {
						notABeanReason = e.getMessage();
						throw e;
					}
					if (notABeanReason == null)
						beanMeta = newMeta;
				}

			} catch (NoClassDefFoundError e) {
				initException = e;
			} catch (RuntimeException e) {
				initException = e;
				throw e;
			}

			if (beanMeta != null)
				dictionaryName = beanMeta.getDictionaryName();

			serializedClassMeta = (this.pojoSwaps.isEmpty() ? ClassMeta.this : findClassMeta(this.pojoSwaps.get(0).getSwapClass()));
			if (serializedClassMeta == null)
				serializedClassMeta = ClassMeta.this;

			if (beanMeta != null && beanContext != null && beanContext.useInterfaceProxies && innerClass.isInterface())
				invocationHandler = new BeanProxyInvocationHandler<T>(beanMeta);

			Bean b = c.getAnnotation(Bean.class);
			if (b != null && b.beanDictionary().length != 0)
				beanRegistry = new BeanRegistry(beanContext, null, b.beanDictionary());
		}

		private BeanFilter findBeanFilter() {
			try {
				Map<Class<?>,Bean> ba = findAnnotationsMap(Bean.class, innerClass);
				if (! ba.isEmpty())
					return new AnnotationBeanFilterBuilder(beanContext, innerClass, ba).build();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return null;
		}

		private void findPojoSwaps(List<PojoSwap> l) {
			Swap swap = innerClass.getAnnotation(Swap.class);
			if (swap != null)
				l.add(createPojoSwap(swap));
			Swaps swaps = innerClass.getAnnotation(Swaps.class);
			if (swaps != null)
				for (Swap s : swaps.value())
					l.add(createPojoSwap(s));
		}

		private PojoSwap<T,?> createPojoSwap(Swap s) {
			Class<?> c = s.value();
			if (c == Null.class)
				c = s.impl();

			if (isParentClass(PojoSwap.class, c)) {
				PojoSwap ps = beanContext.newInstance(PojoSwap.class, c);
				if (s.mediaTypes().length > 0)
					ps.forMediaTypes(MediaType.forStrings(s.mediaTypes()));
				if (! s.template().isEmpty())
					ps.withTemplate(s.template());
				return ps;
			}

			if (isParentClass(SurrogateSwap.class, c))
				throw new FormattedRuntimeException("TODO - Surrogate classes currently not supported in @Swap annotation", c);

			throw new FormattedRuntimeException("Invalid swap class ''{0}'' specified.  Must extend from PojoSwap or Surrogate.", c);
		}

		private ClassMeta<?> findClassMeta(Class<?> c) {
			return beanContext.getClassMeta(c, false);
		}

		private ClassMeta<?>[] findParameters() {
			return beanContext.findParameters(innerClass, innerClass);
		}
	}


	/**
	 * Returns the type property name associated with this class and subclasses.
	 *
	 * <p>
	 * If <jk>null</jk>, <js>"_type"</js> should be assumed.
	 *
	 * @return
	 * 	The type property name associated with this bean class, or <jk>null</jk> if there is no explicit type
	 * 	property name defined or this isn't a bean.
	 */
	public String getBeanTypePropertyName() {
		return typePropertyName;
	}

	/**
	 * Returns the bean dictionary name associated with this class.
	 *
	 * <p>
	 * The lexical name is defined by {@link Bean#typeName()}.
	 *
	 * @return
	 * 	The type name associated with this bean class, or <jk>null</jk> if there is no type name defined or this
	 * 	isn't a bean.
	 */
	public String getDictionaryName() {
		return dictionaryName;
	}

	/**
	 * Returns the bean registry for this class.
	 *
	 * <p>
	 * This bean registry contains names specified in the {@link Bean#beanDictionary()} annotation defined on the class,
	 * regardless of whether the class is an actual bean.
	 * This allows interfaces to define subclasses with type names.
	 *
	 * @return The bean registry for this class, or <jk>null</jk> if no bean registry is associated with it.
	 */
	public BeanRegistry getBeanRegistry() {
		return beanRegistry;
	}

	/**
	 * Returns the category of this class.
	 *
	 * @return The category of this class.
	 */
	public ClassCategory getClassCategory() {
		return cc;
	}

	/**
	 * Returns <jk>true</jk> if this class is a superclass of or the same as the specified class.
	 *
	 * @param c The comparison class.
	 * @return <jk>true</jk> if this class is a superclass of or the same as the specified class.
	 */
	public boolean isAssignableFrom(Class<?> c) {
		return isParentClass(innerClass, c);
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of or the same as the specified class.
	 *
	 * @param c The comparison class.
	 * @return <jk>true</jk> if this class is a subclass of or the same as the specified class.
	 */
	public boolean isInstanceOf(Class<?> c) {
		return isParentClass(c, innerClass);
	}

	/**
	 * Returns <jk>true</jk> if this class or any child classes has a {@link PojoSwap} associated with it.
	 *
	 * <p>
	 * Used when transforming bean properties to prevent having to look up transforms if we know for certain that no
	 * transforms are associated with a bean property.
	 *
	 * @return <jk>true</jk> if this class or any child classes has a {@link PojoSwap} associated with it.
	 */
	protected boolean hasChildPojoSwaps() {
		return childPojoSwaps != null;
	}

	/**
	 * Returns the {@link PojoSwap} where the specified class is the same/subclass of the normal class of one of the
	 * child POJO swaps associated with this class.
	 *
	 * @param normalClass The normal class being resolved.
	 * @return The resolved {@link PojoSwap} or <jk>null</jk> if none were found.
	 */
	protected PojoSwap<?,?> getChildPojoSwapForSwap(Class<?> normalClass) {
		if (childSwapMap != null) {
			PojoSwap<?,?> s = childSwapMap.get(normalClass);
			if (s == null) {
				for (PojoSwap<?,?> f : childPojoSwaps)
					if (s == null && isParentClass(f.getNormalClass(), normalClass))
						s = f;
				if (s == null)
					s = PojoSwap.NULL;
				PojoSwap<?,?> s2 = childSwapMap.putIfAbsent(normalClass, s);
				if (s2 != null)
					s = s2;
			}
			if (s == PojoSwap.NULL)
				return null;
			return s;
		}
		return null;
	}

	/**
	 * Returns the {@link PojoSwap} where the specified class is the same/subclass of the swap class of one of the child
	 * POJO swaps associated with this class.
	 *
	 * @param swapClass The swap class being resolved.
	 * @return The resolved {@link PojoSwap} or <jk>null</jk> if none were found.
	 */
	protected PojoSwap<?,?> getChildPojoSwapForUnswap(Class<?> swapClass) {
		if (childUnswapMap != null) {
			PojoSwap<?,?> s = childUnswapMap.get(swapClass);
			if (s == null) {
				for (PojoSwap<?,?> f : childPojoSwaps)
					if (s == null && isParentClass(f.getSwapClass(), swapClass))
						s = f;
				if (s == null)
					s = PojoSwap.NULL;
				PojoSwap<?,?> s2 = childUnswapMap.putIfAbsent(swapClass, s);
				if (s2 != null)
					s = s2;
			}
			if (s == PojoSwap.NULL)
				return null;
			return s;
		}
		return null;
	}

	/**
	 * Locates the no-arg constructor for the specified class.
	 *
	 * <p>
	 * Constructor must match the visibility requirements specified by parameter 'v'.
	 * If class is abstract, always returns <jk>null</jk>.
	 * Note that this also returns the 1-arg constructor for non-static member classes.
	 *
	 * @param c The class from which to locate the no-arg constructor.
	 * @param v The minimum visibility.
	 * @return The constructor, or <jk>null</jk> if no no-arg constructor exists with the required visibility.
	 */
	@SuppressWarnings({"rawtypes","unchecked"})
	protected static <T> Constructor<? extends T> findNoArgConstructor(Class<?> c, Visibility v) {
		int mod = c.getModifiers();
		if (Modifier.isAbstract(mod))
			return null;
		boolean isMemberClass = c.isMemberClass() && ! isStatic(c);
		for (Constructor cc : c.getConstructors()) {
			mod = cc.getModifiers();
			if (cc.getParameterTypes().length == (isMemberClass ? 1 : 0) && v.isVisible(mod) && isNotDeprecated(cc))
				return v.transform(cc);
		}
		return null;
	}

	/**
	 * Returns the {@link Class} object that this class type wraps.
	 *
	 * @return The wrapped class object.
	 */
	public Class<T> getInnerClass() {
		return innerClass;
	}

	/**
	 * Returns the serialized (swapped) form of this class if there is an {@link PojoSwap} associated with it.
	 *
	 * @param session
	 * 	The bean session.
	 * 	<br>Required because the swap used may depend on the media type being serialized or parsed.
	 * @return The serialized class type, or this object if no swap is associated with the class.
	 */
	@BeanIgnore
	public ClassMeta<?> getSerializedClassMeta(BeanSession session) {
		PojoSwap<T,?> ps = getPojoSwap(session);
		return (ps == null ? this : ps.getSwapClassMeta(session));
	}

	/**
	 * For array and {@code Collection} types, returns the class type of the components of the array or
	 * {@code Collection}.
	 *
	 * @return The element class type, or <jk>null</jk> if this class is not an array or Collection.
	 */
	public ClassMeta<?> getElementType() {
		return elementType;
	}

	/**
	 * For {@code Map} types, returns the class type of the keys of the {@code Map}.
	 *
	 * @return The key class type, or <jk>null</jk> if this class is not a Map.
	 */
	public ClassMeta<?> getKeyType() {
		return keyType;
	}

	/**
	 * For {@code Map} types, returns the class type of the values of the {@code Map}.
	 *
	 * @return The value class type, or <jk>null</jk> if this class is not a Map.
	 */
	public ClassMeta<?> getValueType() {
		return valueType;
	}

	/**
	 * Returns <jk>true</jk> if this class implements {@link Delegate}, meaning it's a representation of some other
	 * object.
	 *
	 * @return <jk>true</jk> if this class implements {@link Delegate}.
	 */
	public boolean isDelegate() {
		return isDelegate;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Map}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Map}.
	 */
	public boolean isMap() {
		return cc == MAP || cc == BEANMAP;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Map} or it's a bean.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Map} or it's a bean.
	 */
	public boolean isMapOrBean() {
		return cc == MAP || cc == BEANMAP || beanMeta != null;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link BeanMap}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link BeanMap}.
	 */
	public boolean isBeanMap() {
		return cc == BEANMAP;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Collection}.
	 */
	public boolean isCollection() {
		return cc == COLLECTION;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection} or is an array.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Collection} or is an array.
	 */
	public boolean isCollectionOrArray() {
		return cc == COLLECTION || cc == ARRAY;
	}

	/**
	 * Returns <jk>true</jk> if this class is {@link Class}.
	 *
	 * @return <jk>true</jk> if this class is {@link Class}.
	 */
	public boolean isClass() {
		return cc == CLASS;
	}

	/**
	 * Returns <jk>true</jk> if this class is {@link Method}.
	 *
	 * @return <jk>true</jk> if this class is {@link Method}.
	 */
	public boolean isMethod() {
		return cc == METHOD;
	}

	/**
	 * Returns <jk>true</jk> if this class is an {@link Enum}.
	 *
	 * @return <jk>true</jk> if this class is an {@link Enum}.
	 */
	public boolean isEnum() {
		return cc == ENUM;
	}

	/**
	 * Returns <jk>true</jk> if this class is an array.
	 *
	 * @return <jk>true</jk> if this class is an array.
	 */
	public boolean isArray() {
		return cc == ARRAY;
	}

	/**
	 * Returns <jk>true</jk> if this class is a bean.
	 *
	 * @return <jk>true</jk> if this class is a bean.
	 */
	public boolean isBean() {
		return beanMeta != null;
	}

	/**
	 * Returns <jk>true</jk> if this class is {@link Object}.
	 *
	 * @return <jk>true</jk> if this class is {@link Object}.
	 */
	public boolean isObject() {
		return cc == OBJ;
	}

	/**
	 * Returns <jk>true</jk> if this class is not {@link Object}.
	 *
	 * @return <jk>true</jk> if this class is not {@link Object}.
	 */
	public boolean isNotObject() {
		return cc != OBJ;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Number}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Number}.
	 */
	public boolean isNumber() {
		return cc == NUMBER || cc == DECIMAL;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
	 */
	public boolean isDecimal() {
		return cc == DECIMAL;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Boolean}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Boolean}.
	 */
	public boolean isBoolean() {
		return cc == BOOLEAN;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link CharSequence}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link CharSequence}.
	 */
	public boolean isCharSequence() {
		return cc == STR || cc == CHARSEQ;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link String}.
	 *
	 * @return <jk>true</jk> if this class is a {@link String}.
	 */
	public boolean isString() {
		return cc == STR;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Character}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Character}.
	 */
	public boolean isChar() {
		return cc == CHAR;
	}

	/**
	 * Returns <jk>true</jk> if this class is a primitive.
	 *
	 * @return <jk>true</jk> if this class is a primitive.
	 */
	public boolean isPrimitive() {
		return innerClass.isPrimitive();
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Date} or {@link Calendar}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Date} or {@link Calendar}.
	 */
	public boolean isDate() {
		return cc == DATE;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link URI} or {@link URL}.
	 *
	 * @return <jk>true</jk> if this class is a {@link URI} or {@link URL}.
	 */
	public boolean isUri() {
		return cc == URI;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Reader}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Reader}.
	 */
	public boolean isReader() {
		return cc == READER;
	}

	/**
	 * Returns <jk>true</jk> if this class is an {@link InputStream}.
	 *
	 * @return <jk>true</jk> if this class is an {@link InputStream}.
	 */
	public boolean isInputStream() {
		return cc == INPUTSTREAM;
	}

	/**
	 * Returns <jk>true</jk> if this class is {@link Void} or <jk>void</jk>.
	 *
	 * @return <jk>true</jk> if this class is {@link Void} or <jk>void</jk>.
	 */
	public boolean isVoid() {
		return cc == VOID;
	}

	/**
	 * Returns <jk>true</jk> if this metadata represents an array of argument types.
	 *
	 * @return <jk>true</jk> if this metadata represents an array of argument types.
	 */
	public boolean isArgs() {
		return cc == ARGS;
	}

	/**
	 * Returns the argument types of this meta.
	 *
	 * @return The argument types of this meta, or <jk>null</jk> if this isn't an array of argument types.
	 */
	public ClassMeta<?>[] getArgs() {
		return args;
	}

	/**
	 * Returns the argument metadata at the specified index if this is an args metadata object.
	 *
	 * @param index The argument index.
	 * @return The The argument metadata.  Never <jk>null</jk>.
	 * @throws BeanRuntimeException If this metadata object is not a list of arguments, or the index is out of range.
	 */
	public ClassMeta<?> getArg(int index) {
		if (args != null && index >= 0 && index < args.length)
			return args[index];
		throw new BeanRuntimeException("Invalid argument index specified:  {0}.  Only {1} arguments are defined.", index, args == null ? 0 : args.length);
	}

	/**
	 * Returns <jk>true</jk> if instance of this object can be <jk>null</jk>.
	 *
	 * <p>
	 * Objects can be <jk>null</jk>, but primitives cannot, except for chars which can be represented by
	 * <code>(<jk>char</jk>)0</code>.
	 *
	 * @return <jk>true</jk> if instance of this class can be null.
	 */
	public boolean isNullable() {
		if (innerClass.isPrimitive())
			return cc == CHAR;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this class or one of it's methods are annotated with {@link Remoteable @Remotable}.
	 *
	 * @return <jk>true</jk> if this class is remoteable.
	 */
	public boolean isRemoteable() {
		return remoteableMethods != null;
	}

	/**
	 * Returns <jk>true</jk> if this class is abstract.
	 *
	 * @return <jk>true</jk> if this class is abstract.
	 */
	public boolean isAbstract() {
		return isAbstract;
	}

	/**
	 * Returns <jk>true</jk> if this class is an inner class.
	 *
	 * @return <jk>true</jk> if this class is an inner class.
	 */
	public boolean isMemberClass() {
		return isMemberClass;
	}

	/**
	 * All methods on this class annotated with {@link Remoteable @Remotable}, or all public methods if class is
	 * annotated.
	 *
	 * <p>
	 * Keys are method signatures.
	 *
	 * @return All remoteable methods on this class.
	 */
	public Map<String,Method> getRemoteableMethods() {
		return remoteableMethods;
	}

	/**
	 * All public methods on this class including static methods.
	 *
	 * <p>
	 * Keys are method signatures.
	 *
	 * @return The public methods on this class.
	 */
	public Map<String,Method> getPublicMethods() {
		return publicMethods;
	}

	/**
	 * Returns the {@link PojoSwap} associated with this class that's the best match for the specified session.
	 *
	 * @param session
	 * 	The current bean session.
	 * 	<br>If multiple swaps are associated with a class, only the first one with a matching media type will
	 * 	be returned.
	 * @return
	 * 	The {@link PojoSwap} associated with this class, or <jk>null</jk> if there are no POJO swaps associated with
	 * 	this class.
	 */
	public PojoSwap<T,?> getPojoSwap(BeanSession session) {
		if (pojoSwaps != null) {
			int matchQuant = 0, matchIndex = -1;

			for (int i = 0; i < pojoSwaps.length; i++) {
				int q = pojoSwaps[i].match(session);
				if (q > matchQuant) {
					matchQuant = q;
					matchIndex = i;
				}
			}

			if (matchIndex > -1)
				return pojoSwaps[matchIndex];
		}
		return null;
	}

	/**
	 * Returns the {@link BeanMeta} associated with this class.
	 *
	 * @return
	 * 	The {@link BeanMeta} associated with this class, or <jk>null</jk> if there is no bean meta associated with
	 * 	this class.
	 */
	public BeanMeta<T> getBeanMeta() {
		return beanMeta;
	}

	/**
	 * Returns the no-arg constructor for this class.
	 *
	 * @return The no-arg constructor for this class, or <jk>null</jk> if it does not exist.
	 */
	public Constructor<? extends T> getConstructor() {
		return noArgConstructor;
	}

	/**
	 * Returns the language-specified extended metadata on this class.
	 *
	 * @param c The name of the metadata class to create.
	 * @return Extended metadata on this class.  Never <jk>null</jk>.
	 */
	public <M extends ClassMetaExtended> M getExtendedMeta(Class<M> c) {
		return extMeta.get(c, this);
	}

	/**
	 * Returns the interface proxy invocation handler for this class.
	 *
	 * @return The interface proxy invocation handler, or <jk>null</jk> if it does not exist.
	 */
	public InvocationHandler getProxyInvocationHandler() {
		return invocationHandler;
	}

	/**
	 * Returns <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 *
	 * @return <jk>true</jk> if a new instance of this class can be constructed.
	 */
	public boolean canCreateNewInstance() {
		if (isMemberClass)
			return false;
		if (noArgConstructor != null)
			return true;
		if (getProxyInvocationHandler() != null)
			return true;
		if (isArray() && elementType.canCreateNewInstance())
			return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 * Returns <jk>false</jk> if this is a non-static member class and the outer object does not match the class type of
	 * the defining class.
	 *
	 * @param outer
	 * 	The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return
	 * 	<jk>true</jk> if a new instance of this class can be created within the context of the specified outer object.
	 */
	public boolean canCreateNewInstance(Object outer) {
		if (isMemberClass)
			return outer != null && noArgConstructor != null && noArgConstructor.getParameterTypes()[0] == outer.getClass();
		return canCreateNewInstance();
	}

	/**
	 * Returns <jk>true</jk> if this class can be instantiated as a bean.
	 * Returns <jk>false</jk> if this is a non-static member class and the outer object does not match the class type of
	 * the defining class.
	 *
	 * @param outer
	 * 	The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return
	 * 	<jk>true</jk> if a new instance of this bean can be created within the context of the specified outer object.
	 */
	public boolean canCreateNewBean(Object outer) {
		if (beanMeta == null)
			return false;
		if (beanMeta.constructor == null)
			return false;
		if (isMemberClass)
			return outer != null && beanMeta.constructor.getParameterTypes()[0] == outer.getClass();
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
	 *
	 * @param outer
	 * 	The outer class object for non-static member classes.
	 * 	Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 */
	public boolean canCreateNewInstanceFromString(Object outer) {
		if (fromStringMethod != null)
			return true;
		if (stringConstructor != null) {
			if (isMemberClass)
				return outer != null && stringConstructor.getParameterTypes()[0] == outer.getClass();
			return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
	 *
	 * @param outer
	 * 	The outer class object for non-static member classes.
	 * 	Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 */
	public boolean canCreateNewInstanceFromNumber(Object outer) {
		if (numberConstructor != null) {
			if (isMemberClass)
				return outer != null && numberConstructor.getParameterTypes()[0] == outer.getClass();
			return true;
		}
		return false;
	}

	/**
	 * Returns the class type of the parameter of the numeric constructor.
	 *
	 * @return The class type of the numeric constructor, or <jk>null</jk> if no such constructor exists.
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends Number> getNewInstanceFromNumberClass() {
		return (Class<? extends Number>) numberConstructorType;
	}

	/**
	 * Returns the method or field annotated with {@link NameProperty @NameProperty}.
	 *
	 * @return
	 * 	The method or field  annotated with {@link NameProperty @NameProperty} or <jk>null</jk> if method does not
	 * 	exist.
	 */
	public Setter getNameProperty() {
		return namePropertyMethod;
	}

	/**
	 * Returns the method or field annotated with {@link ParentProperty @ParentProperty}.
	 *
	 * @return
	 * 	The method or field annotated with {@link ParentProperty @ParentProperty} or <jk>null</jk> if method does not
	 * 	exist.
	 */
	public Setter getParentProperty() {
		return parentPropertyMethod;
	}

	/**
	 * Returns the reason why this class is not a bean, or <jk>null</jk> if it is a bean.
	 *
	 * @return The reason why this class is not a bean, or <jk>null</jk> if it is a bean.
	 */
	public synchronized String getNotABeanReason() {
		return notABeanReason;
	}

	/**
	 * Returns any exception that was throw in the <code>init()</code> method.
	 *
	 * @return The cached exception.
	 */
	public Throwable getInitException() {
		return initException;
	}

	/**
	 * Returns the {@link BeanContext} that created this object.
	 *
	 * @return The bean context.
	 */
	public BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns the default value for primitives such as <jk>int</jk> or <jk>Integer</jk>.
	 *
	 * @return The default value, or <jk>null</jk> if this class type is not a primitive.
	 */
	@SuppressWarnings("unchecked")
	public T getPrimitiveDefault() {
		return (T)primitiveDefault;
	}

	/**
	 * Create a new instance of the main class of this declared type from a <code>String</code> input.
	 *
	 * <p>
	 * In order to use this method, the class must have one of the following methods:
	 * <ul>
	 * 	<li><code><jk>public static</jk> T valueOf(String in);</code>
	 * 	<li><code><jk>public static</jk> T fromString(String in);</code>
	 * 	<li><code><jk>public</jk> T(String in);</code>
	 * </ul>
	 *
	 * @param outer
	 * 	The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @param arg The input argument value.
	 * @return A new instance of the object, or <jk>null</jk> if there is no string constructor on the object.
	 * @throws IllegalAccessException
	 * 	If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is
	 * 	inaccessible.
	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
	 * @throws InstantiationException
	 * 	If the class that declares the underlying constructor represents an abstract class, or does not have one of
	 * 	the methods described above.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	@SuppressWarnings("unchecked")
	public T newInstanceFromString(Object outer, String arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Method m = fromStringMethod;
		if (m != null)
			return (T)m.invoke(null, arg);
		Constructor<T> c = stringConstructor;
		if (c != null) {
			if (isMemberClass)
				return c.newInstance(outer, arg);
			return c.newInstance(arg);
		}
		throw new InstantiationError("No string constructor or valueOf(String) method found for class '"+getInnerClass().getName()+"'");
	}

	/**
	 * Create a new instance of the main class of this declared type from a <code>Number</code> input.
	 *
	 * <p>
	 * In order to use this method, the class must have one of the following methods:
	 * <ul>
	 * 	<li><code><jk>public</jk> T(Number in);</code>
	 * </ul>
	 *
	 * @param session The current bean session.
	 * @param outer
	 * 	The outer class object for non-static member classes.
	 * 	Can be <jk>null</jk> for non-member or static classes.
	 * @param arg The input argument value.
	 * @return A new instance of the object, or <jk>null</jk> if there is no numeric constructor on the object.
	 * @throws IllegalAccessException
	 * 	If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is
	 * 	inaccessible.
	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
	 * @throws InstantiationException
	 * 	If the class that declares the underlying constructor represents an abstract class, or does not have one of
	 * 	the methods described above.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	public T newInstanceFromNumber(BeanSession session, Object outer, Number arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Constructor<T> c = numberConstructor;
		if (c != null) {
			Object arg2 = session.convertToType(arg, numberConstructor.getParameterTypes()[0]);
			if (isMemberClass)
				return c.newInstance(outer, arg2);
			return c.newInstance(arg2);
		}
		throw new InstantiationError("No string constructor or valueOf(Number) method found for class '"+getInnerClass().getName()+"'");
	}

	/**
	 * Create a new instance of the main class of this declared type.
	 *
	 * @return A new instance of the object, or <jk>null</jk> if there is no no-arg constructor on the object.
	 * @throws IllegalAccessException
	 * 	If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is
	 * 	inaccessible.
	 * @throws IllegalArgumentException
	 * 	If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			The number of actual and formal parameters differ.
	 * 		<li>
	 * 			An unwrapping conversion for primitive arguments fails.
	 * 		<li>
	 * 			A parameter value cannot be converted to the corresponding formal parameter type by a method invocation
	 * 			conversion.
	 * 		<li>
	 * 			The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	@SuppressWarnings("unchecked")
	public T newInstance() throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (isArray())
			return (T)Array.newInstance(getInnerClass().getComponentType(), 0);
		Constructor<? extends T> c = getConstructor();
		if (c != null)
			return c.newInstance((Object[])null);
		InvocationHandler h = getProxyInvocationHandler();
		if (h != null)
			return (T)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { getInnerClass(), java.io.Serializable.class }, h);
		if (isArray())
			return (T)Array.newInstance(this.elementType.innerClass,0);
		return null;
	}

	/**
	 * Same as {@link #newInstance()} except for instantiating non-static member classes.
	 *
	 * @param outer
	 * 	The instance of the owning object of the member class instance.
	 * 	Can be <jk>null</jk> if instantiating a non-member or static class.
	 * @return A new instance of the object, or <jk>null</jk> if there is no no-arg constructor on the object.
	 * @throws IllegalAccessException
	 * 	If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is
	 * 	inaccessible.
	 * @throws IllegalArgumentException
	 * 	If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			The number of actual and formal parameters differ.
	 * 		<li>
	 * 			An unwrapping conversion for primitive arguments fails.
	 * 		<li>
	 * 			A parameter value cannot be converted to the corresponding formal parameter type by a method invocation
	 * 			conversion.
	 * 		<li>
	 * 			The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	public T newInstance(Object outer) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (isMemberClass)
			return noArgConstructor.newInstance(outer);
		return newInstance();
	}

	/**
	 * Checks to see if the specified class type is the same as this one.
	 *
	 * @param t The specified class type.
	 * @return <jk>true</jk> if the specified class type is the same as the class for this type.
	 */
	@Override /* Object */
	public boolean equals(Object t) {
		if (t == null || ! (t instanceof ClassMeta))
			return false;
		ClassMeta<?> t2 = (ClassMeta<?>)t;
		return t2.getInnerClass() == this.getInnerClass();
	}

	/**
	 * Similar to {@link #equals(Object)} except primitive and Object types that are similar are considered the same.
	 * (e.g. <jk>boolean</jk> == <code>Boolean</code>).
	 *
	 * @param cm The class meta to compare to.
	 * @return <jk>true</jk> if the specified class-meta is equivalent to this one.
	 */
	public boolean same(ClassMeta<?> cm) {
		if (equals(cm))
			return true;
		return (isPrimitive() && cc == cm.cc);
	}

	@Override /* Object */
	public String toString() {
		return toString(false);
	}

	/**
	 * Same as {@link #toString()} except use simple class names.
	 *
	 * @param simple Print simple class names only (no package).
	 * @return A new string.
	 */
	public String toString(boolean simple) {
		return toString(new StringBuilder(), simple).toString();
	}

	/**
	 * Appends this object as a readable string to the specified string builder.
	 *
	 * @param sb The string builder to append this object to.
	 * @param simple Print simple class names only (no package).
	 * @return The same string builder passed in (for method chaining).
	 */
	protected StringBuilder toString(StringBuilder sb, boolean simple) {
		String n = innerClass.getName();
		if (simple) {
			int i = n.lastIndexOf('.');
			n = n.substring(i == -1 ? 0 : i+1).replace('$', '.');
		}
		if (cc == ARRAY)
			return elementType.toString(sb, simple).append('[').append(']');
		if (cc == MAP)
			return sb.append(n).append(keyType.isObject() && valueType.isObject() ? "" : "<"+keyType.toString(simple)+","+valueType.toString(simple)+">");
		if (cc == BEANMAP)
			return sb.append(BeanMap.class.getName()).append('<').append(n).append('>');
		if (cc == COLLECTION)
			return sb.append(n).append(elementType.isObject() ? "" : "<"+elementType.toString(simple)+">");
		if (cc == OTHER && beanMeta == null) {
			if (simple)
				return sb.append(n);
			sb.append("OTHER-").append(n).append(",notABeanReason=").append(notABeanReason);
			if (initException != null)
				sb.append(",initException=").append(initException);
			return sb;
		}
		return sb.append(n);
	}

	/**
	 * Returns <jk>true</jk> if the specified object is an instance of this class.
	 *
	 * <p>
	 * This is a simple comparison on the base class itself and not on any generic parameters.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is an instance of this class.
	 */
	public boolean isInstance(Object o) {
		if (o != null)
			return isParentClass(this.innerClass, o.getClass());
		return false;
	}

	/**
	 * Returns a readable name for this class (e.g. <js>"java.lang.String"</js>, <js>"boolean[]"</js>).
	 *
	 * @return The readable name for this class.
	 */
	public String getReadableName() {
		return getReadableClassName(this.innerClass);
	}

	private static class LocaleAsString {
		private static Method forLanguageTagMethod;
		static {
			try {
				forLanguageTagMethod = Locale.class.getMethod("forLanguageTag", String.class);
			} catch (NoSuchMethodException e) {}
		}

		@SuppressWarnings("unused")
		public static final Locale fromString(String localeString) {
			if (forLanguageTagMethod != null) {
				if (localeString.indexOf('_') != -1)
					localeString = localeString.replace('_', '-');
				try {
					return (Locale)forLanguageTagMethod.invoke(null, localeString);
				} catch (Exception e) {
					throw new BeanRuntimeException(e);
				}
			}
			String[] v = localeString.toString().split("[\\-\\_]");
			if (v.length == 1)
				return new Locale(v[0]);
			else if (v.length == 2)
				return new Locale(v[0], v[1]);
			else if (v.length == 3)
				return new Locale(v[0], v[1], v[2]);
			throw new BeanRuntimeException("Could not convert string ''{0}'' to a Locale.", localeString);
		}
	}

	@Override /* Object */
	public int hashCode() {
		return super.hashCode();
	}
}
