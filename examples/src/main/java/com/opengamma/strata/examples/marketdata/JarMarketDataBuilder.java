/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.ResourceLocator;

/**
 * Loads market data from the standard directory structure embedded within a JAR file.
 */
public class JarMarketDataBuilder extends MarketDataBuilder {

  /**
   * The JAR file containing the expected structure of resources.
   */
  private final File jarFile;
  /**
   * The root path to the resources within the JAR file.
   */
  private final String rootPath;
  /**
   * A cache of JAR entries under the root path.
   */
  private final ImmutableSet<String> entries;

  /**
   * Constructs an instance.
   * 
   * @param jarFile  the JAR file containing the expected structure of resources
   * @param rootPath  the root path to the resources within the JAR file
   */
  public JarMarketDataBuilder(File jarFile, String rootPath) {
    String jarRoot = rootPath.startsWith(File.separator) ? rootPath.substring(1) : rootPath;
    if (!jarRoot.endsWith(File.separator)) {
      jarRoot += File.separator;
    }
    this.jarFile = jarFile;
    this.rootPath = jarRoot;
    this.entries = getEntries(jarFile, rootPath);
  }

  //-------------------------------------------------------------------------
  @Override
  protected Collection<ResourceLocator> getAllResources(String subdirectoryName) {
    String resolvedSubdirectory = subdirectoryName + File.separator;
    return entries.stream()
        .filter(e -> e.startsWith(resolvedSubdirectory))
        .map(e -> getEntryLocator(rootPath + e))
        .collect(Collectors.toSet());
  }

  @Override
  protected ResourceLocator getResource(String subdirectoryName, String resourceName) {
    String fullLocation = String.format("%s%s%s%s", rootPath, subdirectoryName, File.separator, resourceName);
    try (JarFile jar = new JarFile(jarFile)) {
      JarEntry entry = jar.getJarEntry(fullLocation);
      if (entry == null) {
        return null;
      }
      return getEntryLocator(entry.getName());
    } catch (Exception e) {
      throw new IllegalArgumentException(
          Messages.format("Error loading resource from JAR file: {}", jarFile), e);
    }
  }

  @Override
  protected boolean subdirectoryExists(String subdirectoryName) {
    String resolvedName = subdirectoryName.startsWith(File.separator) ? subdirectoryName.substring(1) : subdirectoryName;
    if (resolvedName.endsWith(File.separator)) {
      resolvedName = resolvedName.substring(0, resolvedName.length() - 1);
    }
    return entries.contains(resolvedName);
  }

  //-------------------------------------------------------------------------
  // Gets the resource locator corresponding to a given entry
  private ResourceLocator getEntryLocator(String entryName) {
    return ResourceLocator.of(ResourceLocator.CLASSPATH_URL_PREFIX + entryName);
  }

  private static ImmutableSet<String> getEntries(File jarFile, String rootPath) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    try (JarFile jar = new JarFile(jarFile)) {
      Enumeration<JarEntry> jarEntries = jar.entries();
      while (jarEntries.hasMoreElements()) {
        JarEntry entry = jarEntries.nextElement();
        String entryName = entry.getName();
        if (entryName.startsWith(rootPath) && !entryName.equals(rootPath)) {
          builder.add(entryName.substring(rootPath.length() + 1));
        }
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(
          Messages.format("Error scanning entries in JAR file: {}", jarFile), e);
    }
    return builder.build();
  }

}
