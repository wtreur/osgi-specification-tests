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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.luminis.osgitest.testhelper.OSGiSpec;
import net.luminis.osgitest.testhelper.OSGiVersionSpecs;
import net.luminis.osgitest.testhelper.TestBase;
import net.luminis.osgitest.testhelper.packages.q.QInterface1;
import net.luminis.osgitest.testhelper.packages.r.RInterface1;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Tests the framework's module layer: check general wiring tests.
 */
public class WiringTest extends TestBase {

    /**
     * Tests whether we can use the package admin, and it comes from the right source.
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"7.1.3"}),
        @OSGiSpec(version="4.2", sections={"7.1.3"})
    })
    public void testPackageAdmin() {
        // First, try to find the admin package indirectly.
        Bundle packageAdminExporter = m_bu.getWiredPackageExporter(PackageAdmin.class.getPackage(), m_context.getBundle());
        assert packageAdminExporter != null : "Couldn't find the admin package";

        assert packageAdminExporter.getBundleId() == 0 : "Only the System Bundle should export the Package Admin.";

        // Then, see what the package admin itself has to say about this.
        packageAdminExporter = m_admin.getBundle(PackageAdmin.class);
        assert packageAdminExporter != null : "Couldn't find the admin package";

        assert packageAdminExporter.getBundleId() == 0 : "Only the System Bundle should export the Package Admin.";
    }

    /**
     * Tests whether a consumer gets connected to the highest version of a package available.
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.5", "3.6.2", "3.7"}),
        @OSGiSpec(version="4.2", sections={"3.2.6", "3.6.2", "3.7"})
    })
    public void testWireHighestVersion() throws BundleException, IOException {
        // Create our setup
        Bundle a1 = m_bu.installImpExBundle("a1", Foo, null, new String[] { "1.1" }, null);
        Bundle a2 = m_bu.installImpExBundle("a2", Foo, null, new String[] { "1.2" }, null);
        Bundle b = m_bu.installImpExBundle("b1", Foo, null, null, "");

        // Resolve the bundles
        m_admin.resolveBundles(new Bundle[] { a1, a2, b });

        // Check what the package admin has to say about the wiring.
        m_bu.checkWiring(fooPackage, b, a2);

        assert m_bu.getWiredPackageVersion(fooPackage, b).equals("1.2.0") : "b2 should be wired to version 1.2.0, but is wired to version " + m_bu.getWiredPackage(fooPackage, b).getVersion().toString();

        //Now, try to start the bundles and see whether it can reach Foo.
        b.start();
        assert m_bu.isReachable(Foo, b) : "Foo should be reachable from b.";
    }

    /**
     * Tests the correct wiring of restricted consumers to a provider with multiple exports.
     *
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.5", "3.6.2"}),
        @OSGiSpec(version="4.2", sections={"3.2.6", "3.6.2"})
    })
    public void testVersionedResolvingWithMultipleConsumers() throws BundleException, IOException {
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("1.1"))
            .addExport(m_bu.createExportPackage(rPackage).setVersion("1.1"))
            .addExport(m_bu.createExportPackage(rPackage).setVersion("1.2"))
            .pack(QInterface1.class)
            .pack(RInterface1.class));
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .addExport(m_bu.createExportPackage(qPackage).setVersion("1.2"))
            .pack(QInterface1.class));
        Bundle c = m_bu.installBundle(m_bu.createBundleSpecifier("c")
            .addImport(m_bu.createImportPackage(qPackage).setVersion("[1.2,1.2]"))
            .addImport(m_bu.createImportPackage(rPackage).setVersion("[1.2,1.2]")));

        m_admin.resolveBundles(new Bundle[] { a, b, c});

        m_bu.checkWiring(qPackage, c, b);
        m_bu.checkWiring(rPackage, c, a);

        assert m_bu.getWiredPackageVersion(qPackage, c).equals("1.2.0") : "c should be wired to version qPackage version 1.2.0, but is wired to version " + m_bu.getWiredPackage(qPackage, c).getVersion().toString();
        /*
         * Note: It is not useful to check if rPackage is wired to v1.2, since they can't be
         * different from each other and the specification doesn't require this.
         */

