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
package org.apache.juneau.rest.test;

import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

public class PathsTest extends RestTestcase {

	private static String URL = "/testPaths";

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		ObjectMap r;
		String url;

		// [/test/testPaths]
		//	{
		//		pathInfo:null,
		//		pathInfoParts:[],
		//		pathRemainder:null,
		//		pathRemainderUndecoded:null,
		//		requestURI:'/jazz/juneau/test/testPaths',
		//		requestParentURI:'/jazz/juneau/test',
		//		requestURL:'https://localhost:9443/jazz/juneau/test/testPaths',
		//		servletPath:'/juneau/test/testPaths',
		//	}
		url = URL;
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertNull(r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertNull(r.getString("pathRemainderUndecoded"));
		assertNull(r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestParentURI").endsWith("/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));


		// [/test/testPaths/]
		//		{
		//			pathInfo: '/',
		//			pathRemainder: '',
		//			pathRemainderUndecoded: '',
		//			requestURI: '/jazz/juneau/test/testPaths/',
		//			requestParentURI: '/jazz/juneau/test',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/',
		//			servletPath: '/juneau/test/testPaths',
		//		}
		url = URL + '/';
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/", r.getString("pathInfo"));
		assertEquals("", r.getString("pathRemainder"));
		assertEquals("", r.getString("pathRemainderUndecoded"));
		assertEquals("", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/"));
		assertTrue(r.getString("requestParentURI").endsWith("/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths//]
		//		{
		//			pathInfo: '//',
		//			pathRemainder: '/',
		//			requestURI: '/jazz/juneau/test/testPaths//',
		//			requestParentURI: '/jazz/juneau/test',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths//',
		//			servletPath: '/juneau/test/testPaths',
		//		}
		url = URL + "//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("//", r.getString("pathInfo"));
		assertEquals("/", r.getString("pathRemainder"));
		assertEquals("/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths//"));
		assertTrue(r.getString("requestParentURI").endsWith("/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths///]
		//		{
		//			pathInfo: '///',
		//			pathRemainder: '//',
		//			requestURI: '/jazz/juneau/test/testPaths///',
		//			requestParentURI: '/jazz/juneau/test',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths///',
		//			servletPath: '/juneau/test/testPaths',
		//		}
		url = URL + "///";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("///", r.getString("pathInfo"));
		assertEquals("//", r.getString("pathRemainder"));
		assertEquals("//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths///"));
		assertTrue(r.getString("requestParentURI").endsWith("/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths///"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths/foo/bar]
		//		{
		//			pathInfo: '/foo/bar',
		//			pathRemainder: 'foo/bar',
		//			requestURI: '/jazz/juneau/test/testPaths/foo/bar',
		//			requestParentURI: '/jazz/juneau/test/testPaths/foo',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/foo/bar',
		//			servletPath: '/juneau/test/testPaths',
		//		}
		url = URL + "/foo/bar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals("foo/bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/foo/bar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/foo/bar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths/foo/bar/]
		//		{
		//			pathInfo: '/foo/bar/',
		//			pathRemainder: 'foo/bar/',
		//			requestURI: '/jazz/juneau/test/testPaths/foo/bar/',
		//			requestParentURI: '/jazz/juneau/test/testPaths/foo',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/foo/bar/',
		//			servletPath: '/juneau/test/testPaths',
		//		}
		url = URL + "/foo/bar/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/foo/bar/", r.getString("pathInfo"));
		assertEquals("foo/bar/", r.getString("pathRemainder"));
		assertEquals("foo/bar/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/foo/bar/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/foo/bar/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths//foo//bar//]
		//		{
		//			pathInfo: '//foo//bar//',
		//			pathRemainder: '/foo//bar//',
		//			requestURI: '/jazz/juneau/test/testPaths//foo//bar//',
		//			requestParentURI: '/jazz/juneau/test/testPaths//foo',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths//foo//bar//',
		//			servletPath: '/juneau/test/testPaths',
		//		}
		url = URL + "//foo//bar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("//foo//bar//", r.getString("pathInfo"));
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths//foo//bar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/foo/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths//foo//bar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths/test2]
		//		{
		//			pathInfo: '/test2',
		//			pathRemainder: null,
		//			requestURI: '/jazz/juneau/test/testPaths/test2',
		//			requestParentURI: '/jazz/juneau/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/test2',
		//			servletPath: '/juneau/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test',
		//			method: 2
		//		}
		url = URL + "/test2";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2", r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertNull(r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));


		// [/test/testPaths/test2/]
		//		{
		//			pathInfo: '/test2/',
		//			pathRemainder: '',
		//			requestURI: '/jazz/juneau/test/testPaths/test2/',
		//			requestParentURI: '/jazz/juneau/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/test2/',
		//			servletPath: '/juneau/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test',
		//			method: 2
		//		}
		url = URL + "/test2/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/", r.getString("pathInfo"));
		assertEquals("", r.getString("pathRemainder"));
		assertEquals("", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2//]
		//		{
		//			pathInfo: '/test2//',
		//			pathRemainder: '/',
		//			requestURI: '/jazz/juneau/test/testPaths/test2//',
		//			requestParentURI: '/jazz/juneau/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/test2//',
		//			servletPath: '/juneau/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test',
		//			method: 2
		//		}
		url = URL + "/test2//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2//", r.getString("pathInfo"));
		assertEquals("/", r.getString("pathRemainder"));
		assertEquals("/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2///]
		//		{
		//			pathInfo: '/test2///',
		//			pathRemainder: '//',
		//			requestURI: '/jazz/juneau/test/testPaths/test2///',
		//			requestParentURI: '/jazz/juneau/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/test2///',
		//			servletPath: '/juneau/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test',
		//			method: 2
		//		}
		url = URL + "/test2///";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2///", r.getString("pathInfo"));
		assertEquals("//", r.getString("pathRemainder"));
		assertEquals("//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2///"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2///"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2/foo/bar]
		//		{
		//			pathInfo: '/test2/foo/bar',
		//			pathRemainder: 'foo/bar',
		//			requestURI: '/jazz/juneau/test/testPaths/test2/foo/bar',
		//			requestParentURI: '/jazz/juneau/test/testPaths/test2/foo',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/test2/foo/bar',
		//			servletPath: '/juneau/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test',
		//			method: 2
		//		}
		url = URL + "/test2/foo/bar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals("foo/bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2/foo/bar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2/foo/bar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2/foo/bar/]
		//		{
		//			pathInfo: '/test2/foo/bar/',
		//			pathRemainder: 'foo/bar/',
		//			requestURI: '/jazz/juneau/test/testPaths/test2/foo/bar/',
		//			requestParentURI: '/jazz/juneau/test/testPaths/test2/foo',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/test2/foo/bar/',
		//			servletPath: '/juneau/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test',
		//			method: 2
		//		}
		url = URL + "/test2/foo/bar/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/foo/bar/", r.getString("pathInfo"));
		assertEquals("foo/bar/", r.getString("pathRemainder"));
		assertEquals("foo/bar/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2/foo/bar/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2/foo/bar/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2//foo//bar//]
		//		{
		//			pathInfo: '/test2//foo//bar//',
		//			pathRemainder: '/foo//bar//',
		//			requestURI: '/jazz/juneau/test/testPaths/test2//foo//bar//',
		//			requestParentURI: '/jazz/juneau/test/testPaths/test2//foo/',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/test2//foo//bar//',
		//			servletPath: '/juneau/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test',
		//			method: 2
		//		}
		url = URL + "/test2//foo//bar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2//foo//bar//", r.getString("pathInfo"));
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2//foo//bar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2//foo/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2//foo//bar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/a]
		//		{
		//			pathInfo: null,
		//			pathRemainder: null,
		//			requestURI: '/jazz/juneau/test/testPaths/a',
		//			requestParentURI: '/jazz/juneau/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 3
		//		}
		url = URL + "/a";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertNull(r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertNull(r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a/]
		//		{
		//			pathInfo: '/',
		//			pathRemainder: '',
		//			requestURI: '/jazz/juneau/test/testPaths/a/',
		//			requestParentURI: '/jazz/juneau/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a/',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 3
		//		}
		url = URL + "/a/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/", r.getString("pathInfo"));
		assertEquals("", r.getString("pathRemainder"));
		assertEquals("", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a//]
		//		{
		//			pathInfo: '//',
		//			pathRemainder: '/',
		//			requestURI: '/jazz/juneau/test/testPaths/a//',
		//			requestParentURI: '/jazz/juneau/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a//',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 3
		//		}
		url = URL + "/a//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("//", r.getString("pathInfo"));
		assertEquals("/", r.getString("pathRemainder"));
		assertEquals("/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a///]
		//		{
		//			pathInfo: '///',
		//			pathRemainder: '//',
		//			requestURI: '/jazz/juneau/test/testPaths/a///',
		//			requestParentURI: '/jazz/juneau/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a///',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 3
		//		}
		url = URL + "/a///";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("///", r.getString("pathInfo"));
		assertEquals("//", r.getString("pathRemainder"));
		assertEquals("//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a///"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a///"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a/foo/bar]
		//		{
		//			pathInfo: '/foo/bar',
		//			pathRemainder: 'foo/bar',
		//			requestURI: '/jazz/juneau/test/testPaths/a/foo/bar',
		//			requestParentURI: '/jazz/juneau/test/testPaths/a/foo',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a/foo/bar',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 3
		//		}
		url = URL + "/a/foo/bar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals("foo/bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/foo/bar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/foo/bar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a/foo/bar/]
		//		{
		//			pathInfo: '/foo/bar/',
		//			pathRemainder: 'foo/bar/',
		//			requestURI: '/jazz/juneau/test/testPaths/a/foo/bar/',
		//			requestParentURI: '/jazz/juneau/test/testPaths/a/foo',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a/foo/bar/',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 3
		//		}
		url = URL + "/a/foo/bar/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/foo/bar/", r.getString("pathInfo"));
		assertEquals("foo/bar/", r.getString("pathRemainder"));
		assertEquals("foo/bar/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/foo/bar/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/foo/bar/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a//foo//bar//]
		//		{
		//			pathInfo: '//foo//bar//',
		//			pathRemainder: '/foo//bar//',
		//			requestURI: '/jazz/juneau/test/testPaths/a//foo//bar//',
		//			requestParentURI: '/jazz/juneau/test/testPaths/a//foo/',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a//foo//bar//',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 3
		//		}
		url = URL + "/a//foo//bar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("//foo//bar//", r.getString("pathInfo"));
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a//foo//bar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/foo/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a//foo//bar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a/test2]
		//		{
		//			pathInfo: '/test2',
		//			pathRemainder: null,
		//			requestURI: '/jazz/juneau/test/testPaths/a/test2',
		//			requestParentURI: '/jazz/juneau/test/testPaths/a',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a/test2',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 4
		//		}
		url = URL + "/a/test2";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2", r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertNull(r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2/]
		//		{
		//			pathInfo: '/test2/',
		//			pathRemainder: '',
		//			requestURI: '/jazz/juneau/test/testPaths/a/test2/',
		//			requestParentURI: '/jazz/juneau/test/testPaths/a',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a/test2/',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 4
		//		}
		url = URL + "/a/test2/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/", r.getString("pathInfo"));
		assertEquals("", r.getString("pathRemainder"));
		assertEquals("", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2//]
		//		{
		//			pathInfo: '/test2//',
		//			pathRemainder: '/',
		//			requestURI: '/jazz/juneau/test/testPaths/a/test2//',
		//			requestParentURI: '/jazz/juneau/test/testPaths/a',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a/test2//',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 4
		//		}
		url = URL + "/a/test2//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2//", r.getString("pathInfo"));
		assertEquals("/", r.getString("pathRemainder"));
		assertEquals("/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2///]
		//		{
		//			pathInfo: '/test2///',
		//			pathRemainder: '//',
		//			requestURI: '/jazz/juneau/test/testPaths/a/test2///',
		//			requestParentURI: '/jazz/juneau/test/testPaths/a',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a/test2///',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 4
		//		}
		url = URL + "/a/test2///";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2///", r.getString("pathInfo"));
		assertEquals("//", r.getString("pathRemainder"));
		assertEquals("//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2///"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2///"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2/foo/bar]
		//		{
		//			pathInfo: '/test2/foo/bar',
		//			pathRemainder: 'foo/bar',
		//			requestURI: '/jazz/juneau/test/testPaths/a/test2/foo/bar',
		//			requestParentURI: '/jazz/juneau/test/testPaths/a/test2/foo',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a/test2/foo/bar',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 4
		//		}
		url = URL + "/a/test2/foo/bar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals("foo/bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2/foo/bar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2/foo/bar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2/foo/bar/]
		//		{
		//			pathInfo: '/test2/foo/bar/',
		//			pathRemainder: 'foo/bar/',
		//			requestURI: '/jazz/juneau/test/testPaths/a/test2/foo/bar/',
		//			requestParentURI: '/jazz/juneau/test/testPaths/a/test2/foo',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a/test2/foo/bar/',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 4
		//		}
		url = URL + "/a/test2/foo/bar/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/foo/bar/", r.getString("pathInfo"));
		assertEquals("foo/bar/", r.getString("pathRemainder"));
		assertEquals("foo/bar/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2/foo/bar/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2/foo/bar/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2//foo//bar//]
		//		{
		//			pathInfo: '/test2//foo//bar//',
		//			pathRemainder: '/foo//bar//',
		//			requestURI: '/jazz/juneau/test/testPaths/a/test2//foo//bar//',
		//			requestParentURI: '/jazz/juneau/test/testPaths/a/test2//foo/',
		//			requestURL: 'https://localhost:9443/jazz/juneau/test/testPaths/a/test2//foo//bar//',
		//			servletPath: '/juneau/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juneau/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juneau/test/testPaths',
		//			method: 4
		//		}
		url = URL + "/a/test2//foo//bar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2//foo//bar//", r.getString("pathInfo"));
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2//foo//bar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2//foo/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2//foo//bar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(4, (int)r.getInt("method"));

		//--------------------------------------------------------------------------------
		// Spaces
		//--------------------------------------------------------------------------------
		url = URL + "/%20";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/ ", r.getString("pathInfo"));
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("%20", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/%20"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/%20"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		url = URL + "/test2/%20";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/ ", r.getString("pathInfo"));
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("%20", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2/%20"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2/%20"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		url = URL + "/a/%20";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/ ", r.getString("pathInfo"));
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("%20", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/%20"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/%20"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(3, (int)r.getInt("method"));

		url = URL + "/a/test2/%20";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/ ", r.getString("pathInfo"));
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("%20", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2/%20"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2/%20"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(4, (int)r.getInt("method"));

		url = URL + "/+";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/ ", r.getString("pathInfo"));
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("+", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/+"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/+"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		url = URL + "/test2/+";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/ ", r.getString("pathInfo"));
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("+", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2/+"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2/+"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		url = URL + "/a/+";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/ ", r.getString("pathInfo"));
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("+", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/+"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/+"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(3, (int)r.getInt("method"));

		url = URL + "/a/test2/+";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/ ", r.getString("pathInfo"));
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("+", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2/+"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2/+"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertEquals(4, (int)r.getInt("method"));
	}
}
