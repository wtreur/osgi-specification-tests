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
package net.luminis.osgitest.test.framework.util.ldap;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import net.luminis.osgitest.testhelper.OSGiSpec;
import net.luminis.osgitest.testhelper.OSGiVersionSpecs;
import net.luminis.osgitest.testhelper.TestBase;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Tests the framework's filter implementation. Focus on comparison types.
 *
 */
public class FrameworkFilterDictionaryTest extends TestBase {

    private Dictionary<String, Object> dict = null;

    @Before
    public void setupDictionary() {
        dict = new Hashtable<String, Object>();
    }

    /**
     * Test String array in a dictionary
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.6"}),
        @OSGiSpec(version="4.2", sections={"3.2.7"})
    })
    public void testStringContent() throws InvalidSyntaxException{

        dict.put("cn", new String[] {"a", "b", "c"});

        assert m_context.createFilter("(cn=*)").match(dict) : "cn should be in dict";
        assert !m_context.createFilter("(cc=*)").match(dict) : "cc should not be in dict";
        assert m_context.createFilter("(cn=a)").match(dict) : "cn=a should be in dict";
        assert m_context.createFilter("(&(cn=a)(cn=b)(cn=c))").match(dict) : "&(cn=a)(cn=b)(cn=c) should be in dict";
        assert !m_context.createFilter("(&(cn=a)(cn=b)(cn=c)(cn=d))").match(dict) : "(cn=d) is not in dict";
        assert m_context.createFilter("(&(cn=a)(cn=b)(cn=c)(!(cn=d)))").match(dict) : "(cn=d) is not in dict";

    }

    /**
     * Test Integer Array
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"5.5", "6.1.6", "6.1.6.15", "6.1.13"}),
        @OSGiSpec(version="4.2", sections={"5.5", "6.1.6", "6.1.6.5", "6.1.14"})
    })
    public void testIntegercontent() throws InvalidSyntaxException{

        dict.put("cn", new Integer [] {1,2});

        assert m_context.createFilter("(cn=1)").match(dict) : "cn should be 1";
        assert m_context.createFilter("(cn>=1)").match(dict) : "cn should be 2";
        assert m_context.createFilter("(&(cn=1)(cn=2))").match(dict) : "cn should be 1 or 2";
    }

    /**
     * Test match against an empty Dictionary
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"5.5", "6.1.6", "6.1.6.15", "6.1.13"}),
        @OSGiSpec(version="4.2", sections={"5.5", "6.1.6", "6.1.6.5", "6.1.14"})
    })
    public void testEmptyDictionary() throws InvalidSyntaxException{

        assert dict.isEmpty() : "Dictionary should be empty.";
        assert !m_context.createFilter("(cn=*)").match(dict) : "Should be empty..";
    }

    /**
     * Test Boolean scalar and array
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"5.5", "6.1.6", "6.1.6.15", "6.1.13"}),
        @OSGiSpec(version="4.2", sections={"5.5", "6.1.6", "6.1.6.5", "6.1.14"})
    })
    public void testBooleanDictionary() throws InvalidSyntaxException{

        dict.put("T", new Boolean[] { true } );
        dict.put("F", new Boolean(false) );

        assert !dict.isEmpty() : "Dictionary should't be empty.";
        assert m_context.createFilter("(T=true)").match(dict) : "Should be true..";
        assert m_context.createFilter("(F=false)").match(dict) : "Should be false..";
    }

    /**
     * Test Test Numerical comparison case 1: throws an Evaluation exception
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"5.5", "6.1.6", "6.1.6.15", "6.1.13"}),
        @OSGiSpec(version="4.2", sections={"5.5", "6.1.6", "6.1.6.5", "6.1.14"})
    })
    public void testNumericalComparison() throws InvalidSyntaxException{

        dict.put("cn", new Integer(2));

        assert !dict.isEmpty() : "Dictionary should't be empty.";
        try {
            assert !m_context.createFilter("(|(cn=bla)(cn=2))").match(dict) : "The value cn=2 is matched first, filter should stop matching.";
        }
        catch (Exception e) {
            assert e != null : "Exception should be thrown.";
        }
    }


    /**
     * Test Numerical comparison case 2: different order, filter should not match the the second filter-comp
     *
     * TODO: Marked as broken. Looks like something with lazy filtering, but not sure what specification dictates.
     *
     * @throws InvalidSyntaxException
     */
    @Test
    @Ignore("Broken, Looks like something with lazy filtering, but not sure what specification dictates.")
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"5.5", "6.1.6", "6.1.6.15", "6.1.13"}),
        @OSGiSpec(version="4.2", sections={"5.5", "6.1.6", "6.1.6.5", "6.1.14"})
    })
    public void testEvaluationException() throws InvalidSyntaxException{

        dict.put("cn", new Integer(2));

        assert !dict.isEmpty() : "Dictionary should't be empty.";
        assert m_context.createFilter("(|(cn=2)(cn=bla))").match(dict) : "The value cn=2 is matched first, would be nice ";
    }


    /**
     * Test Dictionary with different compared scalars
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"5.5", "6.1.6", "6.1.6.15", "6.1.13"}),
        @OSGiSpec(version="4.2", sections={"5.5", "6.1.6", "6.1.6.5", "6.1.14"})
    })
    public void testStringComparedDictionary() throws InvalidSyntaxException{

        dict.put("F", new Boolean(false) );
        dict.put("string", "ba r");
        dict.put("char", new Character [] {'a', 'b', 'c'});

        assert !dict.isEmpty() : "Dictionary should't be empty.";
        assert m_context.createFilter("(&(F=false)(string=ba r)(char=a))").match(dict) : "Different compared scalars should match";
    }

    /**
     * Test numerical compared dictionary with different numerically compared scalars
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"5.5", "6.1.6", "6.1.6.15", "6.1.13"}),
        @OSGiSpec(version="4.2", sections={"5.5", "6.1.6", "6.1.6.5", "6.1.14"})
    })
    public void testNumericalComparedDictionary() throws InvalidSyntaxException{

        dict.put("integer", new Integer(1));
        dict.put("double", new Double(1234));
        dict.put("float", new Float(123.23));
        dict.put("char", new Character [] {'a', 'b', 'c'});

        assert !dict.isEmpty() : "Dictionary should't be empty.";
        assert m_context.createFilter("(&(integer=1)(double=1234)(float=123.23)(char=a))").match(dict) : "Different numerical scalars should match";
    }


    /**
     * Test a Comparable Object, the String case is ignored.
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"5.5", "6.1.6", "6.1.6.15", "6.1.13"}),
        @OSGiSpec(version="4.2", sections={"5.5", "6.1.6", "6.1.6.5", "6.1.14"})
    })
    public void testComparableClass() throws InvalidSyntaxException{

        dict.put("cn", new FilterTest("hello") );
        assert m_context.createFilter("(&(cn=hello)(cn=HELLO))").match(dict) : "HELLO or hello should match.";

        // No match
        assert !m_context.createFilter("(&(cn=he)(cn=HE))").match(dict) : "'he' should not match" ;


    }

    /**
     * Test other (not comparable) Object.
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"5.5", "6.1.6", "6.1.6.15", "6.1.13"}),
        @OSGiSpec(version="4.2", sections={"5.5", "6.1.6", "6.1.6.5", "6.1.14"})
    })
    public void testNonComparableClass() throws InvalidSyntaxException{

        dict.put("cn", new FilterEqualityTest("hellotoo") );
        assert m_context.createFilter("(&(cn=hellotoo)(cn=HELLOTOO))").match(dict);

        // No match
        assert !m_context.createFilter("(&(cn=he)(cn=HE))").match(dict) : "Nothing should be matched.";

    }

    /**
     * Test a collection (Set) of scalars.
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"5.5", "6.1.6", "6.1.6.15", "6.1.13"}),
        @OSGiSpec(version="4.2", sections={"5.5", "6.1.6", "6.1.6.5", "6.1.14"})
    })
    public void testCollectionSet() throws InvalidSyntaxException{

        HashSet<String> set = new HashSet<String>();
        set.add("hello");
        set.add("bye");

        dict.put("cn", set);

        assert m_context.createFilter("(&(cn=hello)(cn=bye))").match(dict) : "A Set (Collection) of Strings should match.";
    }

    /**
     * Test a collection (Vector) of scalars.
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"5.5", "6.1.6", "6.1.6.15", "6.1.13"}),
        @OSGiSpec(version="4.2", sections={"5.5", "6.1.6", "6.1.6.5", "6.1.14"})
    })
    public void testCollectionVector() throws InvalidSyntaxException{

        // fill a vector with stuff
        Vector<Object> v = new Vector<Object>();
        v.add("hello");
        v.add("bye");
        v.add(null);
        v.add(new Integer(1));

        dict.put("cn", v);

        // put additional stuff in the dictionary
        dict.put("a_string", "blob");

        assert m_context.createFilter("(&(cn=hello)(cn=bye)(a_string=blob))").match(dict) : "A Vector (Collection) of Strings should match.";
    }


    /**
     * helper class for testing Comparable objects
     */
    public static class FilterTest implements Comparable<Object> {

        private final String m_value;

        public FilterTest(String value) {
            m_value = value;
        }

        public int compareTo(Object o) {

            if (o instanceof FilterTest) {

                if (m_value.equalsIgnoreCase(((FilterTest) o).m_value)) {
                    return 0;
                }
             }

            return -1;
        }
    }

    /**
     * helper class for testing Equality objects
     */
    public static class FilterEqualityTest {

        private final String m_value;

        public FilterEqualityTest(String value) {
            m_value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (o instanceof FilterEqualityTest) {
               return m_value.equalsIgnoreCase(((FilterEqualityTest) o).m_value);
            }

            return false;
        }
    }
}
