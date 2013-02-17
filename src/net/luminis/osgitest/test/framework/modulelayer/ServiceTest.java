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

import net.luminis.osgitest.testhelper.BundleSpecifier;
import net.luminis.osgitest.testhelper.OSGiSpec;
import net.luminis.osgitest.testhelper.OSGiVersionSpecs;
import net.luminis.osgitest.testhelper.TestBase;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Tests the framework's service layer: checks service visibility and partitioning.
 */
public class ServiceTest extends TestBase {

    /**
     * We create a bundle that exports an interface and registers a service for it, and a bundle that imports the interface and
     * gets the service. Then, we update the exporting bundle, and see whether the wiring gets done correctly by making the
     * importing bundle try to use the altered service.
     *
     * Timeouts after 60 seconds, because it freezes in some versions of knopflerfish.
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"4.3.10", "5.9.1"}),
        @OSGiSpec(version="4.2", sections={"4.4.10", "5.9.1"})
    })
    public void testPartitionedBundleUpdate1() throws IOException, BundleException {
        testBundleUpdatePartition(new String[] {}, new String[] {}, null, null);
    }

    /**
     * Timeouts after 60 seconds, because it freezes in some versions of knopflerfish.
     *
     * @see ServiceTest#testPartitionedBundleUpdate1()
     * @throws IOException
     * @throws BundleException
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"4.3.10", "5.9.1"}),
        @OSGiSpec(version="4.2", sections={"4.4.10", "5.9.1"})
    })
    public void testPartitionedBundleUpdate2() throws IOException, BundleException {
        testBundleUpdatePartition(new String[] { "1" }, new String[] { "2" }, null, null);
    }

    /**
     * Timeouts after 60 seconds, because it freezes in some versions of knopflerfish.
     *
     * @see ServiceTest#testPartitionedBundleUpdate1()
     * @throws IOException
     * @throws BundleException
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"4.3.10", "5.9.1"}),
        @OSGiSpec(version="4.2", sections={"4.4.10", "5.9.1"})
    })
    public void testPartitionedBundleUpdate3() throws IOException, BundleException {
        testBundleUpdatePartition(new String[] { "1" }, new String[] { "2" }, "[1,1]", "[2,2]");
    }

    /**
     * Builds a scenario in which a bundle gets updated while it is in use. It assumes the parameters passed will create a
     * partition after the update, but solvable after the refresh.
     *
     * @param expVersions1 The exported versions of a package for the first instantiation of the provider.
     * @param expVersions2 The exported versions of a package for the update of the provider.
     * @param impVersion1 The imported versions of a package for the first instantiation of the provider.
     * @param impVersion2 The imported versions of a package for the update of the provider.
     */
    public void testBundleUpdatePartition(String[] expVersions1, String[] expVersions2, String impVersion1, String impVersion2) throws IOException, BundleException {
        Bundle b = m_bu.installBundle(m_bu.createBundleSpecifier("b")
            .addImport(m_bu.createImportPackage(fooPackage)));

        BundleSpecifier as1 = m_bu.createBundleSpecifier("a1").pack(FooImpl1).pack(Foo);
        if (expVersions1.length == 0) {
            as1.addExport(m_bu.createExportPackage(fooPackage));
        }
        else {
            for (String s : expVersions1) {
                as1.addExport(m_bu.createExportPackage(fooPackage).setVersion(s));
            }
        }
        if (impVersion1 != null) {
            as1.addImport(m_bu.createImportPackage(fooPackage).setVersion(impVersion1));
        }
        Bundle a1 = m_bu.installBundle(as1);

        a1.start();
        m_bu.registerService(FooImpl1, Foo, a1);

        b.start();

        assert a1.getBundleId() == m_bu.getWiredPackageExporter(fooPackage, b).getBundleId() : "Bundle b should be wired to a1 after both are started.";

        assert m_bu.seesService(Foo, b) : "Bundle b should see a service for Foo from a.";
        assert m_bu.canUseService(Foo, b) : "Bundle b should be able to use a service using Foo.";

        BundleSpecifier as2 = m_bu.createBundleSpecifier("a1").pack(FooImpl2).pack(Foo);
        if (expVersions2.length == 0) {
            as2.addExport(m_bu.createExportPackage(fooPackage));
        }
        else {
            for (String s : expVersions2) {
                as2.addExport(m_bu.createExportPackage(fooPackage).setVersion(s));
            }
        }
        if (impVersion2 != null) {
            as2.addImport(m_bu.createImportPackage(fooPackage).setVersion(impVersion2));
        }
        a1.update(m_bu.generateBundle(as2));
        // a1 gets stopped and started automatically.
        m_bu.registerService(FooImpl2, Foo, a1);

        assert !m_bu.seesService(Foo, b) : "Bundle b should no longer see a service for Foo after update of a.";
        assert a1.getBundleId() == m_bu.getWiredPackageExporter(fooPackage, b).getBundleId() : "Bundle b should still be wired to a1 after update.";

        m_bu.refreshFrameworkAndWait(null);
        //re-register the service after refresh.
        m_bu.registerService(FooImpl2, Foo, a1);

        assert a1.getBundleId() == m_bu.getWiredPackageExporter(fooPackage, b).getBundleId() : "Bundle b should still be wired to a1 after update and refresh.";
        assert m_bu.seesService(Foo, b) : "Bundle b should see a service using Foo after refresh.";
        assert m_bu.canUseService(Foo, b) : "Bundle b should be still able to use a service using Foo after refresh.";

    }

