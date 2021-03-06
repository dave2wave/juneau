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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.beans.*;
import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.transform.*;

/**
 * Used to tailor how beans get interpreted by the framework.
 *
 * <p>
 * Can be used to do the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Explicitly specify the set and order of properties on a bean.
 * 	<li>
 * 		Associate a {@link PropertyNamer} with a class.
 * 	<li>
 * 		Specify subtypes of a bean differentiated by a sub type property.
 * </ul>
 *
 * <p>
 * This annotation can be applied to classes and interfaces.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Bean {

	/**
	 * An identifying name for this class.
	 *
	 * <p>
	 * The name is used to identify the class type during parsing when it cannot be inferred through reflection.
	 * For example, if a bean property is of type <code>Object</code>, then the serializer will add the name to the
	 * output so that the class can be determined during parsing.
	 * It is also used to specify element names in XML.
	 *
	 * <p>
	 * The name is used in combination with the bean dictionary defined through {@link BeanProperty#beanDictionary()} or
	 * {@link BeanContext#BEAN_beanDictionary}.
	 * Together, they make up a simple name/value mapping of names to classes.
	 * Names do not need to be universally unique.
	 * However, they must be unique within a dictionary.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@Bean</ja>(typeName=<js>"foo"</js>)
	 * 	<jk>public class</jk> Foo {
	 * 		<jc>// A bean property where the object types cannot be inferred since it's an Object[].</jc>
	 * 		<ja>@BeanProperty</ja>(beanDictionary={Bar.<jk>class</jk>,Baz.<jk>class</jk>})
	 * 		<jk>public</jk> Object[] x = <jk>new</jk> Object[]{<jk>new</jk> Bar(), <jk>new</jk> Baz()};
	 * 	}
	 *
	 * 	<ja>@Bean</ja>(typeName=<js>"bar"</js>)
	 * 	<jk>public class</jk> Bar {}
	 *
	 * 	<ja>@Bean</ja>(typeName=<js>"baz"</js>)
	 * 	<jk>public class</jk> Baz {}
	 * </p>
	 *
	 * <p>
	 * When serialized as XML, the bean is rendered as:
	 * <p class='bcode'>
	 * 	<xt>&lt;foo&gt;</xt>
	 * 		<xt>&lt;x&gt;</xt>
	 * 			<xt>&lt;bar/&gt;</xt>
	 * 			<xt>&lt;baz/&gt;</xt>
	 * 		<xt>&lt;/x&gt;</xt>
	 * 	<xt>&lt;/foo&gt;</xt>
	 * </p>
	 *
	 * <p>
	 * When serialized as JSON, <js>'n'</js> attributes would be added when needed to infer the type during parsing:
	 * <p class='bcode'>
	 * 	{
	 * 		x: [
	 * 			{_type:<js>'bar'</js>},
	 * 			{_type:<js>'baz'</js>}
	 * 		]
	 * 	}
	 * </p>
	 */
	String typeName() default "";


	/**
	 * The property name to use for representing the type name.
	 *
	 * <p>
	 * This can be used to override the name used for the <js>"_type"</js> property designated above.
	 * Typically, you'll define this on an interface class so that it can apply to all subclasses.
	 *
	 * <p class='bcode'>
	 * 	<ja>@Bean</ja>(typePropertyName=<js>"mytype"</js>, beanDictionary={MyClass1.<jk>class</jk>,MyClass2.<jk>class</jk>})
	 * 	<jk>public interface</jk> MyInterface {...}
	 *
	 * 	<ja>@Bean</ja>(typeName=<js>"C1"</js>)
	 * 	<jk>public class</jk> MyClass1 <jk>implements</jk> MyInterface {...}
	 *
	 * 	<ja>@Bean</ja>(typeName=<js>"C2"</js>)
	 * 	<jk>public class</jk> MyClass2 <jk>implements</jk> MyInterface {...}
	 *
	 * 	MyInterface[] x = <jk>new</jk> MyInterface[]{ <jk>new</jk> MyClass1(), <jk>new</jk> MyClass2() };
	 *
	 *	<jc>// Produces "[{mytype:'C1',...},{mytype:'C2',...}]"</jc>
	 * 	String json = JsonSerializer.<jsf>DEFAULT_LAX</jsf>.serialize(x);
	 * </p>
	 *
	 * <p>
	 * This is similar in concept to the {@link BeanContext#BEAN_beanTypePropertyName} setting except this annotation
	 * applies only to the annotated class and subclasses whereas the bean context property applies globally on
	 * serializers and parsers.
	 *
	 * <ul class='doctree'>
	 * 	<li class='warn'>
	 * 		Be careful what value you specify for this.  It should not interfere with bean property names or
	 * 		common HTML attribute names.
	 * </ul>
	 *
	 * The default value if not specified is <js>"_type"</js> unless overridden by the
	 * {@link BeanContext#BEAN_beanTypePropertyName} setting.
	 */
	String typePropertyName() default "";

	/**
	 * The set and order of names of properties associated with a bean class.
	 *
	 * <p>
	 * The order specified is the same order that the entries will be returned by the {@link BeanMap#entrySet()} and
	 * related methods.
	 *
	 * <p>
	 * This annotation is an alternative to using the {@link BeanFilter} class with an implemented
	 * {@link BeanFilter#getProperties()} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Address class with only street/city/state properties (in that order).</jc>
	 * 	<jc>// All other properties are ignored.</jc>
	 * 	<ja>@Bean</ja>(properties=<js>"street,city,state"</js>)
	 * 	<jk>public class</jk> Address {
	 * 		...
	 * </p>
	 */
	String properties() default "";

	/**
	 * Sort bean properties in alphabetical order.
	 *
	 * <p>
	 * When <jk>true</jk>, all bean properties will be serialized and access in alphabetical order.
	 * Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
	 * On IBM JVMs, the bean properties are ordered based on their ordering in the Java file.
	 * On Oracle JVMs, the bean properties are not ordered (which follows the official JVM specs).
	 *
	 * <p>
	 * This property is disabled by default so that IBM JVM users don't have to use {@link Bean @Bean} annotations
	 * to force bean properties to be in a particular order and can just alter the order of the fields/methods
	 * in the Java file.
	 *
	 * <p>
	 * This annotation is equivalent to using the {@link BeanContext#BEAN_sortProperties} property, but applied to
	 * individual classes instead of globally at the serializer or parser level.
	 */
	boolean sort() default false;

	/**
	 * Specifies a list of properties that should be excluded from {@link BeanMap#entrySet()}.
	 *
	 * <p>
	 * This annotation is an alternative to using the {@link BeanFilter} class with an implemented
	 * {@link BeanFilter#getExcludeProperties()} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Address class with only street/city/state properties (in that order).</jc>
	 * 	<jc>// All other properties are ignored.</jc>
	 * 	<ja>@Bean</ja>(excludeProperties=<js>"city,state"</js>})
	 * 	<jk>public class</jk> Address {
	 * 		...
	 * </p>
	 */
	String excludeProperties() default "";

	/**
	 * Associates a {@link PropertyNamer} with this bean to tailor the names of the bean properties.
	 *
	 * <p>
	 * Property namers are used to transform bean property names from standard form to some other form.
	 * For example, the {@link PropertyNamerDLC} will convert property names to dashed-lowercase, and these will be used
	 * as attribute names in JSON, and element names in XML.
	 *
	 * <p>
	 * This annotation is an alternative to using the {@link BeanFilter} class with an implemented
	 * {@link BeanFilter#getPropertyNamer()} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Define a class with dashed-lowercase property names.</jc>
	 * 	<ja>@Bean</ja>(propertyNamer=PropertyNamerDashedLC.<jk>class</jk>)
	 * 	<jk>public class</jk> MyClass {
	 * 		...
	 * 	}
	 * </p>
	 */
	Class<? extends PropertyNamer> propertyNamer() default PropertyNamerDefault.class;

	/**
	 * Identifies a class to be used as the interface class for this and all subclasses.
	 *
	 * <p>
	 * When specified, only the list of properties defined on the interface class will be used during serialization.
	 * Additional properties on subclasses will be ignored.
	 *
	 * <p class='bcode'>
	 * 	<jc>// Parent class</jc>
	 * 	<ja>@Bean</ja>(interfaceClass=A.<jk>class</jk>)
	 * 	<jk>public abstract class</jk> A {
	 * 		<jk>public</jk> String <jf>f0</jf> = <js>"f0"</js>;
	 * 	}
	 *
	 * 	<jc>// Sub class</jc>
	 * 	<jk>public class</jk> A1 <jk>extends</jk> A {
	 * 		<jk>public</jk> String <jf>f1</jf> = <js>"f1"</js>;
	 * 	}
	 *
	 * 	JsonSerializer s = JsonSerializer.<jsf>DEFAULT_LAX</jsf>;
	 * 	A1 a1 = <jk>new</jk> A1();
	 * 	String r = s.serialize(a1);
	 * 	<jsm>assertEquals</jsm>(<js>"{f0:'f0'}"</js>, r);  <jc>// Note f1 is not serialized.</jc>
	 * </p>
	 *
	 * <p>
	 * Note that this annotation can be used on the parent class so that it filters to all child classes,
	 * or can be set individually on the child classes.
	 *
	 * <p>
	 * This annotation is an alternative to using the {@link BeanFilter} class with an implemented
	 * {@link BeanFilter#getInterfaceClass()} method.
	 */
	Class<?> interfaceClass() default Object.class;

	/**
	 * Identifies a stop class for the annotated class.
	 *
	 * <p>
	 * Identical in purpose to the stop class specified by {@link Introspector#getBeanInfo(Class, Class)}.
	 * Any properties in the stop class or in its base classes will be ignored during analysis.
	 *
	 * <p>
	 * For example, in the following class hierarchy, instances of <code>C3</code> will include property <code>p3</code>,
	 * but not <code>p1</code> or <code>p2</code>.
	 * <p class='bcode'>
	 * 	<jk>public class</jk> C1 {
	 * 		<jk>public int</jk> getP1();
	 * 	}
	 *
	 * 	<jk>public class</jk> C2 <jk>extends</jk> C1 {
	 * 		<jk>public int</jk> getP2();
	 * 	}
	 *
	 * 	<ja>@Bean</ja>(stopClass=C2.<jk>class</jk>)
	 * 	<jk>public class</jk> C3 <jk>extends</jk> C2 {
	 * 		<jk>public int</jk> getP3();
	 * 	}
	 * </p>
	 */
	Class<?> stopClass() default Object.class;


	/**
	 * The list of classes that make up the bean dictionary for all properties of this bean or for subclasses of this
	 * bean.
	 *
	 * <p>
	 * This is a shorthand for setting the {@link BeanProperty#beanDictionary()} on all properties of the bean.
	 *
	 * <p>
	 * This list can consist of the following class types:
	 * <ul>
	 * 	<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean.name()};
	 * 	<li>Any subclass of {@link BeanDictionaryList} that defines an entire set of mappings.
	 * 		Note that the subclass MUST implement a no-arg constructor so that it can be instantiated.
	 * 	<li>Any subclass of {@link BeanDictionaryMap} that defines an entire set of mappings.
	 * 		Note that the subclass MUST implement a no-arg constructor so that it can be instantiated.
	 * </ul>
	 */
	Class<?>[] beanDictionary() default {};
}