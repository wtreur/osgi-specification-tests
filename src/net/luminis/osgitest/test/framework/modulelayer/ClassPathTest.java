/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.luminis.osgitest.test.framework.modulelayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import net.luminis.osgitest.testhelper.BundleSpecifier;
import net.luminis.osgitest.testhelper.OSGiSpec;
import net.luminis.osgitest.testhelper.OSGiVersionSpecs;
import net.luminis.osgitest.testhelper.TestBase;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Tests the Bundle's getResource and getResources functionality.
 * TODO:
 * - create tests for checking bundle fragments
 * - create tests using one or more target specifiers on the classpath.
 */
public class ClassPathTest extends TestBase {

    /**
     * Tests the correct behavior of getEntry: this should not respect the classpath at all
     * (see section 4.3.15 of the spec).
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections = {"4.3.15"}),
        @OSGiSpec(version="4.2", sections = {"4.4.14"})
    })
    public void testGetEntry() throws Exception {
        BundleSpecifier inner = m_bu.createBundleSpecifier("inner").pack("innerStream", new ByteArrayInputStream("theInner".getBytes()));
        BundleSpecifier outer = m_bu.createBundleSpecifier("outer").pack(inner, true);
        Bundle b = m_bu.installBundle(outer);
        assert b.getEntry("innerStream") == null : "We should not be able to find a stream in an embedded bundle when using getEntry(...).";
    }

    /**
     * Tests the correct behavior of getEntryPaths: this should include directory entries,
     * but only if the directory entry has been _explicitly_ included, that is, it is not part
     * of the path to some other resource (otherwise it wouldn't be the longest path,
     * see section 6.1.4.14 of the spec).
     */
    @SuppressWarnings("unchecked")
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections = {"6.1.4", "6.1.4.14"}),
        @OSGiSpec(version="4.2", sections = {"6.1.4", "6.1.4.16"})
    })
    public void testGetEntryPaths() throws Exception {
        BundleSpecifier outer = m_bu.createBundleSpecifier("outer")
        .pack("streamOne", new ByteArrayInputStream("theInner".getBytes()))
        .pack("folderOne/streamOne", new ByteArrayInputStream("theInner".getBytes()));

        Bundle b = m_bu.installBundle(outer);

        boolean foundEntry = false;
        boolean foundFolder = false;
        int nrEntries = 0;
        Enumeration e = b.getEntryPaths("/");
        StringBuilder foundEntries = new StringBuilder();
        while (e.hasMoreElements()) {
            nrEntries++;
            String entry = (String) e.nextElement();
            foundEntries.append(entry + "\n");
            if (entry.equals("streamOne")) {
                foundEntry = true;
            }
            if (entry.equals("folderOne/")) {
                foundFolder = true;
            }
        }

        StringBuilder whatHappened = new StringBuilder().append("getEntryPaths did not return what we expected:\n");
        if (nrEntries == 1) {
            whatHappened.append("we found 1 entry as expected,\n");
        }
        else {
            whatHappened.append("we expected to find 1 entry, but found " + nrEntries + "\n");
        }
        whatHappened.append("We found the following path entries:\n")
        .append(foundEntries);

        assert (nrEntries == 1) && foundEntry && !foundFolder : whatHappened.toString();
    }

    /**
     * We create a bundle, which contains another bundle that provides an export to which
     * the first should wire. This means that the outer bundle can only resolve once the inner
     * bundle has been extracted and installed, which proves that the resource is packaged
     * and extracted correctly.
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"6.1.4", "6.1.4.20"}),
        @OSGiSpec(version="4.2", sections={"6.1.4", "6.1.4.22"})
    })
    public void testGetResourceBasic() throws BundleException, IOException {
        BundleSpecifier innerSpec = m_bu.createBundleSpecifier("inner")
        .addExport(m_bu.createExportPackage(fooPackage));

        BundleSpecifier outerSpec = m_bu.createBundleSpecifier("outer")
        .addImport(m_bu.createImportPackage(fooPackage))
        .pack(innerSpec, false);

        Bundle outer = m_bu.installBundle(outerSpec);

        assert outer.getState() == Bundle.INSTALLED : "The outer bundle should simply be installed.";

        URL jarResource = outer.getResource(innerSpec.getJarName());
        assert jarResource != null : "getResource should have returned the jar URL of the inner bundle.";

        Bundle inner = m_context.installBundle("inner", jarResource.openStream());

        assert inner != null : "We should have been able to install the bundle.";

        assert m_admin.resolveBundles(new Bundle[] {inner, outer}) : "We should be able to resolve both bundles now.";

        m_bu.checkWiring(fooPackage, outer, inner);
    }

    /**
     * Builds a system of three bundles,
     * innerinner ->[q]>- inner ->[p]>- outer,
     * in which the inner is packed in the outer, and the innerinner in the inner.
     * The inner is on the classpath of the outer, and we try to get the innerinner
     * by using the outer's getResource.
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.4", "3.8.4", "6.1.4", "6.1.4.20"}),
        @OSGiSpec(version="4.2", sections={"3.4", "3.8.4", "6.1.4", "6.1.4.22"})
    })
    public void testGetResourceTwoLevels() throws BundleException, IOException {
        BundleSpecifier innerinnerSpec = m_bu.createBundleSpecifier("innerinner")
            .addExport(m_bu.createExportPackage(qPackage));

        BundleSpecifier innerSpec = m_bu.createBundleSpecifier("inner")
            .addExport(m_bu.createExportPackage(pPackage))
            .addImport(m_bu.createImportPackage(qPackage))
            .pack(innerinnerSpec, false);

        BundleSpecifier outerSpec = m_bu.createBundleSpecifier("outer")
            .addImport(m_bu.createImportPackage(pPackage))
            .pack(innerSpec, true)
            .includeDotOnClasspath(true);

        Bundle outer = m_bu.installBundle(outerSpec);

        assert outer.getState() == Bundle.INSTALLED : "The outer bundle should simply be installed.";

        URL jarResource = outer.getResource(innerSpec.getJarName());
        assert jarResource != null : "getResource should have returned the jar URL of the inner bundle.";

        Bundle inner = m_context.installBundle("inner", jarResource.openStream());
        Bundle innerinner = m_context.installBundle("innerinner", outer.getResource(innerinnerSpec.getJarName()).openStream());

        assert inner != null : "We should have been able to install the bundle.";

        assert m_admin.resolveBundles(new Bundle[] {innerinner, inner, outer}) : "We should be able to resolve both bundles now.";

        m_bu.checkWiring(pPackage, outer, inner);
        m_bu.checkWiring(qPackage, inner, innerinner);
    }

    /**
     * Tests a number of different getResource scenarios, by packing (or not)
     * a resource with the same name into the inner or outer bundle, and putting
     * the outer and inner on the classpath (or not).
     * @throws IOException
     * @throws BundleException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"6.1.4", "6.1.4.20"}),
        @OSGiSpec(version="4.2", sections={"6.1.4", "6.1.4.22"})
    })
    public void testGetResource() throws IOException, BundleException {
        boolean[] allBools = new boolean[] {true, false};
        for (boolean inOuter : allBools) {
            for (boolean inInner : allBools) {
                for (boolean outerInClasspath : allBools) {
                    for (boolean innerInClasspath : allBools) {
                        _testGetResource(inOuter,
                            inInner,
                            outerInClasspath,
                            innerInClasspath,
                            inOuter && ( outerInClasspath || (!outerInClasspath && !innerInClasspath) ),
                            inInner && innerInClasspath);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void _testGetResource(boolean inOuter, boolean inInner, boolean outerOnClasspath, boolean innerOnClasspath, boolean shouldFindOuter, boolean shouldFindInner) throws IOException, BundleException {
        StringBuilder whatHappened = new StringBuilder(" (")
        .append((inOuter?"":"not ")+"packing in outer, ")
        .append((inInner?"":"not ")+"packing in inner, ")
        .append("outer "+(outerOnClasspath?"":"not ")+"on classpath, ")
        .append("inner "+(innerOnClasspath?"":"not ")+"on classpath, ")
        .append("so we should "+(shouldFindOuter?"":"not ")+"see outer, ")
        .append("and we should "+(shouldFindInner?"":"not ")+"see inner.)");

        String streamName = "myStream";
        String innerStream = "innerContents";
        String outerStream = "outerContents";

        BundleSpecifier innerSpec = m_bu.createBundleSpecifier("inner");
        if (inInner) {
            innerSpec.pack(streamName, new ByteArrayInputStream(innerStream.getBytes()));
        }

        BundleSpecifier outerSpec = m_bu.createBundleSpecifier("outer");
        if (inOuter) {
            outerSpec.pack(streamName, new ByteArrayInputStream(outerStream.getBytes()));
        }

        if (outerOnClasspath) {
        outerSpec.includeDotOnClasspath(outerOnClasspath);
        }

        outerSpec.pack(innerSpec, innerOnClasspath);

        Bundle outer = m_bu.installBundle(outerSpec);

        ByteArrayOutputStream generatedManifest = new ByteArrayOutputStream();
        outerSpec.getManifest().write(generatedManifest);
        whatHappened.append("\n( manifest as generated by specifier: \n")
        .append(new String(generatedManifest.toByteArray()))
        .append(")\n");

        whatHappened.append("\n( outer bundle headers, once installed: \n");
        Dictionary outerHeaders = outer.getHeaders();
        Enumeration outerHeaderKeys = outerHeaders.keys();
        while (outerHeaderKeys.hasMoreElements()) {
            String key = (String) outerHeaderKeys.nextElement();
            Object value = outerHeaders.get(key);
            whatHappened.append(" " + key.toString())
            .append(": ")
            .append(value.toString())
            .append('\n');
        }
        whatHappened.append(")\n");

        String classPath = new StringBuilder()
        .append("Classpath according to specifier: ")
        .append(outerSpec.getManifest().getMainAttributes().getValue(Constants.BUNDLE_CLASSPATH))
        .append(" and according to bundle: ")
        .append(outer.getHeaders().get(Constants.BUNDLE_CLASSPATH))
        .toString();
        whatHappened.append(classPath + "\n");

        // First, check which one getResource finds, and whether that is the one we expect (if any).
        // Note the order of the else if: we expect to find the outer first, since we first put the .
        // on the classpath.
        URL foundURL = outer.getResource(streamName);
        if (!shouldFindInner && !shouldFindOuter) {
            assert foundURL == null : "Since we do not expect to find any resource, getResource should return null" + whatHappened;
        }
        else if (shouldFindOuter){
            assert foundURL != null : "We expect to see the inner resource, so getResource should not return null" + whatHappened;
            String foundString = getInputStreamAsString(foundURL.openStream());
            assert outerStream.equals(foundString) : "We expect to see the outer resource \"" + outerStream + "\", but we found \""+foundString+"\".\n" + whatHappened + "\n" + classPath;
        }
        else if (shouldFindInner){
            assert foundURL != null : "We expect to see the inner resource, so getResource should not return null" + whatHappened;
            String foundString = getInputStreamAsString(foundURL.openStream());
            assert innerStream.equals(foundString) : "We expect to see the inner resource \"" + innerStream + "\", but we found \""+foundString+"\".\n" + whatHappened + "\n" + classPath;
        }

        // Then, use getResources to check whether we find everything we should find, and no more.
        Enumeration resources = outer.getResources(streamName);
        boolean foundOuter = false;
        boolean foundInner = false;
        int totalFound = 0;
        whatHappened.append("( Found resource URLs:\n");

        if (resources != null) {
            while (resources.hasMoreElements()) {
                totalFound++;
                foundURL = (URL) resources.nextElement();
                whatHappened.append(foundURL.toString() + "\n");
                String foundString = getInputStreamAsString(foundURL.openStream());
                if (outerStream.equals(foundString)) {
                    foundOuter = true;
                }
                if (innerStream.equals(foundString)) {
                    foundInner = true;
                }
            }
        }
        whatHappened.append(")");
        if (shouldFindInner && shouldFindOuter) {
            assert totalFound == 2 : "We should have found 2 resources, but found "+totalFound+": " + whatHappened;
        }
        else if (shouldFindInner) {
            assert totalFound == 1 : "We should have found 1 resource, but found "+totalFound+": " + whatHappened;

        }
        else if (shouldFindOuter) {
            assert totalFound == 1 : "We should have found 1 resource, but found "+totalFound+": " + whatHappened;

        }
        else {
            assert totalFound == 0 : "We should have found no resources, but found "+totalFound+": " + whatHappened;
        }
        assert (shouldFindInner == foundInner) && (shouldFindOuter == foundOuter) : "We did " + (foundInner?"not ":"") + "find inner, and did " + (foundInner?"not ":"") + "find outer: " + whatHappened.toString();

        outer.uninstall();
    }

    private String getInputStreamAsString(InputStream in) throws IOException {
        char[] buf = new char[1];
        StringBuilder found = new StringBuilder();
        InputStreamReader bf = new InputStreamReader(in);
        while (bf.read(buf) > 0) {
            found.append(buf);
        }
        bf.close();
        return found.toString();
    }
}
