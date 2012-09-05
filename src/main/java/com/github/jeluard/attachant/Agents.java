/*
 * Copyright 2012 Julien Eluard.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jeluard.attachant;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Helper methods for loading Java agents via HotSpot Attach API.
 *
 * @see {@link http://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html}
 * @see {@link http://docs.oracle.com/javase/6/docs/technotes/guides/attach/index.html}
 * @see {@link https://blogs.oracle.com/CoreJavaTechTips/entry/the_attach_api}
 */
public class Agents {

  private static final String AGENT_CLASS_ATTRIBUTE = "Agent-Class";
  private static final String AGENT_MAIN_METHOD_NAME = "agentmain";

  /**
   * @return true if current platform support this library
   */
  public static boolean isPlatformSupported() {
    return System.getProperty("java.vm.name").contains("HotSpot");
  }

  private static void ensurePlatformSupported() {
    if (!Agents.isPlatformSupported()) {
      System.err.println("Attachant only supports HotSpot platform.");
    }
  }

  private static void ensureIsCorrectAgentJar(final String agentJarPath) throws IOException {
    final File file = new File(agentJarPath);
    if (!file.exists()) {
      throw new IllegalArgumentException("Cannot find agent at <"+agentJarPath+">");
    }
    final JarFile jarFile = new JarFile(agentJarPath);
    final Manifest manifest;
    try {
      manifest = jarFile.getManifest();
    } finally {
      jarFile.close();
    }
    final String agentClass = Preconditions.checkNotNull(manifest.getMainAttributes().getValue(Agents.AGENT_CLASS_ATTRIBUTE), "Cannot find value for "+Agents.AGENT_CLASS_ATTRIBUTE+" manifest attribute");
    try {
      final URL agentURL = new File(agentJarPath).toURI().toURL();
      final URLClassLoader classLoader = new URLClassLoader(new URL[]{agentURL});
      final Class<?> clazz = classLoader.loadClass(agentClass);
      try {
        clazz.getMethod(Agents.AGENT_MAIN_METHOD_NAME, String.class, Instrumentation.class);
      } catch (NoSuchMethodException e) {
        try {
          clazz.getMethod(Agents.AGENT_MAIN_METHOD_NAME, String.class);
        } catch (NoSuchMethodException ee) {
          throw new IllegalArgumentException("Cannot find "+Agents.AGENT_MAIN_METHOD_NAME+" method in <"+agentClass+">", ee);
        }
      }
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Cannot load class <"+agentClass+">", e);
    }
  }

  private static String definingJarPath(final Class<?> clazz) {
    return clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
  }

  /*
   * Helper method calling hotspot API via reflection.
   */

  private static Object attachVirtualMachine(final String id) {
    try {
      final Class<?> virtualMachineClass = Class.forName("com.sun.tools.attach.VirtualMachine");
      return virtualMachineClass.getMethod("attach", String.class).invoke(null, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void loadAgent(final Object virtualMachine, final String agentJarPath) {
    try {
      final Class<?> virtualMachineClass = virtualMachine.getClass();
      virtualMachineClass.getMethod("loadAgent", String.class).invoke(virtualMachine, agentJarPath);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Properties getSystemProperties(final Object virtualMachine) {
    try {
      final Class<?> virtualMachineClass = virtualMachine.getClass();
      return (Properties) virtualMachineClass.getMethod("getSystemProperties").invoke(virtualMachine);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void loadAgent(final Object virtualMachine, final String agentJarPath, final String options) {
    try {
      final Class<?> virtualMachineClass = virtualMachine.getClass();
      virtualMachineClass.getMethod("loadAgent", String.class, String.class).invoke(virtualMachine, agentJarPath, options);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void detachVirtualMachine(final Object virtualMachine) {
    try {
      final Class<?> virtualMachineClass = virtualMachine.getClass();
      virtualMachineClass.getMethod("detach").invoke(virtualMachine);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Load jar containing specified `clazz` as an agent.
   * <br />
   * {@code Agents.class} can be used to load jar defining this code. Convenient when creating uber jars.
   *
   * @param clazz
   * @param id
   * @param options
   * @throws IOException
   */
  public static void loadSelf(final Class<?> clazz, final String id, final Optional<String> options) throws IOException {
    Agents.ensurePlatformSupported();
    Preconditions.checkNotNull(clazz, "null clazz");

    Agents.load(Agents.definingJarPath(clazz), id, options);
  }

  /**
   * Load agent located at `agentJarPath` into local VM `id`.
   * <br />
   * Once loaded an agent cannot be unloaded.
   *
   * @param agentJar 
   * @param id
   * @param options
   * @throws IOException
   */
  public static void load(final String agentJarPath, final String id, final Optional<String> options) throws IOException {
    Agents.ensurePlatformSupported();
    Preconditions.checkNotNull(agentJarPath, "null agentJarPath");
    Preconditions.checkNotNull(id, "null id");
    Agents.ensureIsCorrectAgentJar(agentJarPath);

    final Object virtualMachine = Agents.attachVirtualMachine(id);
    try {
      if (options.isPresent()) {
        Agents.loadAgent(virtualMachine, agentJarPath, options.get());
      } else {
        Agents.loadAgent(virtualMachine, agentJarPath);
      }
    } finally {
      Agents.detachVirtualMachine(virtualMachine);
    }
  }

  /**
   * Load remote management agent from JAVA_HOME/lib.
   *
   * @param id
   * @param port
   * @param authenticate
   * @param ssl
   * @throws IOException
   * @see {@link http://docs.oracle.com/javase/7/docs/technotes/guides/management/agent.html}
   */
  public static void loadRemoteManagement(final String id, final int port, final boolean authenticate, final boolean ssl, final Optional<String> options) throws IOException {
    Agents.ensurePlatformSupported();
    Preconditions.checkNotNull(id, "null id");

    final Object virtualMachine = Agents.attachVirtualMachine(id);
    try {
      final String home = Agents.getSystemProperties(virtualMachine).getProperty("java.home");
      final String managementAgentJar = home + File.separator + "lib" + File.separator + "management-agent.jar";
      final StringBuilder builder = new StringBuilder("com.sun.management.jmxremote.port="+Integer.toString(port) +",com.sun.management.jmxremote.authenticate="+Boolean.toString(authenticate)+",com.sun.management.jmxremote.ssl="+Boolean.toString(ssl));
      if (options.isPresent()) {
        builder.append(",").append(options.get());
      }
      Agents.loadAgent(virtualMachine, managementAgentJar, builder.toString());
    } finally {
      Agents.detachVirtualMachine(virtualMachine);
    }
  }

}
