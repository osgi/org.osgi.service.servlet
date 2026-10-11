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
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_ENABLED;
import static org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE;
import static org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.runtime.dto.DTOConstants;
import org.osgi.service.servlet.runtime.dto.ErrorPageDTO;
import org.osgi.service.servlet.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.servlet.runtime.dto.FailedServletDTO;
import org.osgi.service.servlet.runtime.dto.RequestInfoDTO;
import org.osgi.service.servlet.runtime.dto.ServletContextDTO;
import org.osgi.service.servlet.runtime.dto.ServletDTO;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

public class ServletTestCase extends BaseHttpWhiteboardTestCase {

	@Test
	public void test_140_4_4to5() throws Exception {
		BundleContext context = this.context;

		final AtomicBoolean invoked = new AtomicBoolean(false);

		Servlet servlet = new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				invoked.set(true);

				response.getWriter().write("a");
			}

		};

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] {"/a", "/b"});
		serviceRegistrations.add(context.registerService(Servlet.class, servlet, properties));

		assertThat(request("a")).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		invoked.set(false);
		assertThat(request("b")).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
	}

	@Test
	public void test_140_4_9() throws Exception {
		BundleContext context = this.context;

		final AtomicBoolean invoked = new AtomicBoolean(false);

		Servlet servlet = new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				invoked.set(true);

				response.getWriter().write("a");
			}

		};

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
		serviceRegistrations.add(context.registerService(Servlet.class, servlet, properties));

		assertThat(request("a")).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		invoked.set(false);
		assertThat(request("b.html")).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		invoked.set(false);
		assertThat(request("some/path/b.html")).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		assertThat(request("")).isEqualTo("a");
	}

	@Test
	public void test_140_4_10() throws Exception {
		BundleContext context = this.context;

		final AtomicBoolean invoked = new AtomicBoolean(false);

		Servlet servlet = new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				invoked.set(true);

				response.getWriter().write("a");
			}

		};

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "*.html");
		serviceRegistrations.add(context.registerService(Servlet.class, servlet, properties));

		assertThat(request("a.html")).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		invoked.set(false);
		assertThat(request("b.html")).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		invoked.set(false);
		assertThat(request("some/path/b.html")).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		assertThat(request("a.xhtml", null).get("responseCode").get(0)).isEqualTo("404");
		assertThat(request("some/path/a.xhtml", null).get("responseCode").get(0)).isEqualTo("404");
	}

	@Test
	public void test_140_4_11to13() throws Exception {
		BundleContext context = this.context;

		final AtomicBoolean invoked = new AtomicBoolean(false);

		Servlet servlet = new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				invoked.set(true);

				PrintWriter writer = response.getWriter();
				writer.write((request.getContextPath() == null) ? "" : request.getContextPath());
				writer.write(":");
				writer.write((request.getServletPath() == null) ? "" : request.getServletPath());
				writer.write(":");
				writer.write((request.getPathInfo() == null) ? "" : request.getPathInfo());
			}

		};

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "");
		serviceRegistrations.add(context.registerService(Servlet.class, servlet, properties));

		assertThat(request("")).isEqualTo("::/");
		assertThat(invoked.get()).isTrue();
		assertThat(request("a.xhtml", null).get("responseCode").get(0)).isEqualTo("404");
		assertThat(request("some/path/a.xhtml", null).get("responseCode").get(0)).isEqualTo("404");
	}

	@Test
	public void test_140_4_14to15() throws Exception {
		BundleContext context = this.context;

		final AtomicBoolean invoked = new AtomicBoolean(false);

		Servlet servlet = new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				invoked.set(true);

				PrintWriter writer = response.getWriter();
				writer.write((request.getContextPath() == null) ? "" : request.getContextPath());
				writer.write(":");
				writer.write((request.getServletPath() == null) ? "" : request.getServletPath());
				writer.write(":");
				writer.write((request.getPathInfo() == null) ? "" : request.getPathInfo());
			}

		};

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/");
		serviceRegistrations.add(context.registerService(Servlet.class, servlet, properties));

		assertThat(request("a.html")).isEqualTo(":/a.html:");
		assertThat(invoked.get()).isTrue();
		invoked.set(false);
		assertThat(request("a.xhtml")).isEqualTo(":/a.xhtml:");
		assertThat(invoked.get()).isTrue();
		invoked.set(false);
		assertThat(request("some/path/a.xhtml")).isEqualTo(":/some/path/a.xhtml:");
		assertThat(invoked.get()).isTrue();
	}

	@Test
	public void test_140_4_16() throws Exception {
		BundleContext context = this.context;

		final AtomicBoolean invoked = new AtomicBoolean(false);

		class TestServlet extends HttpServlet {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			private final String content;

			public TestServlet(String content) {
				this.content = content;
			}
			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				invoked.set(true);
				response.getWriter().write(content);
			}
		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new TestServlet("a"), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/fee/fi/foo/fum");
		serviceRegistrations.add(context.registerService(Servlet.class, new TestServlet("b"), properties));

		assertThat(request("a")).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		invoked.set(false);
		assertThat(request("fee/fi/foo/fum")).isEqualTo("b");
		assertThat(invoked.get()).isTrue();
	}

	@Test
	public void test_140_4_17to22() throws Exception {
		BundleContext context = this.context;

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public AServlet(String content) {
				this.content = content;
			}

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				response.getWriter().write(content);
			}

			private final String	content;

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> srA = context.registerService(Servlet.class, new AServlet("a"), properties);
		serviceRegistrations.add(srA);

		assertThat(request("a")).isEqualTo("a");

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> srB = context.registerService(Servlet.class, new AServlet("b"), properties);
		serviceRegistrations.add(srB);

		assertThat(request("a")).isEqualTo("a");

		FailedServletDTO failedServletDTO = getFailedServletDTOByName("b");

		assertThat(failedServletDTO).isNotNull();
		assertThat(failedServletDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
		assertThat(failedServletDTO.serviceId).isEqualTo(getServiceId(srB));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "c");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		properties.put(Constants.SERVICE_RANKING, 1000);
		serviceRegistrations.add(context.registerService(Servlet.class, new AServlet("c"), properties));

		assertThat(request("a")).isEqualTo("c");

		failedServletDTO = getFailedServletDTOByName("a");

		assertThat(failedServletDTO).isNotNull();
		assertThat(failedServletDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
		assertThat(failedServletDTO.serviceId).isEqualTo(getServiceId(srA));
	}

	@Test
	public void test_140_4_23to25() throws Exception {
		BundleContext context = this.context;

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> srA = context.registerService(Servlet.class, new AServlet(), properties);
		serviceRegistrations.add(srA);

		ServletDTO servletDTO = getServletDTOByName(
				DEFAULT,
				AServlet.class.getName());

		assertThat(servletDTO).isNotNull();
		assertThat(servletDTO.serviceId).isEqualTo(getServiceId(srA));
	}

	@Test
	public void test_140_4_26to31() throws Exception {
		BundleContext context = this.context;

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public AServlet(String content) {
				this.content = content;
			}

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				response.getWriter().write(content);
			}

			private final String	content;

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		ServiceRegistration<Servlet> srA = context.registerService(Servlet.class, new AServlet("a"), properties);
		serviceRegistrations.add(srA);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");

		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO.name).isEqualTo("a");
		assertThat(requestInfoDTO.servletDTO.serviceId).isEqualTo(getServiceId(srA));
		assertThat(request("a")).isEqualTo("a");
	}

	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED_validate() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "blah");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;}, properties));

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");

		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO.asyncSupported).isFalse();

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "true");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		serviceRegistrations.add(context.registerService(Servlet.class, new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;}, properties));

		requestInfoDTO = calculateRequestInfoDTO("/b");

		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO.asyncSupported).isTrue();

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "false");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/c");
		serviceRegistrations.add(context.registerService(Servlet.class, new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;}, properties));

		requestInfoDTO = calculateRequestInfoDTO("/c");

		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO.asyncSupported).isFalse();

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, 234l);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/d");
		serviceRegistrations.add(context.registerService(Servlet.class, new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;}, properties));

		requestInfoDTO = calculateRequestInfoDTO("/d");

		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO.asyncSupported).isFalse();

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/e");
		serviceRegistrations.add(context.registerService(Servlet.class, new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;}, properties));

		requestInfoDTO = calculateRequestInfoDTO("/e");

		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO.asyncSupported).isFalse();
	}

	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED() throws Exception {
		BundleContext context = this.context;
		final AtomicBoolean invoked = new AtomicBoolean(false);

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			final ExecutorService	executor	= Executors.newCachedThreadPool();

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				doGetAsync(req.startAsync());
			}

			private void doGetAsync(final AsyncContext asyncContext) {
				executor.submit(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						try {
							invoked.set(true);

							PrintWriter writer = asyncContext.getResponse().getWriter();

							writer.print("a");
						} finally {
							asyncContext.complete();
						}

						return null;
					}
				});
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "true");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> srA = context.registerService(Servlet.class, new AServlet(), properties);
		serviceRegistrations.add(srA);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");

		assertThat(requestInfoDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO).isNotNull();
		assertThat(requestInfoDTO.servletDTO.asyncSupported).isTrue();
		assertThat(getServiceId(srA)).isEqualTo(requestInfoDTO.servletDTO.serviceId);
		assertThat(requestInfoDTO.servletDTO.name).isEqualTo("a");
		assertThat(request("a")).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		invoked.set(false);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "false");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		ServiceRegistration<Servlet> srB = context.registerService(Servlet.class, new AServlet(), properties);
		serviceRegistrations.add(srB);

		assertThat(request("b", null).get("responseCode").get(0)).isEqualTo("500");
		assertThat(invoked.get()).isFalse();
	}

	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_validate() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "400");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> srA = context.registerService(Servlet.class, new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;}, properties);
		serviceRegistrations.add(srA);

		ErrorPageDTO errorPageDTO = getErrorPageDTOByName(DEFAULT, "a");
		assertThat(errorPageDTO).isNotNull();
		assertThat(errorPageDTO.errorCodes).hasSize(1);
		assertThat(errorPageDTO.errorCodes[0]).isEqualTo(400);
		assertThat(errorPageDTO.exceptions).isEmpty();

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, new String[] {"400", ServletException.class.getName()});
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> srB = context.registerService(Servlet.class, new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;}, properties);
		serviceRegistrations.add(srB);

		errorPageDTO = getErrorPageDTOByName(DEFAULT, "a");
		assertThat(errorPageDTO).isNotNull();
		assertThat(errorPageDTO.errorCodes).hasSize(1);
		assertThat(errorPageDTO.errorCodes[0]).isEqualTo(400);
		assertThat(errorPageDTO.exceptions).isEmpty();

		FailedServletDTO failedServletDTO = getFailedServletDTOByName("a");
		assertThat(failedServletDTO).isNotNull();
		assertThat(failedServletDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
		assertThat(failedServletDTO.serviceId).isEqualTo(getServiceId(srB));

		properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		srB.setProperties(properties);

		errorPageDTO = getErrorPageDTOByName(DEFAULT, "a");
		assertThat(errorPageDTO).isNotNull();
		assertThat(errorPageDTO.errorCodes[0]).isEqualTo(400);
		assertThat(errorPageDTO.exceptions[0]).isEqualTo(ServletException.class.getName());

		failedServletDTO = getFailedServletDTOByName("a");
		assertThat(failedServletDTO).isNotNull();
		assertThat(failedServletDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
		assertThat(failedServletDTO.serviceId).isEqualTo(getServiceId(srA));
	}

	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE() throws Exception {
		BundleContext context = this.context;

		final AtomicBoolean invoked = new AtomicBoolean(false);

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "a");
			}

		}

		class BServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				invoked.set(true);
				String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
				response.getWriter().write((message == null) ? "" : message);
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new AServlet(), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, new String[] {HttpServletResponse.SC_BAD_GATEWAY + "", HttpServletResponse.SC_FORBIDDEN + ""});
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/error");
		serviceRegistrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		ServletDTO servletDTO = getServletDTOByName(DEFAULT, "b");
		assertThat(servletDTO).isNotNull();

		ErrorPageDTO errorPageDTO = getErrorPageDTOByName(DEFAULT, "b");
		assertThat(errorPageDTO).isNotNull();
		assertThat(binarySearch(errorPageDTO.errorCodes, HttpServletResponse.SC_BAD_GATEWAY)).isGreaterThanOrEqualTo(0);

		Map<String, List<String>> response = request("a", null);
		assertThat(response.get("responseBody").get(0)).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		assertThat(response.get("responseCode").get(0)).isEqualTo(HttpServletResponse.SC_BAD_GATEWAY + "");
	}

	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_4xx() throws Exception {
		BundleContext context = this.context;

		final AtomicBoolean invoked = new AtomicBoolean(false);

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "a");
			}

		}

		class BServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				invoked.set(true);
				String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
				response.getWriter().write((message == null) ? "" : message);
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new AServlet(), properties));

		// Register the 4xx (b)
		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "4xx");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		serviceRegistrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		ServletDTO servletDTO = getServletDTOByName(DEFAULT, "b");
		assertThat(servletDTO).isNotNull();
		ErrorPageDTO errorPageDTO = getErrorPageDTOByName(DEFAULT, "b");
		assertThat(errorPageDTO).isNotNull();
		assertThat(binarySearch(errorPageDTO.errorCodes, HttpServletResponse.SC_FORBIDDEN)).isGreaterThanOrEqualTo(0);

		Map<String, List<String>> response = request("a", null);
		assertThat(response.get("responseBody").get(0)).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		assertThat(response.get("responseCode").get(0)).isEqualTo(HttpServletResponse.SC_FORBIDDEN + "");

		// register a 4xx which will be shadowed (c)
		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "c");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "4xx");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/c");
		serviceRegistrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		FailedErrorPageDTO failedErrorPageDTO = getFailedErrorPageDTOByName("c");
		assertThat(failedErrorPageDTO).isNotNull();
		assertThat(failedErrorPageDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);

		// register a specific 404 which shouldn't shadow 4xx (b)
		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "d");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "404");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/d");
		properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		serviceRegistrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		failedErrorPageDTO = getFailedErrorPageDTOByName("b");
		assertThat(failedErrorPageDTO).isNull();
		failedErrorPageDTO = getFailedErrorPageDTOByName("d");
		assertThat(failedErrorPageDTO).isNull();
		errorPageDTO = getErrorPageDTOByName(DEFAULT, "d");
		assertThat(errorPageDTO).isNotNull();
		assertThat(errorPageDTO.errorCodes[0]).isEqualTo(404);
	}

	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_5xx() throws Exception {
		BundleContext context = this.context;

		final AtomicBoolean invoked = new AtomicBoolean(false);

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "a");
			}

		}

		class BServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				invoked.set(true);
				String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
				response.getWriter().write((message == null) ? "" : message);
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new AServlet(), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "5xx");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/error");
		serviceRegistrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		ServletDTO servletDTO = getServletDTOByName(DEFAULT, "b");
		assertThat(servletDTO).isNotNull();
		ErrorPageDTO errorPageDTO = getErrorPageDTOByName(DEFAULT, "b");
		assertThat(errorPageDTO).isNotNull();
		assertThat(binarySearch(errorPageDTO.errorCodes, HttpServletResponse.SC_BAD_GATEWAY)).isGreaterThanOrEqualTo(0);

		Map<String, List<String>> response = request("a", null);
		assertThat(response.get("responseBody").get(0)).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		assertThat(response.get("responseCode").get(0)).isEqualTo(HttpServletResponse.SC_BAD_GATEWAY + "");
	}

	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_exception() throws Exception {
		BundleContext context = this.context;

		final AtomicBoolean invoked = new AtomicBoolean(false);

		class AException extends ServletException {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
		}

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				throw new AException();
			}

		}

		class BServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@SuppressWarnings("deprecation")
			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				invoked.set(true);
				Object exceptionType = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
				if (exceptionType == null) {
					response.getWriter().write("");
				} else if (exceptionType instanceof Class) {
					response.getWriter()
							.write(((Class< ? >) exceptionType).getName());
				} else {
					fail(RequestDispatcher.ERROR_EXCEPTION_TYPE
							+ " must be of type Class but was "
							+ exceptionType.getClass().getName());
				}
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new AServlet(), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, ServletException.class.getName());
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/error");
		serviceRegistrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		ServletDTO servletDTO = getServletDTOByName(DEFAULT, "b");
		assertThat(servletDTO).isNotNull();
		ErrorPageDTO errorPageDTO = getErrorPageDTOByName(DEFAULT, "b");
		assertThat(errorPageDTO).isNotNull();
		assertThat(binarySearch(errorPageDTO.exceptions, ServletException.class.getName())).isGreaterThanOrEqualTo(0);

		Map<String, List<String>> response = request("a", null);
		assertThat(response.get("responseBody").get(0)).isEqualTo(AException.class.getName());
		assertThat(invoked.get()).isTrue();
		assertThat(response.get("responseCode").get(0)).isEqualTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "");
	}

	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_NAME() throws Exception {
		BundleContext context = this.context;

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.getWriter().write(getServletName());
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> srA = context.registerService(Servlet.class, new AServlet(), properties);
		serviceRegistrations.add(srA);

		ServletDTO servletDTO = getServletDTOByName(
				DEFAULT,
				AServlet.class.getName());

		assertThat(servletDTO).isNotNull();
		assertThat(servletDTO.serviceId).isEqualTo(getServiceId(srA));
		assertThat(request("a")).isEqualTo(AServlet.class.getName());
	}

	@Test
	public void test_table_140_4_initParams() throws Exception {
		BundleContext context = this.context;

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				String initParameter = getServletConfig().getInitParameter(request.getParameter("p"));

				response.getWriter().write((initParameter == null) ? "" : initParameter);
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + "param1", "value1");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + "param2", "value2");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + "param3", 345l);
		ServiceRegistration<Servlet> srA = context.registerService(Servlet.class, new AServlet(), properties);
		serviceRegistrations.add(srA);

		ServletDTO servletDTO = getServletDTOByName(
				DEFAULT,
				AServlet.class.getName());

		assertThat(servletDTO).isNotNull();
		assertThat(servletDTO.initParams).containsKey("param1");
		assertThat(servletDTO.initParams).containsKey("param2");
		assertThat(servletDTO.initParams).doesNotContainKey("param3");
		assertThat(servletDTO.serviceId).isEqualTo(getServiceId(srA));
		assertThat(request("a?p=param1")).isEqualTo("value1");
		assertThat(request("a?p=param2")).isEqualTo("value2");
		assertThat(request("a?p=param3")).isEmpty();
	}

	@Test
	public void test_140_4_38to42() throws Exception {
		BundleContext context = this.context;
		final AtomicBoolean invoked = new AtomicBoolean(false);

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void destroy() {
				invoked.set(true);

				super.destroy();
			}

			@Override
			public void init(ServletConfig config) throws ServletException {
				invoked.set(true);

				super.init(config);
			}

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.getWriter().write("a");
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> srA = context.registerService(Servlet.class, new AServlet(), properties);

		assertThat(request("a")).isEqualTo("a");
		assertThat(invoked.get()).isTrue();
		invoked.set(false);
		srA.unregister();
		assertThat(invoked.get()).isTrue();
	}

	@Test
	public void test_140_4_42to44() throws Exception {
		BundleContext context = this.context;
		final AtomicBoolean invoked = new AtomicBoolean(false);

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void init(ServletConfig config) throws ServletException {
				invoked.set(true);

				throw new ServletException();
			}

		}

		class BServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.getWriter().write("failed");
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new AServlet(), properties));

		FailedServletDTO failedServletDTO = getFailedServletDTOByName("a");
		assertThat(failedServletDTO).isNotNull();
		assertThat(failedServletDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT);
		assertThat(invoked.get()).isTrue();

		Map<String, List<String>> response = request("a", null);
		// init failed, no servlet
		assertThat(response.get("responseCode").get(0)).isEqualTo("404");

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		response = request("a", null);
		// BServlet handles the request
		assertThat(response.get("responseCode").get(0)).isEqualTo("200");
	}

	@Test
	public void test_140_4_45to46() throws Exception {
		BundleContext context = this.context;
		final AtomicBoolean invokedInit = new AtomicBoolean(false);
		final AtomicBoolean invokedDestroy = new AtomicBoolean(false);

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void destroy() {
				invokedDestroy.set(true);

				super.destroy();
			}

			@Override
			public void init(ServletConfig config) throws ServletException {
				invokedInit.set(true);

				super.init(config);
			}

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.getWriter().write("a");
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> sr = context.registerService(Servlet.class, new AServlet(), properties);
		serviceRegistrations.add(sr);

		assertThat(request("a")).isEqualTo("a");
		assertThat(invokedInit.get()).isTrue();
		assertThat(invokedDestroy.get()).isFalse();
		invokedInit.set(false);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		sr.setProperties(properties);
		assertThat(invokedDestroy.get()).isTrue();
		assertThat(request("b")).isEqualTo("a");
		assertThat(invokedInit.get()).isTrue();
	}

	@Test
	public void test_140_4_46to47() throws Exception {
		BundleContext context = this.context;
		final AtomicBoolean invokedGetService = new AtomicBoolean(false);
		final AtomicBoolean invokedInit = new AtomicBoolean(false);
		final AtomicBoolean invokedDestroy = new AtomicBoolean(false);

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void destroy() {
				invokedDestroy.set(true);

				super.destroy();
			}

			@Override
			public void init(ServletConfig config) throws ServletException {
				invokedInit.set(true);

				super.init(config);
			}

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.getWriter().write("a");
			}

		}

		PrototypeServiceFactory<Servlet> factory = new PrototypeServiceFactory<Servlet>() {

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Servlet> registration, Servlet service) {
			}

			@Override
			public Servlet getService(Bundle bundle, ServiceRegistration<Servlet> registration) {
				invokedGetService.set(true);
				return new AServlet();
			}
		};

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> sr = context.registerService(Servlet.class, factory, properties);
		serviceRegistrations.add(sr);

		assertThat(request("a")).isEqualTo("a");
		assertThat(invokedGetService.get()).isTrue();
		assertThat(invokedInit.get()).isTrue();
		assertThat(invokedDestroy.get()).isFalse();
		invokedInit.set(false);
		invokedGetService.set(false);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		sr.setProperties(properties);
		assertThat(invokedDestroy.get()).isTrue();
		assertThat(invokedGetService.get()).isTrue();
		assertThat(request("b")).isEqualTo("a");
		assertThat(invokedInit.get()).isTrue();
	}

	@Test
	public void test_140_4_48to51() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(some.property=some.value)");
		serviceRegistrations.add(context.registerService(Servlet.class, new HttpServlet() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;}, properties));

		ServletDTO servletDTO = getServletDTOByName(DEFAULT, "a");
		assertThat(servletDTO).isNull();

		FailedServletDTO failedServletDTO = getFailedServletDTOByName("a");
		assertThat(failedServletDTO).isNull();
	}

	@Test
	public void test_140_4_1_22to23() throws Exception {
		BundleContext context = this.context;

		final AtomicBoolean invoked = new AtomicBoolean(false);

		class AServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "a");
			}

		}

		class BServlet extends HttpServlet {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				invoked.set(true);
				throw new ServletException();
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		serviceRegistrations.add(context.registerService(Servlet.class, new AServlet(), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, HttpServletResponse.SC_BAD_GATEWAY + "");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		serviceRegistrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		Map<String, List<String>> response = request("a", null);
		assertThat("a").isNotEqualTo(response.get("responseBody").get(0));
		assertThat(invoked.get()).isTrue();
		assertThat(response.get("responseCode").get(0)).isEqualTo(HttpServletResponse.SC_BAD_GATEWAY + "");
	}

	/**
	 * Tests for 1.1 Update
	 */

	/**
	 * Servlet registrations
	 */
	@Test
	public void testServletRegistration() throws Exception {
		// new in 1.1: servlet needs only one of those: pattern, name, error
		// page
		// previously pattern was always required

		BundleContext context = this.context;

		long before = this.getHttpRuntimeChangeCount();

		Dictionary<String,Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME,
				"myname");
		serviceRegistrations.add(context.registerService(Servlet.class,
				new HttpServlet() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;}, properties));
		before = this.waitForRegistration(before);

		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
				"/mypattern");
		serviceRegistrations.add(context.registerService(Servlet.class,
				new HttpServlet() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;}, properties));
		before = this.waitForRegistration(before);

		properties = new Hashtable<>();
		properties.put(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE,
				"404");
		serviceRegistrations.add(context.registerService(Servlet.class,
				new HttpServlet() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;}, properties));
		before = this.waitForRegistration(before);

		// two servlets and one error page should be registered now
		final ServletContextDTO dto = this.getServletContextDTOByName(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
		assertThat(dto.errorPageDTOs).hasSize(1);
		assertThat(dto.errorPageDTOs[0].errorCodes[0]).isEqualTo(404L);
		assertThat(dto.servletDTOs).hasSize(2);
		int indexName = dto.servletDTOs[0].patterns.length == 0 ? 0 : 1;
		int indexPattern = indexName == 0 ? 1 : 0;
		assertThat(dto.servletDTOs[indexName].name).isEqualTo("myname");
		assertThat(dto.servletDTOs[indexName].patterns).isEmpty();

		assertThat(dto.servletDTOs[indexPattern].patterns).hasSize(1);
		assertThat(dto.servletDTOs[indexPattern].patterns[0]).isEqualTo("/mypattern");
	}

	private void setupUploadServlet(final CountDownLatch receivedLatch,
			final Map<String,Long> contents)
			throws Exception {
		final Dictionary<String,Object> servletProps = new Hashtable<String,Object>();
		servletProps.put(HTTP_WHITEBOARD_SERVLET_PATTERN, "/post");
		servletProps.put(HTTP_WHITEBOARD_SERVLET_MULTIPART_ENABLED,
				Boolean.TRUE);
		servletProps.put(HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE, 1024L);

		final Servlet uploadServlet = new HttpServlet() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void doPost(HttpServletRequest req,
					HttpServletResponse resp)
					throws IOException, ServletException {
				try {
					final Collection<Part> parts = req.getParts();
					for (final Part p : parts) {
						contents.put(p.getName(), p.getSize());
					}
					resp.setStatus(201);
				} finally {
					receivedLatch.countDown();
				}

			}
		};

		long before = this.getHttpRuntimeChangeCount();
		serviceRegistrations.add(context.registerService(
				Servlet.class.getName(), uploadServlet, servletProps));
		this.waitForRegistration(before);
	}

	private void postContent(final char c, final long length,
			final int expectedRT) throws IOException {
		final URL url = getServerURL("/post");
		final CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			final HttpPost httppost = new HttpPost(url.toExternalForm());

			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < length; i++) {
				sb.append(c);
			}
			final StringBody text = new StringBody(sb.toString(),
					ContentType.TEXT_PLAIN);

			final HttpEntity reqEntity = MultipartEntityBuilder.create()
					.addPart("text", text)
					.build();


			httppost.setEntity(reqEntity);

			final CloseableHttpResponse response = httpclient.execute(httppost);
			try {
				final HttpEntity resEntity = response.getEntity();
				EntityUtils.consume(resEntity);
				assertThat(response.getStatusLine().getStatusCode()).isEqualTo(expectedRT);
			} finally {
				response.close();
			}
		} finally {
			httpclient.close();
		}
	}

	@Test
	public void testUpload() throws Exception {
		final CountDownLatch receivedLatch = new CountDownLatch(1);
		final Map<String,Long> contents = new HashMap<>();
		setupUploadServlet(receivedLatch, contents);

		postContent('a', 500, 201);
		assertThat(receivedLatch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(contents).hasSize(1);
		assertThat((long) contents.get("text")).isEqualTo(500L);
	}

	@Test
	public void testMaxFileSize() throws Exception {
		final CountDownLatch receivedLatch = new CountDownLatch(1);
		final Map<String,Long> contents = new HashMap<>();
		setupUploadServlet(receivedLatch, contents);

		postContent('b', 2048, 500);
		assertThat(receivedLatch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(contents).isEmpty();
	}
}
