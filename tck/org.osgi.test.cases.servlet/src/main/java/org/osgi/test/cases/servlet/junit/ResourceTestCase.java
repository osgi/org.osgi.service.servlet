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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.runtime.dto.DTOConstants;
import org.osgi.service.servlet.runtime.dto.FailedResourceDTO;
import org.osgi.service.servlet.runtime.dto.ResourceDTO;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.osgi.test.cases.servlet.junit.mock.MockSCHFactory;
import org.osgi.test.cases.servlet.junit.mock.MockServlet;

import jakarta.servlet.Servlet;

public class ResourceTestCase extends BaseHttpWhiteboardTestCase {

	@Test
	public void test_table_140_6_HTTP_WHITEBOARD_RESOURCE_validation() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, 34l);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/osgi/test/cases/servlet/junit/res");
		ServiceRegistration<Object> sr = context.registerService(Object.class, new Object(), properties);
		serviceRegistrations.add(sr);

		FailedResourceDTO failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertThat(failedResourceDTO).isNotNull();
		assertThat(failedResourceDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, 45);
		sr.setProperties(properties);

		failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertThat(failedResourceDTO).isNotNull();
		assertThat(failedResourceDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, new String[] {"/a", "/b"});
		sr.setProperties(properties);

		failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertThat(failedResourceDTO).isNotNull();
		assertThat(failedResourceDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, new String[] {"/a", "/b"});
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/osgi/test/cases/servlet/junit/res/index.txt");
		sr.setProperties(properties);

		failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertThat(failedResourceDTO).isNull();

		ResourceDTO resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertThat(resourceDTO).isNotNull();
		assertThat(resourceDTO.patterns).hasSize(2);
	}

	@Test
	public void test_table_140_6_HTTP_WHITEBOARD_RESOURCE_required() {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/*");
		ServiceRegistration<Object> sr = context.registerService(Object.class, new Object(), properties);
		serviceRegistrations.add(sr);

		FailedResourceDTO failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertThat(failedResourceDTO).isNull();
		ResourceDTO resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertThat(resourceDTO).isNull();

		properties.remove(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/");
		sr.setProperties(properties);

		failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertThat(failedResourceDTO).isNull();
		resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertThat(resourceDTO).isNull();
	}

	@Test
	public void test_140_6() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/osgi/test/cases/servlet/junit/res");
		ServiceRegistration<Object> sr = context.registerService(Object.class, new Object(), properties);
		serviceRegistrations.add(sr);

		ResourceDTO resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertThat(resourceDTO).isNotNull();
		assertThat(request("index.txt")).isEqualTo("a");

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/osgi/test/cases");
		sr.setProperties(properties);

		resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertThat(resourceDTO).isNotNull();
		assertThat(request("index.txt", null).get("responseCode").get(0)).isEqualTo("404");
		assertThat(request("servlet/junit/res/index.txt")).isEqualTo("a");

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/res/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/osgi/test/cases/servlet/junit/res");
		sr.setProperties(properties);

		resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertThat(resourceDTO).isNotNull();
		assertThat(request("servlet/junit/res/index.txt", null).get("responseCode").get(0)).isEqualTo("404");
		assertThat(request("res/index.txt")).isEqualTo("a");
	}

	@Test
	public void test_140_6_20to21() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/other.txt");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/osgi/test/cases/servlet/junit/res/index.txt");
		ServiceRegistration<Object> sr = context.registerService(Object.class, new Object(), properties);
		serviceRegistrations.add(sr);

		ResourceDTO resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertThat(resourceDTO).isNotNull();
		assertThat(request("other.txt")).isEqualTo("a");
		assertThat(request("index.txt", null).get("responseCode").get(0)).isEqualTo("404");
		assertThat(request("org/osgi/test/cases/servlet/junit/res/index.txt", null).get("responseCode").get(0)).isEqualTo("404");
	}

	@Test
	public void test_140_6_20to21_commonProperties() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/other.txt");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/osgi/test/cases/servlet/junit/res/index.txt");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(osgi.http.whiteboard.context.name=foo)");
		ServiceRegistration<Object> sr = context.registerService(Object.class, new Object(), properties);
		serviceRegistrations.add(sr);

		FailedResourceDTO failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertThat(failedResourceDTO).isNotNull();
		assertThat(failedResourceDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING);

		ResourceDTO resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertThat(resourceDTO).isNull();
		assertThat(request("other.txt", null).get("responseCode").get(0)).isEqualTo("404");

		properties.remove(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(some=foo)");
		sr.setProperties(properties);

		failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertThat(failedResourceDTO).isNull();

		resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertThat(resourceDTO).isNull();
		assertThat(request("other.txt", null).get("responseCode").get(0)).isEqualTo("404");
	}

	@Test
	public void test_140_6_28to29() throws Exception {
		BundleContext context = this.context;

		class AServletContextHelper extends ServletContextHelper {

			public AServletContextHelper(Bundle bundle) {
				super(bundle);
			}

			@Override
			public URL getResource(String name) {
				if (!name.startsWith("/org/osgi/test/cases/servlet/junit/res")) {
					name = "/org/osgi/test/cases/servlet/junit/res" + name;
				}
				name = name.replaceAll("//", "/");
				return super.getResource(name);
			}

		};

		MockSCHFactory factory = new MockSCHFactory() {

			@Override
			public ServletContextHelper getService(Bundle bundle, ServiceRegistration<ServletContextHelper> registration) {
				return new AServletContextHelper(bundle);
			}

		};

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "context1");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/context1");
		serviceRegistrations.add(context.registerService(ServletContextHelper.class, factory, properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(osgi.http.whiteboard.context.name=context1)");
		ServiceRegistration<Object> sr = context.registerService(Object.class, new Object(), properties);
		serviceRegistrations.add(sr);

		assertThat(request("context1/other.txt", null).get("responseCode").get(0)).isEqualTo("404");
		assertThat(request("context1/index.txt")).isEqualTo("a");
	}

	@Test
	public void test_140_6_1() throws Exception {
		BundleContext context = this.context;

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
		serviceRegistrations.add(context.registerService(Servlet.class, new MockServlet().content("b"), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/osgi/test/cases/servlet/junit/res");
		ServiceRegistration<Object> sr = context.registerService(Object.class, new Object(), properties);
		serviceRegistrations.add(sr);

		FailedResourceDTO failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertThat(failedResourceDTO).isNotNull();
		assertThat(failedResourceDTO.failureReason).isEqualTo(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);

		ResourceDTO resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertThat(resourceDTO).isNull();
		assertThat(request("index.txt")).isEqualTo("b");

		properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		sr.setProperties(properties);

		resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertThat(resourceDTO).isNotNull();
		assertThat(request("index.txt")).isEqualTo("a");

		failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertThat(failedResourceDTO).isNull();
	}
}
