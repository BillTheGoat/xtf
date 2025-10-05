package org.cdlib.xtf.test;


/**
 * Copyright (c) 2004, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the University of California nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import org.cdlib.xtf.util.Path;

/**
 * Used to abuse dynaXML and crossQuery, providing only as much context as
 * they need within the test environment.
 *
 * @author Martin Haye
 */
public class FakeServletContext implements ServletContext 
{
  public Object getAttribute(String name) {
    assert false;
    return null;
  }

  public Enumeration getAttributeNames() {
    assert false;
    return null;
  }

  public ServletContext getContext(String uripath) {
    assert false;
    return null;
  }

  public String getInitParameter(String name) {
    assert false;
    return null;
  }

  public Enumeration getInitParameterNames() {
    assert false;
    return null;
  }

  public int getMajorVersion() {
    assert false;
    return 0;
  }

  public String getMimeType(String file) {
    assert false;
    return null;
  }

  public int getMinorVersion() {
    assert false;
    return 0;
  }

  public RequestDispatcher getNamedDispatcher(String name) {
    assert false;
    return null;
  }

  public String getRealPath(String path) {
    String homeDir = System.getProperty("xtf.home");
    return Path.resolveRelOrAbs(homeDir, path);
  }

  public RequestDispatcher getRequestDispatcher(String path) {
    assert false;
    return null;
  }

  public URL getResource(String path)
    throws MalformedURLException 
  {
    assert false;
    return null;
  }

  public InputStream getResourceAsStream(String path) {
    assert false;
    return null;
  }

  public Set getResourcePaths(String path) {
    assert false;
    return null;
  }

  public String getServerInfo() {
    assert false;
    return null;
  }

  public Servlet getServlet(String name) {
    assert false;
    return null;
  }

  public String getServletContextName() {
    assert false;
    return null;
  }

  public Enumeration getServletNames() {
    assert false;
    return null;
  }

  public Enumeration getServlets() {
    assert false;
    return null;
  }

  public void log(Exception exception, String msg) {
    assert false;
  }

  public void log(String message, Throwable throwable) {
    assert false;
  }

  public void log(String msg) {
    return;
  }

  public void removeAttribute(String name) {
    assert false;
  }

  public void setAttribute(String name, Object object) {
    assert false;
  }

  public String getContextPath() {
    return null;
  }
@Override
public String getRequestCharacterEncoding() {
    return null;
}

@Override
public void setRequestCharacterEncoding(String encoding) {
    // Not implemented for this test class
}

@Override
public String getResponseCharacterEncoding() {
    return null;
}

@Override
public void setResponseCharacterEncoding(String encoding) {
    // Not implemented for this test class
}

@Override
public int getSessionTimeout() {
    return 0;
}

@Override
public void setSessionTimeout(int sessionTimeout) {
    // Not implemented for this test class
}
@Override
public String getVirtualServerName() {
    return null;
}

@Override
public jakarta.servlet.SessionCookieConfig getSessionCookieConfig() {
    return null;
}

@Override
public void addListener(String className) {
    // Not implemented for this test class
}

@Override
public <T extends java.util.EventListener> void addListener(T t) {
    // Not implemented for this test class
}

@Override
public void addListener(Class<? extends java.util.EventListener> listenerClass) {
    // Not implemented for this test class
}

@Override
public <T extends java.util.EventListener> T createListener(Class<T> clazz) 
        throws jakarta.servlet.ServletException {
    return null;
}

@Override
public jakarta.servlet.descriptor.JspConfigDescriptor getJspConfigDescriptor() {
    return null;
}

@Override
public ClassLoader getClassLoader() {
    return null;
}

@Override
public void declareRoles(String... roleNames) {
    // Not implemented for this test class
}

@Override
public java.util.Set<jakarta.servlet.SessionTrackingMode> getDefaultSessionTrackingModes() {
    return new java.util.HashSet<>();
}

@Override
public java.util.Set<jakarta.servlet.SessionTrackingMode> getEffectiveSessionTrackingModes() {
    return new java.util.HashSet<>();
}

@Override
public void setSessionTrackingModes(java.util.Set<jakarta.servlet.SessionTrackingMode> sessionTrackingModes) {
    // Not implemented for this test class
}
@Override
public java.util.Map<String, ? extends jakarta.servlet.ServletRegistration> getServletRegistrations() {
    return new java.util.HashMap<>();
}

@Override
public jakarta.servlet.ServletRegistration getServletRegistration(String servletName) {
    return null;
}

@Override
public java.util.Map<String, ? extends jakarta.servlet.FilterRegistration> getFilterRegistrations() {
    return new java.util.HashMap<>();
}

@Override
public jakarta.servlet.FilterRegistration getFilterRegistration(String filterName) {
    return null;
}

@Override
public jakarta.servlet.ServletRegistration.Dynamic addServlet(String servletName, String className) {
    return null;
}

@Override
public jakarta.servlet.ServletRegistration.Dynamic addServlet(String servletName, jakarta.servlet.Servlet servlet) {
    return null;
}

@Override
public jakarta.servlet.ServletRegistration.Dynamic addServlet(String servletName, Class<? extends jakarta.servlet.Servlet> servletClass) {
    return null;
}

@Override
public jakarta.servlet.ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
    return null;
}

@Override
public <T extends jakarta.servlet.Servlet> T createServlet(Class<T> clazz) throws jakarta.servlet.ServletException {
    return null;
}

@Override
public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
    return null;
}

@Override
public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, jakarta.servlet.Filter filter) {
    return null;
}

@Override
public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends jakarta.servlet.Filter> filterClass) {
    return null;
}

@Override
public <T extends jakarta.servlet.Filter> T createFilter(Class<T> clazz) throws jakarta.servlet.ServletException {
    return null;
}
@Override
public boolean setInitParameter(String name, String value) {
    return false;
}
@Override
public int getEffectiveMajorVersion() {
    return 5;
}

@Override
public int getEffectiveMinorVersion() {
    return 0;
}

} // class FakeServletContext
