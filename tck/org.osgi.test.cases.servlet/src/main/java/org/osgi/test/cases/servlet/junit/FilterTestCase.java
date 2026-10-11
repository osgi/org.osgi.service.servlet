/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/
package org.osgi.test.cases.servlet.junit;

import static java.util.Arrays.binarySearch;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.runtime.dto.DTOConstants;
import org.osgi.service.servlet.runtime.dto.FailedFilterDTO;
import org.osgi.service.servlet.runtime.dto.FilterDTO;
import org.osgi.service.servlet.runtime.dto.RequestInfoDTO;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.osgi.test.cases.servlet.junit.mock.MockFilter;
import org.osgi.test.cases.servlet.junit.mock.MockServlet;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FilterTestCase extends BaseHttpWhiteboardTestCase {

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED_validate() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED, "true");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		ServiceRegistration<Filter> sr = context.registerService(Filter.class, new MockFilter(), properties);
		serviceRegistrations.add(sr);

		FilterDTO filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.asyncSupported).isTrue();

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED, "false");
		sr.setProperties(properties);

		filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.asyncSupported).isFalse();

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED, 234l);
		sr.setProperties(properties);

		filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.asyncSupported).isFalse();

		properties.remove(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED);
		sr.setProperties(properties);

		filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.asyncSupported).isFalse();
	}

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_async() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED, "true");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, "ASYNC");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/b");
		ServiceRegistration<?> srA = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		serviceRegistrations.add(srA);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		ServiceRegistration<?> srB = context.registerService(Servlet.class, new MockServlet().content("a"), properties);
		serviceRegistrations.add(srB);

		final AtomicBoolean invoked = new AtomicBoolean(false);
		MockServlet mockServlet = new MockServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			final ExecutorService	executor	= Executors.newCachedThreadPool();

			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				doGetAsync(req.startAsync());
			}

			private void doGetAsync(final AsyncContext asyncContext) {
				executor.submit(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						try {
							invoked.set(true);

							asyncContext.dispatch("/b");
						} finally {
							asyncContext.complete();
						}

						return null;
					}
				});
			}

		};

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "true");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<?> srC = context.registerService(Servlet.class, mockServlet, properties);
		serviceRegistrations.add(srC);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).isEmpty();
		assertThat(requestInfoDTO.servletDTO.serviceId).isEqualTo(getServiceId(srC));

		assertThat(request("a")).isEqualTo("bab");
		assertThat(invoked.get()).isTrue();
	}

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_request() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		ServiceRegistration<?> srA = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		serviceRegistrations.add(srA);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<?> srB = context.registerService(Servlet.class, new MockServlet().content("a"), properties);
		serviceRegistrations.add(srB);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		FilterDTO filterDTO = requestInfoDTO.filterDTOs[0];
		assertThat(filterDTO.serviceId).isEqualTo(getServiceId(srA));
		assertThat(filterDTO.dispatcher).hasSize(1);
		assertThat(filterDTO.dispatcher[0]).isEqualTo("REQUEST");

		assertThat(request("a")).isEqualTo("bab");

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, "REQUEST");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		srA.setProperties(properties);

		assertThat(request("a")).isEqualTo("bab");
	}

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_include() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, "INCLUDE");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		ServiceRegistration<?> srA = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		serviceRegistrations.add(srA);

		FilterDTO filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.dispatcher).hasSize(1);
		assertThat(filterDTO.dispatcher[0]).isEqualTo("INCLUDE");

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<?> srB = context.registerService(Servlet.class, new MockServlet().content("a"), properties);
		serviceRegistrations.add(srB);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).isEmpty();
		assertThat(request("a")).isEqualTo("a");

		MockServlet mockServlet = new MockServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				RequestDispatcher requestDispatcher = request.getRequestDispatcher("/a");

				requestDispatcher.include(request, response);
			}

		};

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		ServiceRegistration<?> srC = context.registerService(Servlet.class, mockServlet, properties);
		serviceRegistrations.add(srC);

		assertThat(request("b")).isEqualTo("bab");
	}

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_forward() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, "FORWARD");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		ServiceRegistration<?> srA = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		serviceRegistrations.add(srA);

		FilterDTO filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.dispatcher).hasSize(1);
		assertThat(filterDTO.dispatcher[0]).isEqualTo("FORWARD");

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<?> srB = context.registerService(Servlet.class, new MockServlet().content("a"), properties);
		serviceRegistrations.add(srB);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).isEmpty();
		assertThat(request("a")).isEqualTo("a");

		MockServlet mockServlet = new MockServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				RequestDispatcher requestDispatcher = request.getRequestDispatcher("/a");

				requestDispatcher.forward(request, response);
			}

		};

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		ServiceRegistration<?> srC = context.registerService(Servlet.class, mockServlet, properties);
		serviceRegistrations.add(srC);

		assertThat(request("b")).isEqualTo("bab");
	}

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_error() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, "ERROR");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET, "a");
		ServiceRegistration<?> srA = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		serviceRegistrations.add(srA);

		FilterDTO filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.dispatcher).hasSize(1);
		assertThat(filterDTO.dispatcher[0]).isEqualTo("ERROR");

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "4xx");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<?> srB = context.registerService(Servlet.class, new MockServlet().content("a"), properties);
		serviceRegistrations.add(srB);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).isEmpty();
		assertThat(request("a")).isEqualTo("a");

		MockServlet mockServlet = new MockServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}

		};

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		ServiceRegistration<?> srC = context.registerService(Servlet.class, mockServlet, properties);
		serviceRegistrations.add(srC);

		assertThat(request("b")).isEqualTo("bab");
	}

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_multiple() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, new String[] {"REQUEST", "ERROR"});
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET, "a");
		ServiceRegistration<?> srA = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		serviceRegistrations.add(srA);

		FilterDTO filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.dispatcher).hasSize(2);
		Arrays.sort(filterDTO.dispatcher);
		assertThat(binarySearch(filterDTO.dispatcher, "ERROR")).isGreaterThanOrEqualTo(0);
		assertThat(binarySearch(filterDTO.dispatcher, "REQUEST")).isGreaterThanOrEqualTo(0);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "4xx");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<?> srB = context.registerService(Servlet.class, new MockServlet().content("a"), properties);
		serviceRegistrations.add(srB);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		assertThat(request("a")).isEqualTo("bab");

		MockServlet mockServlet = new MockServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}

		};

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		ServiceRegistration<?> srC = context.registerService(Servlet.class, mockServlet, properties);
		serviceRegistrations.add(srC);

		assertThat(request("b")).isEqualTo("bab");
	}

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_NAME() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new MockServlet().content("a"), properties));

		MockFilter mockFilter = new MockFilter() {

			@Override
			public void init(FilterConfig config) throws ServletException {
				super.init(config);
				this.config = config;
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				response.getWriter().write(config.getFilterName());
				chain.doFilter(request, response);
			}

			private FilterConfig config;

		};

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		ServiceRegistration<?> srA = context.registerService(Filter.class, mockFilter, properties);
		serviceRegistrations.add(srA);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		assertThat(requestInfoDTO.filterDTOs[0].name).isEqualTo(mockFilter.getClass().getName());
		assertThat(request("a")).isEqualTo(mockFilter.getClass().getName() + "a");

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "b");
		srA.setProperties(properties);

		requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		assertThat(requestInfoDTO.filterDTOs[0].name).isEqualTo("b");
		assertThat(request("a")).isEqualTo("ba");
	}

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_PATTERN() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] {"", "/"});
		serviceRegistrations.add(context.registerService(Servlet.class, new MockServlet().content("a"), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/**");
		ServiceRegistration<?> sr = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		serviceRegistrations.add(sr);

		FailedFilterDTO failedFilterDTO = getFailedFilterDTOByName("a");
		assertThat(failedFilterDTO).isNotNull();
		assertThat(failedFilterDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		sr.setProperties(properties);

		failedFilterDTO = getFailedFilterDTOByName("a");
		assertThat(failedFilterDTO).isNull();

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		assertThat(request("a")).isEqualTo("bab");
		assertThat(request("a.html")).isEqualTo("bab");
		assertThat(request("some/path/b.html")).isEqualTo("bab");
		assertThat(request("")).isEqualTo("bab");

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "");
		sr.setProperties(properties);

		requestInfoDTO = calculateRequestInfoDTO("/");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		assertThat(request("a")).isEqualTo("a");
		assertThat(request("a.html")).isEqualTo("a");
		assertThat(request("some/path/b.html")).isEqualTo("a");
		assertThat(request("")).isEqualTo("bab");

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "*.html");
		sr.setProperties(properties);

		requestInfoDTO = calculateRequestInfoDTO("/a.html");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		assertThat(request("a")).isEqualTo("a");
		assertThat(request("a.html")).isEqualTo("bab");
		assertThat(request("some/path/b.html")).isEqualTo("bab");
		assertThat(request("")).isEqualTo("a");
	}

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_REGEX() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] {"", "/"});
		serviceRegistrations.add(context.registerService(Servlet.class, new MockServlet().content("a"), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, "**");
		ServiceRegistration<?> sr = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		serviceRegistrations.add(sr);

		FailedFilterDTO failedFilterDTO = getFailedFilterDTOByName("a");
		assertThat(failedFilterDTO).isNotNull();
		assertThat(failedFilterDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, "/.+");
		sr.setProperties(properties);

		failedFilterDTO = getFailedFilterDTOByName("a");
		assertThat(failedFilterDTO).isNull();

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		assertThat(request("a")).isEqualTo("bab");
		assertThat(request("a.html")).isEqualTo("bab");
		assertThat(request("some/path/b.html")).isEqualTo("bab");
		assertThat(request("")).isEqualTo("a");

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, "/?");
		sr.setProperties(properties);

		requestInfoDTO = calculateRequestInfoDTO("/");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		assertThat(request("a")).isEqualTo("a");
		assertThat(request("a.html")).isEqualTo("a");
		assertThat(request("some/path/b.html")).isEqualTo("a");
		assertThat(request("")).isEqualTo("bab");

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, ".*\\.html");
		sr.setProperties(properties);

		requestInfoDTO = calculateRequestInfoDTO("/a.html");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		assertThat(request("a")).isEqualTo("a");
		assertThat(request("a.html")).isEqualTo("bab");
		assertThat(request("some/path/b.html")).isEqualTo("bab");
		assertThat(request("")).isEqualTo("a");
	}

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_SERVLET() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] {"", "/"});
		serviceRegistrations.add(context.registerService(Servlet.class, new MockServlet().content("a"), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] {"/b"});
		serviceRegistrations.add(context.registerService(Servlet.class, new MockServlet().content("b"), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET, "**");
		ServiceRegistration<?> sr = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		serviceRegistrations.add(sr);

		FailedFilterDTO failedFilterDTO = getFailedFilterDTOByName("a");
		assertThat(failedFilterDTO).isNull();

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET, "a");
		sr.setProperties(properties);

		failedFilterDTO = getFailedFilterDTOByName("a");
		assertThat(failedFilterDTO).isNull();

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		assertThat(request("a")).isEqualTo("bab");
		assertThat(request("a.html")).isEqualTo("bab");
		assertThat(request("some/path/b.html")).isEqualTo("bab");
		assertThat(request("")).isEqualTo("bab");
		assertThat(request("b")).isEqualTo("b");
	}

	@Test
	public void test_table_140_5_initParams() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new MockServlet().content("a"), properties));

		MockFilter mockFilter = new MockFilter() {

			@Override
			public void init(FilterConfig config) throws ServletException {
				this.config = config;
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				String initParameter = config.getInitParameter(request.getParameter("p"));

				response.getWriter().write((initParameter == null) ? "" : initParameter);
			}

			private FilterConfig	config;

		};

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + "param1", "value1");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + "param2", "value2");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + "param3", 345l);
		ServiceRegistration<?> sr = context.registerService(Filter.class, mockFilter, properties);
		serviceRegistrations.add(sr);

		FilterDTO filterDTO = getFilterDTOByName(DEFAULT, "a");

		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.initParams).containsKey("param1");
		assertThat(filterDTO.initParams).containsKey("param2");
		assertThat(filterDTO.initParams).doesNotContainKey("param3");
		assertThat(filterDTO.serviceId).isEqualTo(getServiceId(sr));
		assertThat(request("a?p=param1")).isEqualTo("value1");
		assertThat(request("a?p=param2")).isEqualTo("value2");
		assertThat(request("a?p=param3")).isEmpty();
	}

	@Test
	public void test_140_5_7to10() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		ServiceRegistration<?> sr = context.registerService(Filter.class, new MockFilter(), properties);
		serviceRegistrations.add(sr);

		FailedFilterDTO failedFilterDTO = getFailedFilterDTOByName("a");
		assertThat(failedFilterDTO).isNull();
		FilterDTO filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNull();

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		sr.setProperties(properties);

		failedFilterDTO = getFailedFilterDTOByName("a");
		assertThat(failedFilterDTO).isNull();

		filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.serviceId).isEqualTo(getServiceId(sr));

		properties.remove(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, ".*");
		sr.setProperties(properties);

		failedFilterDTO = getFailedFilterDTOByName("a");
		assertThat(failedFilterDTO).isNull();

		filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.serviceId).isEqualTo(getServiceId(sr));

		properties.remove(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET, "a");
		sr.setProperties(properties);

		failedFilterDTO = getFailedFilterDTOByName("a");
		assertThat(failedFilterDTO).isNull();

		filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNotNull();
		assertThat(filterDTO.serviceId).isEqualTo(getServiceId(sr));
	}

	@Test
	public void test_140_5_11to17() throws Exception {
		BundleContext context = this.context;
		final AtomicBoolean invokedDestroy = new AtomicBoolean(false);
		final AtomicBoolean invokedInit = new AtomicBoolean(false);

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new MockServlet().content("a"), properties));

		MockFilter mockFilter = new MockFilter() {

			@Override
			public void destroy() {
				invokedDestroy.set(true);
			}

			@Override
			public void init(FilterConfig config) throws ServletException {
				invokedInit.set(true);
			}

		};

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		ServiceRegistration<?> sr = context.registerService(Filter.class, mockFilter.around("b"), properties);
		serviceRegistrations.add(sr);

		assertThat(request("a")).isEqualTo("bab");
		assertThat(invokedInit.get()).isTrue();
		invokedInit.set(false);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET, "b");
		sr.setProperties(properties);
		assertThat(invokedDestroy.get()).isTrue();
		assertThat(request("a")).isEqualTo("bab");
		assertThat(invokedInit.get()).isTrue();
	}

	@Test
	public void test_140_5_17to20() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(some.property=some.value)");
		serviceRegistrations.add(context.registerService(Filter.class, new MockFilter(), properties));

		FilterDTO filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertThat(filterDTO).isNull();

		FailedFilterDTO failedFilterDTO = getFailedFilterDTOByName("a");
		assertThat(failedFilterDTO).isNull();
	}

	@Test
	public void test_140_5_21to25() throws Exception {
		BundleContext context = this.context;
		final AtomicBoolean invokedGetService = new AtomicBoolean(false);
		final AtomicBoolean invokedDestroy = new AtomicBoolean(false);
		final AtomicBoolean invokedInit = new AtomicBoolean(false);

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new MockServlet().content("a"), properties));

		class AFilter extends MockFilter {

			@Override
			public void destroy() {
				invokedDestroy.set(true);
			}

			@Override
			public void init(FilterConfig config) throws ServletException {
				invokedInit.set(true);
			}

		}

		PrototypeServiceFactory<Filter> factory = new PrototypeServiceFactory<Filter>() {

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Filter> registration, Filter service) {
			}

			@Override
			public Filter getService(Bundle bundle, ServiceRegistration<Filter> registration) {
				invokedGetService.set(true);
				return new AFilter().around("b");
			}

		};

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		ServiceRegistration<?> sr = context.registerService(Filter.class, factory, properties);
		serviceRegistrations.add(sr);

		assertThat(request("a")).isEqualTo("bab");
		assertThat(invokedGetService.get()).isTrue();
		assertThat(invokedInit.get()).isTrue();
		invokedGetService.set(false);
		invokedInit.set(false);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET, "b");
		sr.setProperties(properties);
		assertThat(invokedDestroy.get()).isTrue();
		assertThat(invokedGetService.get()).isTrue();
		assertThat(request("a")).isEqualTo("bab");
		assertThat(invokedInit.get()).isTrue();
	}

	@Test
	public void test_140_5_26to31() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new MockServlet().content("a"), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		properties.put(Constants.SERVICE_RANKING, 1000);
		ServiceRegistration<?> srB = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		serviceRegistrations.add(srB);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "c");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		properties.put(Constants.SERVICE_RANKING, 1000);
		ServiceRegistration<?> srC = context.registerService(Filter.class, new MockFilter().around("c"), properties);
		serviceRegistrations.add(srC);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(2);
		assertThat(request("a")).isEqualTo("bcacb");

		properties.put(Constants.SERVICE_RANKING, 2000);
		srC.setProperties(properties);
		assertThat(request("a")).isEqualTo("cbabc");
	}

	@Test
	public void test_140_5_32to33() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new MockServlet().content("a"), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(osgi.http.whiteboard.context.name=foo)");
		ServiceRegistration<?> srB = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		serviceRegistrations.add(srB);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "c");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		ServiceRegistration<?> srC = context.registerService(Filter.class, new MockFilter().around("c"), properties);
		serviceRegistrations.add(srC);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "d");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(something=foo)");
		ServiceRegistration<?> srD = context.registerService(Filter.class, new MockFilter().around("d"), properties);
		serviceRegistrations.add(srD);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.filterDTOs).hasSize(1);
		assertThat(request("a")).isEqualTo("cac");
	}
}
