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

import java.io.IOException;

import net.luminis.osgitest.testhelper.OSGiSpec;
import net.luminis.osgitest.testhelper.OSGiVersionSpecs;
import net.luminis.osgitest.testhelper.TestBase;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Tests for fragment bundles
 *
 */
public class FragmentTest extends TestBase {

    /**
     * Test if packages are properly exported when the originate from fragmented bundles.
     *
     * @throws BundleException
     * @throws IOException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.14.2"}),
        @OSGiSpec(version="4.2", sections={"3.13.2"})
    })
    public void testPackageExport() throws BundleException, IOException {
        //setup
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addExport(m_bu.createExportPackage(pPackage))
            .pack(PInterface1));
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .setFragmentHost(a)
            .pack(PInterface2));
        Bundle t = m_bu.installBundle(m_bu.createBundleSpecifier("t")
            .addImport(m_bu.createImportPackage(pPackage)));

        //resolve bundles
        m_admin.resolveBundles(new Bundle[] {a, b, t});

        //check wiring
        m_bu.checkWiring(pPackage, t, a);
        m_bu.checkWiring(pPackage, t, b, false);

        //check if classes can be reached
        assert m_bu.isReachable(PInterface1, t) : "PInterface1 should be reachable from bundle t";
        assert m_bu.isReachable(PInterface2, t) : "PInterface2 should be reachable from bundle t";
    }

    /**
     * Test if imported packages takes precedence over fragmented packages and
     * that imported packages aren't split with packages from fragmented bundles.
     *
     * @throws BundleException
     * @throws IOException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.14.2"}),
        @OSGiSpec(version="4.2", sections={"3.13.2"})
    })
    public void testPackageImport1() throws BundleException, IOException {
        //setup
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addImport(m_bu.createImportPackage(qPackage)));
        Bundle d = m_bu.installBundle(m_bu.createBundleSpecifier("d")
            .addExport(m_bu.createExportPackage(qPackage))
            .pack(QInterface1));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .setFragmentHost(a)
            .pack(QInterface2));

        //resolve bundles
        m_admin.resolveBundles(new Bundle[] {d, a, c});

        //check wiring
        m_bu.checkWiring(qPackage, a, d);

        //check if classes can be reached
        assert m_bu.isReachable(QInterface1, a) : "QInterface1 should be reachable from bundle a";
        assert !m_bu.isReachable(QInterface2, a) : "QInterface2 shouldn't be reachable from bundle a";
    }

    /**
     * Test if imported packages takes precedence over fragmented packages and
     * that imported packages aren't split with packages from fragmented bundles.
     *
     * @throws BundleException
     * @throws IOException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.14.2"}),
        @OSGiSpec(version="4.2", sections={"3.13.2"})
    })
    public void testPackageImport2() throws BundleException, IOException {
        //setup
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addImport(m_bu.createImportPackage(pPackage))
            .pack(PInterface1));
        Bundle d = m_bu.installBundle(m_bu.createBundleSpecifier("d")
            .addExport(m_bu.createExportPackage(pPackage))
            .pack(PInterface2));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .setFragmentHost(a)
            .pack(PInterface3));

        //resolve bundles
        m_admin.resolveBundles(new Bundle[] {d, a, c});

        //check wiring
        m_bu.checkWiring(pPackage, a, d);

        //check if classes can be reached
        assert !m_bu.isReachable(PInterface1, a) : "PInterface1 shouldn't be reachable from bundle a";
        assert m_bu.isReachable(PInterface2, a) : "PInterface2 should be reachable from bundle a";
        assert !m_bu.isReachable(PInterface3, a) : "PInterface3 shouldn't be reachable from bundle a";
    }

    /**
     * Test if classes and packages from different fragments are accessible from the host package
     *
     * @throws BundleException
     * @throws IOException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.14.2"}),
        @OSGiSpec(version="4.2", sections={"3.13.2"})
    })
    public void testMultipleFragments() throws BundleException, IOException {
        //setup
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .pack(RInterface1));
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .setFragmentHost(a)
            .pack(RInterface2)
            .pack(TInterface1));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .setFragmentHost(a)
            .pack(SInterface)
            .pack(TInterface2));


        //resolve bundles
        m_admin.resolveBundles(new Bundle[] {a, b, c});

        //check if classes can be reached
        assert m_bu.isReachable(RInterface1, a) : "RInterface1 should be reachable from bundle a";
        assert m_bu.isReachable(RInterface2, a) : "RInterface1 should be reachable from bundle a";
        assert m_bu.isReachable(SInterface, a) : "SInterface should be reachable from bundle a";
        assert m_bu.isReachable(TInterface1, a) : "TInterface1 should be reachable from bundle a";
        assert m_bu.isReachable(TInterface2, a) : "TInterface2 should be reachable from bundle a";
    }

}