        c.start();
        assert m_bu.isReachable(QInterface1.class, c) : "QInterface should be reachable from c";
        assert m_bu.isReachable(RInterface1.class, c) : "RInterface should be reachable from c";
        c.stop();
    }

    /**
     * Tests the version checking mechanism for (optionally) multiple-versioned exports, and (optionally) single-version-ranged
     * imports. This also covers simple resolving.
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.5", "3.6.2"}),
        @OSGiSpec(version="4.2", sections={"3.2.6", "3.6.2"})
    })
    public void testVersionedResolving() throws BundleException, IOException {
        testVersionedResolve(new String[] {}, "", true);
        cleanupBundles();
        testVersionedResolve(new String[] { "1.1" }, "[1.1,1.1]", true);
        cleanupBundles();
        testVersionedResolve(new String[] { "2.0" }, "[1.0,2.0)", false);
        cleanupBundles();
        testVersionedResolve(new String[] { "1.1" }, "[1.0,1.1)", false);
        cleanupBundles();
        testVersionedResolve(new String[] { "1.1", "1.0" }, "[1.0,1.1)", true);
        cleanupBundles();
        testVersionedResolve(new String[] { "1.1", "1.0" }, "(1.0,1.1]", true);
        cleanupBundles();
        testVersionedResolve(new String[] { "1.0" }, "(1.0,1.1]", false);
        cleanupBundles();
        testVersionedResolve(new String[] { "1.0" }, "(1.0,1.1)", false);
        cleanupBundles();
        testVersionedResolve(new String[] { "1.0.2" }, "(1.0,1.1)", true);
        cleanupBundles();
    }

    /**
     * Helper method for checking (optionally) multiple-versioned exports, and single- version-ranged imports of the same
     * package.
     *
     * @param exportedVersion The versions that are to be exported of the package by the exporter.
     * @param importedVersion The version range to be imported by the importer.
     * @param allowed Whether or not this combination is supposed to be allowed; will assert on violation.
     */
    private void testVersionedResolve(String[] exportedVersion, String importedVersion, boolean allowed) throws BundleException, IOException {
        StringBuilder exported = new StringBuilder();
        if (exportedVersion != null) {
            for (String version : exportedVersion) {
                if (exported.length() > 0) {
                    exported.append(", ");
                }
                exported.append(version);
            }
        }
        else {
            exported.append("(none)");
        }

        boolean isAllowed;
        try {
            testVersionedResolve(exportedVersion, importedVersion);
            isAllowed = true;
        }
        catch (AssertionError e) {
            isAllowed = false;
        }
        assert allowed == isAllowed : "Exporting versions " + exported.toString() + " and importing version " + ((importedVersion == null) ? "(none)" : importedVersion) + " should " + (allowed ? "" : "not") + " resolve.";
    }

    /**
     * Helper-helper method for testVersionedResolve
     */
    private void testVersionedResolve(String[] exportedVersion, String importedVersion) throws BundleException, IOException {
        Bundle a = m_bu.installImpExBundle("a", Foo, null, exportedVersion, null);
        Bundle b = m_bu.installImpExBundle("b", Foo, null, null, importedVersion);

        assert m_admin.resolveBundles(new Bundle[] { a, b }) : "Cannot resolve a and b.";

        m_bu.checkWiring(fooPackage, b, a);

        b.start();
        assert m_bu.isReachable(Foo, b) : "Foo should be reachable from b1.";
    }

    /**
     * Tests an unwiring scenario: provider is A started and stopped, B is wired to a through foo, A is removed, and C (which
     * provides foo) is installed. Before a refresh, B should remain wired to A (which is about to be removed), and after a
     * refresh, A is gone, and B is wired to C.
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"4.3.11", "7.4.3", "7.4.3.11"}),
        @OSGiSpec(version="4.2", sections={"4.4.10", "7.4.3", "7.5.3.11"})
    })
    public void testUninstallUnwiring1() throws IOException, BundleException {
        Bundle a = m_bu.installImpExBundle("A", Foo, null, new String[] {}, null);
        a.start();
        a.stop();

        assert !m_bu.isProvidingPackages(a) : "A should not be providing anything.";
        Bundle b = m_bu.installImpExBundle("B", Foo, null, null, "");
        assert m_admin.resolveBundles(new Bundle[] { b }) : "B should be able to resolve, since a is around.";

        assert m_bu.isProvidingPackages(a) : "A should provide something.";

        a.uninstall();

        m_bu.checkWiring(fooPackage, b, a);

        Bundle c = m_bu.installImpExBundle("C", Foo, null, new String[] {}, null);

        m_bu.checkWiring(fooPackage, b, a);

        m_bu.refreshFrameworkAndWait(null);

        assert m_admin.resolveBundles(new Bundle[] { b }) : "B should be able to resolve, since C is around.";
        assert !(a.getBundleId() == m_bu.getWiredPackageExporter(fooPackage, b).getBundleId()) : "After refresh, B should no longer be wired to A.";
        assert c.getBundleId() == m_bu.getWiredPackageExporter(fooPackage, b).getBundleId() : "After refresh, B should be wired to C.";
        assert !m_bu.isProvidingPackages(a) : "After refresh, A should no longer provide anything.";
        assert m_bu.isReachable(Foo, b) : "After refresh, Foo should be reachable from B.";
    }

    /**
     * Tests a special unwiring scenario: after removing a providing bundle, the registration should no longer be around.
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"4.3.11"}),
        @OSGiSpec(version="4.2", sections={"4.4.10"})
    })
    public void testUninstallUnwiring2() throws IOException, BundleException {
        Bundle a = m_bu.installImpExBundle("a", Foo, null, new String[] {}, null);

        a.start();
        a.stop();

        assert !m_bu.isProvidingPackages(a) : "a should not be providing anything.";

        a.uninstall();

        Bundle b = m_bu.installImpExBundle("b", Foo, null, null, "");

        assert !m_admin.resolveBundles(new Bundle[] { b }) : "b should not be able to resolve since a is gone.";

    }

    /**
     * Tests a scenario in which a single bundle exports multiple versions of the same package; this should be allowed.
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.5.5"}),
        @OSGiSpec(version="4.2", sections={"3.5.5"})
    })
    public void testMultipleExports() throws IOException {
        try {
            // Create a bundle that exports the same package twice but in different versions.
            Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
                .addExport(m_bu.createExportPackage(fooPackage).setVersion("1.1"))
                .addExport(m_bu.createExportPackage(fooPackage).setVersion("1.2")));
            a.start();

            ExportedPackage[] packages = m_admin.getExportedPackages(a);
            assert packages != null : "a should at least export something.";
            if (packages != null) {
                List<String> headers = new ArrayList<String>();
                // Get all the exports.
                for (ExportedPackage p : packages) {
                    headers.add(p.getName() + ";version=" + p.getVersion());
                }
                List<String> expected = Arrays.asList(new String[] { Foo.getPackage().getName() + ";version=1.1.0", Foo.getPackage().getName() + ";version=1.2.0" });
                assert headers.size() == 2 : "Two versions of net.luminis.test.osgi.helper.foo should be found, 1.1 and 1.2. (we found " + headers.size() + ")";
                assert headers.containsAll(expected) : "Wrong packages are exported: \n" + headers.get(0) + ",\n " + headers.get(1) + "\nBut we expect \n" + expected.get(0) + ",\n " + expected.get(1);
            }
            a.stop();
            a.uninstall();
            m_admin.refreshPackages(null);

        }
        catch (BundleException e) {
            assert false : "Multiple exports should be allowed.";
        }

    }

    /**
     * Tests the behavior with an optional resolve. The using bundle should be able to be started, but not wired; after a
     * provider shows up, the wire should not be created before a refresh. Uninstalling is tested in this case too.
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.6.3", "4.3.11", "6.1.4", "6.1.4.31", "7.4.3", "7.4.3.11"}),
        @OSGiSpec(version="4.2", sections={"3.6.3", "4.4.10", "6.1.4", "6.1.4.35", "7.4.3", "7.4.3.11"})
    })
    public void testOptional() throws BundleException, IOException {
        // Create a lonesome bundle B with an optional import.
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .addImport(m_bu.createImportPackage(fooPackage)
                .setResolutionOptional(true)));
        try {
            b.start();
        }
        catch (BundleException e) {
            assert false : "With an optional import, b should be able to be started, but: " + e.getMessage();
        }

        assert !m_bu.isReachable(Foo, b) : "b should not be able to reach Foo.";

        // Create a bundle A that exports the fooPackage that B needs.
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addExport(m_bu.createExportPackage(fooPackage))
            .addClass(Foo));

        // Without a refresh, the wiring should not have changed because of the appearance of A.
        assert !m_bu.isProvidingPackages(a) : "Bundle a should not provide any package now";
        m_bu.checkWiring(fooPackage, b, a, false);
        assert !m_bu.isReachable(Foo, b) : "b should not be able to reach Foo.";

        m_bu.refreshFrameworkAndWait(new Bundle[] { a, b });

        // After the refresh, we should have a wire, and b should still be running.
        m_bu.checkWiring(fooPackage, b, a);
        assert b.getState() == Bundle.ACTIVE : "Bundle b should be active.";
        assert m_bu.isReachable(Foo, b) : "b should be able to reach Foo.";

        // Even when we uninstall a, the wire should still exist.
        a.uninstall();
        m_bu.checkWiring(fooPackage, b, a);
        assert b.getState() == Bundle.ACTIVE : "Bundle b should be active.";
        assert m_bu.isReachable(Foo, b) : "b should be able to reach Foo.";

        // But after another refresh, the wire should be gone.
        m_bu.refreshFrameworkAndWait(new Bundle[] { a, b });
        m_bu.checkWiring(fooPackage, b, a, false);
        assert !m_bu.isReachable(Foo, b) : "b should not be able to reach Foo.";
    }

    /**
     * Tests processing of non-mandatory attribute specifications and requirements.
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.6.5"}),
        @OSGiSpec(version="4.2", sections={"3.6.5"})
    })
    public void testNonMandatoryAttributeMatching() throws BundleException, IOException {
        final String COMPANY_KEY = "Company";
        final String COMPANY_STRING = "Reynhold Industries";
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addExport(m_bu.createExportPackage(pPackage)
                .addAttribute(COMPANY_KEY, COMPANY_STRING)));

        Bundle b1 = m_bu.installBundle(m_bu.createBundleSpecifier("b1")
            .addImport(m_bu.createImportPackage(pPackage)));
        assert m_admin.resolveBundles(new Bundle[] {b1}) : "b1 should be able to be resolved, since it does not impose any constraints.";
        m_bu.checkWiring(pPackage, b1, a);

        Bundle b2 = m_bu.installBundle(m_bu.createBundleSpecifier("b2")
            .addImport(m_bu.createImportPackage(pPackage)
                .addAttribute(COMPANY_KEY, "IKEA")));
        assert !m_admin.resolveBundles(new Bundle[] {b2}) : "b2 should not be able to be resolved, since specified attributes must match.";
        m_bu.checkWiring(pPackage, b2, a, false);

        Bundle b3 = m_bu.installBundle(m_bu.createBundleSpecifier("b3")
            .addImport(m_bu.createImportPackage(qPackage)
                .addAttribute(COMPANY_KEY, COMPANY_STRING)));
        assert !m_admin.resolveBundles(new Bundle[] {b3}) : "b3 should not be able to be resolved, since it specifies a satisfied constraint, but on the wrong package.";
        m_bu.checkWiring(pPackage, b3, a, false);

        Bundle b4 = m_bu.installBundle(m_bu.createBundleSpecifier("b4")
            .addImport(m_bu.createImportPackage(pPackage)
                .addAttribute(COMPANY_KEY, "*")));
        assert !m_admin.resolveBundles(new Bundle[] {b4}) : "b4 should not be able to be resolved, since wildcard matching is not supported by attributes.";
        m_bu.checkWiring(pPackage, b4, a, false);

        Bundle b5 = m_bu.installBundle(m_bu.createBundleSpecifier("b5")
            .addImport(m_bu.createImportPackage(pPackage)
                .addAttribute("Customer", COMPANY_STRING)));
        assert !m_admin.resolveBundles(new Bundle[] {b5}) : "b5 should not be able to be resolved, since it specifies an attribute that is not specified by the exporter.";
        m_bu.checkWiring(pPackage, b5, a, false);

        Bundle b6 = m_bu.installBundle(m_bu.createBundleSpecifier("b6")
            .addImport(m_bu.createImportPackage(pPackage)
                .addAttribute(COMPANY_KEY, COMPANY_STRING)));
        assert m_admin.resolveBundles(new Bundle[] {b6}) : "b6 should be able to be resolved, since it specifies the correct attributes.";
        m_bu.checkWiring(pPackage, b6, a);
    }

    /**
     * Tests processing of mandatory attribute specifications and requirements.
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.6.6"}),
        @OSGiSpec(version="4.2", sections={"3.6.6"})
    })
    public void testMandatoryAttributeMatching() throws BundleException, IOException {

        final String COMPANY_KEY = "Company";
        final String COMPANY_STRING = "Reynhold Industries";
        Bundle a = m_bu.installBundle(m_bu.createBundleSpecifier("a")
            .addExport(m_bu.createExportPackage(pPackage)
                .addAttribute(COMPANY_KEY, COMPANY_STRING, true)));

        Bundle b1 = m_bu.installBundle(m_bu.createBundleSpecifier("b1")
            .addImport(m_bu.createImportPackage(pPackage)));
        assert !m_admin.resolveBundles(new Bundle[] {b1}) : "b1 should not be able to be resolved, since it does have the company attribute.";
        m_bu.checkWiring(pPackage, b1, a, false);

        Bundle b2 = m_bu.installBundle(m_bu.createBundleSpecifier("b2")
            .addImport(m_bu.createImportPackage(qPackage)
                .addAttribute(COMPANY_KEY, "IKEA")));
        assert !m_admin.resolveBundles(new Bundle[] {b2}) : "b2 should not be able to be resolved, since the attribute is mandatory in the provider.";
        m_bu.checkWiring(pPackage, b2, a, false);

        Bundle b3 = m_bu.installBundle(m_bu.createBundleSpecifier("b3")
            .addImport(m_bu.createImportPackage(qPackage)
                .addAttribute(COMPANY_KEY, COMPANY_STRING)));
        assert !m_admin.resolveBundles(new Bundle[] {b3}) : "b3 should not be able to be resolved, since it specifies a satisfied constraint, but on the wrong package.";
        m_bu.checkWiring(pPackage, b3, a, false);

        Bundle b4 = m_bu.installBundle(m_bu.createBundleSpecifier("b4")
            .addImport(m_bu.createImportPackage(pPackage)
                .addAttribute(COMPANY_KEY, "*")));
        assert !m_admin.resolveBundles(new Bundle[] {b4}) : "b4 should not be able to be resolved, since wildcard matching is not supported by attributes.";
        m_bu.checkWiring(pPackage, b4, a, false);

        Bundle b5 = m_bu.installBundle(m_bu.createBundleSpecifier("b5")
            .addImport(m_bu.createImportPackage(pPackage)
                .addAttribute(COMPANY_KEY, COMPANY_STRING)));
        assert m_admin.resolveBundles(new Bundle[] {b5}) : "b5 should be able to be resolved, since it specifies the correct attributes.";
        m_bu.checkWiring(pPackage, b5, a);
    }

}
