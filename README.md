# OSGi specification tests

This projects contains a tests for testing the specification conformance of multiple OSGi framework implementations.
The tests are executed using the [OSGi specification testframework](https://github.com/wtreur/osgi-specification-test-framework)
(which is included as binary)

# Usage

## Execute framework tests

`ant test`

Runs the JUnit tests in the Test Framework.
This target needs the following parameters that can be passed via the commandline with the `-Dname` option.

- `osgi-vendors`<br />
  Comma separated list of OSGi frameworks to test. Only the supported
  vendors by Pax Exam can be used. Use `all` for all available frameworks
  Use `latest-build` as version to test the latest available (nightly) build.
  eg: `felix/latest-build` (Only supported for felix and knopflerfish)

### example:

    ant test -Dosgi-vendors=felix/2.0.0,knopflerfish/latest-build,equinox/2.5.0


## Create test report

`ant create-testreport`

This will create a nice matrix comparing specification compliance of different part in the specification
for each tested framework

## Export testresults

`ant export-testreport`

Transfers the results to a remote svn repository<br />
This target needs the following parameters that can be passed via the commandline with the `-Dname` option.
- `svn-trunk`<br />
  URI of the subversion trunk
- `svn-username`<br />
  Username used to access the svn trunk
- `svn-password`<br />
  Password used to access the svn trunk

### Example:

    ant clean, test, export-testresults
        -Dosgi-vendors=felix/2.0.0,equinox/2.5.0
        -Dsvn-trunk=http://svn.acme.com/test-results/trunk/
        -Dsvn-username=john
        -Dsvn-password=doe
