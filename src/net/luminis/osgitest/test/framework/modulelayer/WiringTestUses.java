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
 * Tests the framework's module layer: checks uses constraints.
 */
public class WiringTestUses extends TestBase {

    /**
     * Recreates a case from spec section 3.6.4, fig 3.16, (fig3.17 in r4.2) in the original shape.
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.6.4"}),
        @OSGiSpec(version="4.2", sections={"3.6.4"})
    })
    public void testSpec364fig316case1() throws IOException, BundleException {
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a").
            addImport(m_bu.createImportPackage(pPackage)).
            addImport(m_bu.createImportPackage(tPackage)));
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .addImport(m_bu.createImportPackage(qPackage))
            .addExport(m_bu.createExportPackage(pPackage).addUses(qPackage)));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .addImport(m_bu.createImportPackage(rPackage))
            .addImport(m_bu.createImportPackage(sPackage))
            .addExport(m_bu.createExportPackage(qPackage)
                .addUses(rPackage)
                .addUses(sPackage)));
        Bundle d = m_bu.installBundle(m_bu.createBundleSpecifier("d")
            .addImport(m_bu.createImportPackage(tPackage))
            .addExport(m_bu.createExportPackage(sPackage)
                .addUses(tPackage))
            .addExport(m_bu.createExportPackage(tPackage)
                .addUses(tPackage)));
        Bundle e = m_bu.installBundle(m_bu.createBundleSpecifier("e")
            .addImport(m_bu.createImportPackage(tPackage))
            .addExport(m_bu.createExportPackage(rPackage)
                .addUses(tPackage)));
        Bundle f = m_bu.installBundle(m_bu.createBundleSpecifier("f")
            .addExport(m_bu.createExportPackage(tPackage)));

        assert m_admin.resolveBundles(new Bundle[] { a, b, c, d, e, f }) : "This situation should resolve.";

        m_bu.checkWiring(pPackage, a, b);
        m_bu.checkWiring(qPackage, b, c);
        m_bu.checkWiring(rPackage, c, e);
        m_bu.checkWiring(sPackage, c, d);
        m_bu.checkWiring(tPackage, e, d);
        m_bu.checkWiring(tPackage, a, d);
        assert !m_bu.isProvidingPackages(f) : "f should not be wired to any user.";
    }

    /**
     * Recreates a case from spec section 3.6.4, fig 3.16 (fig3.17 in r4.2), in the original shape,
     * but reorders the bundle installation, so the t of f gets used.
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.6.4"}),
        @OSGiSpec(version="4.2", sections={"3.6.4"})
    })
    public void testSpec364fig316case2() throws IOException, BundleException {
        Bundle f = m_bu.installBundle(m_bu.createBundleSpecifier("f")
            .addExport(m_bu.createExportPackage(tPackage)));
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a").
            addImport(m_bu.createImportPackage(pPackage)).
            addImport(m_bu.createImportPackage(tPackage)));
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .addImport(m_bu.createImportPackage(qPackage))
            .addExport(m_bu.createExportPackage(pPackage).addUses(qPackage)));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .addImport(m_bu.createImportPackage(rPackage))
            .addImport(m_bu.createImportPackage(sPackage))
            .addExport(m_bu.createExportPackage(qPackage)
                .addUses(rPackage)
                .addUses(sPackage)));
        Bundle d = m_bu.installBundle(m_bu.createBundleSpecifier("d")
            .addImport(m_bu.createImportPackage(tPackage))
            .addExport(m_bu.createExportPackage(sPackage)
                .addUses(tPackage))
            .addExport(m_bu.createExportPackage(tPackage)
                .addUses(tPackage)));
        Bundle e = m_bu.installBundle(m_bu.createBundleSpecifier("e")
            .addImport(m_bu.createImportPackage(tPackage))
            .addExport(m_bu.createExportPackage(rPackage)
                .addUses(tPackage)));

        assert m_admin.resolveBundles(new Bundle[] { a, b, c, d, e, f }) : "This situation should resolve.";

        m_bu.checkWiring(pPackage, a, b);
        m_bu.checkWiring(qPackage, b, c);
        m_bu.checkWiring(rPackage, c, e);
        m_bu.checkWiring(sPackage, c, d);
        m_bu.checkWiring(tPackage, d, f);
        m_bu.checkWiring(tPackage, e, f);
        m_bu.checkWiring(tPackage, a, f);
    }

    /**
     * Recreates a case from spec section 3.6.4, fig 3.15 (fig 3.16 in r4.2), in the original shape.
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.6.4"}),
        @OSGiSpec(version="4.2", sections={"3.6.4"})
    })
    public void testSpec364fig315case1() throws IOException, BundleException {
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addImport(m_bu.createImportPackage(pPackage))
            .addImport(m_bu.createImportPackage(qPackage)));
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .addImport(m_bu.createImportPackage(qPackage))
            .addExport(m_bu.createExportPackage(pPackage)
                .addUses(qPackage)));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .addExport(m_bu.createExportPackage(qPackage)));
        Bundle d = m_bu.installBundle(m_bu.createBundleSpecifier("d")
            .addExport(m_bu.createExportPackage(qPackage)));

        assert m_admin.resolveBundles(new Bundle[] { a, b, c, d }) : "This situation should resolve.";

        m_bu.checkWiring(pPackage, a, b);
        m_bu.checkWiring(qPackage, a, c);
        m_bu.checkWiring(qPackage, b, c);
        assert !m_bu.isProvidingPackages(d) : "d should not be wired to any user.";
    }

    /**
     * Recreates a case from spec section 3.6.4, fig 3.15 (fig 3.16 in r4.2), but adds a versioning
     * requirement and drops the uses constraint. This resolves, but can cause errors in
     * passing around objects from package q.
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.6.4"}),
        @OSGiSpec(version="4.1", sections={"3.6.4"})
    })
    public void testSpec364fig315case2() throws IOException, BundleException {
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addImport(m_bu.createImportPackage(pPackage))
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[2.1,2.1]")));
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[2.4,2.4]"))
            .addExport(m_bu.createExportPackage(pPackage)));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("2.1")));
        Bundle d = m_bu.installBundle(m_bu.createBundleSpecifier("d")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("2.4")));

        assert m_admin.resolveBundles(new Bundle[] { a, b, c, d }) : "This situation should resolve.";

        m_bu.checkWiring(pPackage, a, b);
        m_bu.checkWiring(qPackage, a, c);
        m_bu.checkWiring(qPackage, b, d);
    }

    /**
     * Recreates a case from spec section 3.6.4, fig 3.15 (fig 3.16 in r4.2), with added version
     * constraints. This should not resolve, since a gets wired to c and b to d (due to the version
     * constraints), but a and b cannot be wired together since they use a different instance of q.
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.6.4"}),
        @OSGiSpec(version="4.2", sections={"3.6.4"})
    })
    public void testSpec364fig315case3() throws IOException, BundleException {
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addImport(m_bu.createImportPackage(pPackage))
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[2.1,2.1]")));
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[2.4,2.4]"))
            .addExport(m_bu.createExportPackage(pPackage)
                .addUses(qPackage)));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("2.1")));
        Bundle d = m_bu.installBundle(m_bu.createBundleSpecifier("d")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("2.1")));

        assert !m_admin.resolveBundles(new Bundle[] { a, b, c, d }) : "This situation should not resolve, due to the uses constraint.";
    }

    /**
     * Recreates a case from spec section 3.6.4, fig 3.17 (fig 3.18 in r4.2)
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.6.4"}),
        @OSGiSpec(version="4.2", sections={"3.6.4"})
    })
    public void testSpec364fig317() throws IOException, BundleException {
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[1.0,1.0]"))
            .addExport(m_bu.createExportPackage(pPackage)
                .addUses(qPackage))
            .addExport(m_bu.createExportPackage(rPackage)));
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("1.0")));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("2.0")));

        assert m_admin.resolveBundles(new Bundle[] { a, b }) : "This situation should resolve.";

        m_bu.checkWiring(qPackage, a, b);
        assert !m_bu.isProvidingPackages(c) : "c should not be wired to any user.";

        Bundle d = m_bu.installBundle(m_bu.createBundleSpecifier("d")
            .addImport(m_bu.createImportPackage(pPackage).setVersion("[2.0,2.0]"))
            .addImport(m_bu.createImportPackage(qPackage)));

        assert !m_admin.resolveBundles(new Bundle[] { d }) : "This situation should not resolve, due to the uses constraint.";
    }

    /**
     * Recreates a case from spec section 3.6.4, fig 3.17 (fig 3.18 in r4.2)
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.6.4"}),
        @OSGiSpec(version="4.2", sections={"3.6.4"})
    })
    public void testSpec364fig317case2() throws IOException, BundleException {
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[1.0,1.0]"))
            .addExport(m_bu.createExportPackage(pPackage)
                .setVersion("1.0")
                .addUses(qPackage))
            .addExport(m_bu.createExportPackage(rPackage)));
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("1.0")));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("2.0")));

        assert m_admin.resolveBundles(new Bundle[] { a, b }) : "This situation should resolve.";

        m_bu.checkWiring(qPackage, a, b);
        assert !m_bu.isProvidingPackages(c) : "c should not be wired to any user.";

        Bundle d = m_bu.installBundle(m_bu.createBundleSpecifier("d")
            .addImport(m_bu.createImportPackage(pPackage).setVersion("[2.0,2.0]"))
            .addImport(m_bu.createImportPackage(qPackage)));

        assert !m_admin.resolveBundles(new Bundle[] { d }) : "This situation should not resolve, due to the uses constraint.";
    }

    /**
     * This test uses unspecified behavior: it assumes the most 'logical' thing to do. Failing it is no problem.
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.6.4"}),
        @OSGiSpec(version="4.2", sections={"3.6.4"})
    })
    public void testMultipleVersionExportsWithDifferentUses() throws IOException, BundleException {
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[1.0,1.0]"))
            .addImport(m_bu.createImportPackage(rPackage).setVersion("[1.0,1.0]"))
            .addExport(m_bu.createExportPackage(pPackage)
                .setVersion("1.0")
                .addUses(qPackage))
            .addExport(m_bu.createExportPackage(pPackage)
                .setVersion("2.0")
                .addUses(rPackage)));
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .addExport(m_bu.createExportPackage(rPackage).setVersion("1.0")));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("1.0")));
        Bundle d = m_bu.installBundle(m_bu.createBundleSpecifier("d")
            .addImport(m_bu.createImportPackage(pPackage).setVersion("[2.0,2.0]")));
        Bundle e = m_bu.installBundle(m_bu.createBundleSpecifier("e")
            .addExport(m_bu.createExportPackage(rPackage).setVersion("2.0")));
        Bundle f = m_bu.installBundle(m_bu.createBundleSpecifier("f")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("2.0")));

        assert m_admin.resolveBundles(new Bundle[] { a, b, c, d, e, f }) : "This situation should resolve.";

        m_bu.checkWiring(qPackage, a, c);
        m_bu.checkWiring(rPackage, a, b);
        m_bu.checkWiring(pPackage, d, a);
        assert !m_bu.isProvidingPackages(e) : "e should not be wired to any user.";
        assert !m_bu.isProvidingPackages(f) : "f should not be wired to any user.";

        d.update(m_bu.generateBundle(m_bu.createBundleSpecifier("d2")
            .addImport(m_bu.createImportPackage(pPackage).setVersion("[2.0,2.0]"))
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[1.0,1.0]"))
            .addImport(m_bu.createImportPackage(rPackage).setVersion("[1.0,1.0]"))));

        assert m_admin.resolveBundles(new Bundle[] { a, b, c, d, e, f }) : "This situation should resolve.";

        m_bu.checkWiring(qPackage, a, c);
        m_bu.checkWiring(rPackage, a, b);
        m_bu.checkWiring(pPackage, d, a);
        m_bu.checkWiring(qPackage, d, c);
        m_bu.checkWiring(rPackage, d, b);
        assert !m_bu.isProvidingPackages(e) : "e should not be wired to any user.";
        assert !m_bu.isProvidingPackages(f) : "f should not be wired to any user.";

        d.update(m_bu.generateBundle(m_bu.createBundleSpecifier("d3")
            .addImport(m_bu.createImportPackage(pPackage).setVersion("[2.0,2.0]"))
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[2.0,2.0]"))
            .addImport(m_bu.createImportPackage(rPackage).setVersion("[1.0,1.0]"))));

        assert m_admin.resolveBundles(new Bundle[] { a, b, c, d, e, f }) : "This situation should resolve.";

        m_bu.checkWiring(qPackage, a, c);
        m_bu.checkWiring(rPackage, a, b);
        m_bu.checkWiring(pPackage, d, a);
        m_bu.checkWiring(qPackage, d, f);
        m_bu.checkWiring(rPackage, d, b);
        assert !m_bu.isProvidingPackages(e) : "e should not be wired to any user.";

        d.update(m_bu.generateBundle(m_bu.createBundleSpecifier("d4")
            .addImport(m_bu.createImportPackage(pPackage).setVersion("[2.0,2.0]"))
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[2.0,2.0]"))
            .addImport(m_bu.createImportPackage(rPackage))));

        assert m_admin.resolveBundles(new Bundle[] { a, b, c, d, e, f }) : "This situation should resolve.";
        m_bu.checkWiring(qPackage, a, c);
        m_bu.checkWiring(rPackage, a, b);
        m_bu.checkWiring(pPackage, d, a);
        m_bu.checkWiring(qPackage, d, f);
        m_bu.checkWiring(rPackage, d, b);
        assert !m_bu.isProvidingPackages(e) : "e should not be wired to any user.";

        d.update(m_bu.generateBundle(m_bu.createBundleSpecifier("d5")
            .addImport(m_bu.createImportPackage(pPackage).setVersion("[2.0,2.0]"))
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[1.0,1.0]"))
            .addImport(m_bu.createImportPackage(rPackage).setVersion("[2.0,2.0]"))));

        assert !m_admin.resolveBundles(new Bundle[] { a, b, c, d, e, f }) : "This situation should not resolve, since d uses p version 2.0, which uses a r version 1.0, but it already wired to a r version 2.0.";
    }
}
