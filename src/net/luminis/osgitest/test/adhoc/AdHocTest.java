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
package net.luminis.osgitest.test.adhoc;

import java.io.IOException;

import net.luminis.osgitest.testhelper.BundleSpecifier;
import net.luminis.osgitest.testhelper.OSGiSpec;
import net.luminis.osgitest.testhelper.OSGiVersionSpecs;
import net.luminis.osgitest.testhelper.TestBase;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.ExportedPackage;

/**
 * Empty framework for ad-hoc tests
 */
public class AdHocTest extends TestBase {


    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.5.5"}),
        @OSGiSpec(version="4.2", sections={"3.5.5"})
    })
    public void testGetExportedPackagesByBundle() throws BundleException {
        int nrOfRuns = 50;
        int maxNrOfPackages = 4;
        boolean allOk = true;
        StringBuilder results = new StringBuilder();
        for (int j = 1; j <= maxNrOfPackages; j++) {
            for (int i = 0; i < nrOfRuns; i++) {
                Bundle b = generateMultiplePackageExporter("multi"+i+(j+1), j);
                m_admin.resolveBundles(new Bundle[] { b });
                ExportedPackage[] exportedPackages = m_admin.getExportedPackages(b);
                results.append("Run " + i + ", expected " + j + " packages, saw " + exportedPackages.length + ((exportedPackages.length != j*j)?"<--":"")+ "\n");
                if (exportedPackages.length != j) {
                    allOk = false;
                }
                b.uninstall();
                m_bu.refreshFrameworkAndWait(new Bundle[] {b});
            }
        }

        assert allOk : "Something went wrong creating multi-export packages; run output below.\n" + results.toString();
    }

    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.5.5"}),
        @OSGiSpec(version="4.2", sections={"3.5.5"})
    })
    public void testGetExportedPackagesByPackage() throws BundleException {
        int nrOfRuns = 50;
        int maxNrOfPackages = 4;
        boolean allOk = true;
        StringBuilder results = new StringBuilder();
        for (int j = 1; j <= maxNrOfPackages; j++) {
            for (int i = 0; i < nrOfRuns; i++) {
                Bundle b = generateMultiplePackageExporter("multi"+i+(j+1), j);
                m_admin.resolveBundles(new Bundle[] { b });
                ExportedPackage[] exportedPackages = m_admin.getExportedPackages(rPackage.getName());
                results.append("Run " + i + ", expected " + j + " packages, saw " + exportedPackages.length + ((exportedPackages.length != j*j)?"<--":"")+ "\n");
                for (ExportedPackage exportedPackage : exportedPackages) {
                    results.append("Package: "+ exportedPackage.getName() + ":" + exportedPackage.getVersion() + "\n");
                }
                results.append("\n");
                if (exportedPackages.length != j) {
                    allOk = false;
                }
                b.uninstall();
                m_bu.refreshFrameworkAndWait(new Bundle[] {b});
            }
        }
        assert allOk : "Something went wrong creating multi-export packages; run output below.\n" + results.toString();
    }

    /**
     * Installs and returns a bundle with nrOfExports + 1 exports:
     * One is always installed, nrOfExports gives the number of versions
     * of a second package. TODO this is not true.
     * @param name The name of the bundle.
     * @param nrOfExports The number of exported versions of a package.
     * @return
     */
    public Bundle generateMultiplePackageExporter(String name, int nrOfExports) {
        BundleSpecifier bs = m_bu.createBundleSpecifier(name);
        for (int i = 1; i <= nrOfExports; i++) {
            bs.addExport(m_bu.createExportPackage(rPackage).setVersion("3."+i));
        }
        Bundle b = null;
        try {
            b = m_bu.installBundle(bs);
        }
        catch (BundleException e) {
            // Safely suppressed.
        }
        catch (IOException e) {
            // Safely suppressed.
        }
        return b;
    }
}