    /**
     * Uses testBundleUpdateNoPartition to check a scenario of updating.
     *
     * Timeouts after 60 seconds, because it freezes in some versions of knopflerfish.
     *
     * @throws IOException
     * @throws BundleException
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"4.3.10", "5.9.1"}),
        @OSGiSpec(version="4.2", sections={"4.4.10", "5.9.1"})
    })
    public void testUnPartitionedBundleUpdate1() throws IOException, BundleException {
        testBundleUpdateNoPartition(new String[] {}, new String[] {}, "", "");
    }

    /**
     * Uses testBundleUpdateNoPartition to check a scenario of updating.
     *
     * Timeouts after 60 seconds, because it freezes in some versions of knopflerfish.
     *
     * @throws IOException
     * @throws BundleException
     */
    @Test(timeout=60000)
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"4.3.10", "5.9.1"}),
        @OSGiSpec(version="4.2", sections={"4.4.10", "5.9.1"})
    })
    public void testUnPartitionedBundleUpdate3() throws IOException, BundleException {
        testBundleUpdateNoPartition(new String[] { "1" }, new String[] { "2" }, "[1,1]", "[1,2]");
    }

    /**
     * Builds a scenario in which a bundle gets updated while it is in use. It assumes the parameters passed will NOT create a
     * partition after the update.
     *
     * @param expVersions1 The exported versions of a package for the first instantiation of the provider.
     * @param expVersions2 The exported versions of a package for the update of the provider.
     * @param impVersion1 The imported versions of a package for the first instantiation of the provider.
     * @param impVersion2 The imported versions of a package for the update of the provider.
     */
    public void testBundleUpdateNoPartition(String[] expVersions1, String[] expVersions2, String impVersion1, String impVersion2) throws IOException, BundleException {

        Bundle b = m_bu.installImpExBundle("b", Foo, null, null, "");
        Bundle a1 = m_bu.installImpExBundle("a1", Foo, FooImpl1, expVersions1, impVersion1);

        a1.start();
        assert m_bu.registerService(FooImpl1, Foo, a1) : "a should be able to register FooImpl1.";

        b.start();

        assert a1.getBundleId() == m_bu.getWiredPackageExporter(fooPackage, b).getBundleId() : "Bundle b should be wired to a1 after both are started.";

        assert m_bu.seesService(Foo, b) : "Bundle b should see a service for Foo from a.";
        assert m_bu.canUseService(Foo, b) : "Bundle b should be able to use a service using Foo.";

        a1.update(m_bu.generateImpExBundle("a1", Foo, FooImpl2, expVersions2, impVersion2));
        // a1 gets stopped and started automatically.
        assert !m_bu.registerService(FooImpl2, Foo, a1) : "After update, a should not be able to register a FooImpl2 because the first version is still around.";
        assert m_bu.registerService(FooImpl1, Foo, a1) : "Even after update, a should be able to register a FooImpl1 because the first version is still around.";

        assert m_bu.seesService(Foo, b) : "Bundle b should still see a service for Foo after update of a.";
        assert m_bu.canUseService(Foo, b) : "Bundle b should still be able to use a service using Foo after update of a.";
        assert a1.getBundleId() == m_bu.getWiredPackageExporter(fooPackage, b).getBundleId() : "Bundle b should still be wired to a1 after update.";

        m_bu.refreshFrameworkAndWait(null);
        assert !m_bu.registerService(FooImpl1, Foo, a1) : "After refresh, a should not be able to register a FooImpl1 because the first version is no longer around.";
        assert m_bu.registerService(FooImpl2, Foo, a1) : "After refresh, a should be able to register a FooImpl2 because the second version is around now.";

        assert a1.getBundleId() == m_bu.getWiredPackageExporter(fooPackage, b).getBundleId() : "Bundle b should still be wired to a1 after update and refresh.";
        assert m_bu.seesService(Foo, b) : "Bundle b should see a service using Foo after refresh.";
        assert m_bu.canUseService(Foo, b) : "Bundle b should be still able to use a service using Foo after refresh.";
    }

}
