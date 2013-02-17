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

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import net.luminis.osgitest.testhelper.OSGiSpec;
import net.luminis.osgitest.testhelper.OSGiVersionSpecs;
import net.luminis.osgitest.testhelper.TestBase;

import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Tests the framework's filter implementation, focusing on the syntax checking.
 */
public class FrameworkFilterSyntaxTest extends TestBase {
    /*
     * Some setup
     */
    private final Dictionary dict = new Hashtable();


    /*
     * Actual tests
     */
    /**
     * This test should actually be part of the Dictionary testing.
     */
    @SuppressWarnings("unchecked")
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.6"}),
        @OSGiSpec(version="4.2", sections={"3.2.7"})
    })
    public void testNonExistingAttribute() throws InvalidSyntaxException {
        assert !m_context.createFilter("(cn=whatever)").match(new Hashtable()) : "Nonexistent attributes should be allowed.";
    }

    /**
     * Tests for the outer parentheses, which should always be present.
     * @throws InvalidSyntaxException
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.6"}),
        @OSGiSpec(version="4.2", sections={"3.2.7"})
    })
    public void testOmittedParentheses() throws InvalidSyntaxException {
        try {
            m_context.createFilter("a=a").match(dict);
            assert false : "Non-parenthesized filters should not be allowed.";
        }
        catch (InvalidSyntaxException e) {
        }
        try {
            m_context.createFilter("((a=a))").match(dict);
            assert false : "Doubly-parenthesized filters should not be allowed.";
        }
        catch (InvalidSyntaxException e) {
        }
    }


    /**
     * Tests all combinations of special characters in attribute names, whether in front,
     * in the middle or at the end of the attribute name, and combined with various filtertypes.
     * Marked broken because (!cn=a) is marked as invalid, but should be valid according to RFC-1960
     * @throws InvalidSyntaxException
     */
    @Test
    @Ignore("(!cn=a) is marked as invalid, but should be valid according to RFC-1960")
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.6"}),
        @OSGiSpec(version="4.2", sections={"3.2.7"})
    })
    public void testSpecialCharactersInAttributes() throws InvalidSyntaxException {
        // Test the six illegal characters
        // '=' is a special case: handled separately.
        for (char c : new char[] {'<', '>', '~', '(', ')'}) {
            for (int n : new int[] {1, 2, 3}) {
                for (String o : new String[] {"~=", "<=", ">="}) {
                    testSpecialCharacterInAttribute(c, n, o, false);
                }
            }
        }

        // Test some special characters
        // _ has charactercode 95, so it is inbetween 'A' and 'z'
        for (char c : new char[] {'_', '1', '!', '*',}) {
            for (int n : new int[] {1,2,3}) {
                for (String o : new String[] {"~=", "<=", ">="}) {
                    testSpecialCharacterInAttribute(c, n, o, true);
                }
            }
        }
    }

    /**
     * Test the legality of a character at a given position in an attribute name.
     * @param character The character to be inserted.
     * @param position The position to put it in: first, middle, or last.
     * @param filtertype The filtertype to be used in the filter, e.g. = or <=
     * @param allowed Whether or not this combination should be allowed.
     */
    private void testSpecialCharacterInAttribute(char character, int position, String filtertype, boolean allowed) {
        String attribute = "";
        String posName = "";
        switch (position) {
            case 1: attribute = character + "cn"; posName = "first"; break;
            case 2: attribute = "c" + character + "n"; posName = "middle"; break;
            case 3: attribute = "cn" + character; posName = "last"; break;
        }

        String filter = "(" + attribute + filtertype+"a)";
        try {
            m_context.createFilter(filter).match(dict);
            assert allowed : "Character '" + character + "' should not be allowable in "+posName+" position in attributes with operator "+filtertype+". Filter = " + filter + ".";
        }
        catch (InvalidSyntaxException e) {
            assert !allowed : "Character '" + character + "' should be allowable in "+posName+" position in attributes with operator "+filtertype+". Filter = " + filter + ". Exception = " + e.getMessage();
        }
    }


    /**
     * Tests the special characters in values.
     * @throws InvalidSyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.6"}),
        @OSGiSpec(version="4.2", sections={"3.2.7"})
    })
    public void testSpecialCharactersInValue() throws InvalidSyntaxException {
        // We use a specific dictionary here.
        Dictionary dict = new Hashtable();
        dict.put("a", "*");
        dict.put("b", "\\");
        dict.put("c", "(");
        dict.put("d", ")");

        // There should be escape characters if we want to find special characters.
        assert m_context.createFilter("(a=\\*)").match(dict) : "a=* is in the dictionary.";
        assert m_context.createFilter("(b=\\\\)").match(dict) : "b=\\ is in the dictionary.";
        assert !m_context.createFilter("(b=\\))").match(dict) : "b=\\ should not match: \\ needs to be escaped.";
        assert m_context.createFilter("(c=\\()").match(dict) : "c=( is in the dictionary.";
        try {
            m_context.createFilter("(c=()").match(dict);
            assert false : "(c=() is illegal syntax: ( needs to be escaped";
        }
        catch (InvalidSyntaxException e) {
        }
        assert m_context.createFilter("(d=\\))").match(dict) : "d=) is in the dictionary.";
        try {
            m_context.createFilter("(d=))").match(dict);
            assert false : "(d=)) is illegal syntax: ) needs to be escaped";
        }
        catch (InvalidSyntaxException e) {
        }
    }

    /**
     * Tests the handling of the equals sign in an expression.
     * @throws InvalidSyntaxException
     */
    @SuppressWarnings("unchecked")
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.6"}),
        @OSGiSpec(version="4.2", sections={"3.2.7"})
    })
    public void testEqualsHandling() throws InvalidSyntaxException {
        // We use a specific dictionary here.
        Dictionary dict = new Hashtable();
        dict.put("a", "=a");
        dict.put("b", "b=a");

        // First, try the different syntax variants.
        testFilterSyntax("(a==a)", true, "(a==a) is legal syntax.");
        testFilterSyntax("(b=a=a)", true, "(b=a=a) is legal syntax.");
        testFilterSyntax("(a=a)", true, "(a=a) is legal syntax.");

        // Some special cases, where confusion might arise in the location of the =.
        testFilterSyntax("(a<==a)", true, "(a<==a) is legal syntax.");
        testFilterSyntax("(a=<=a)", true, "(a=<=a) is legal syntax.");
        testFilterSyntax("(a==<a)", true, "(a==<a) is legal syntax.");
        testFilterSyntax("(<a=a)", false, "(<a=a) is illegal syntax.");
        testFilterSyntax("(a<=a=a)", true, "(a<=a=a) is legal syntax.");
        testFilterSyntax("(a=a<=a)", true, "(a=a<=a) is legal syntax.");

        // Now, test the allowable ones to see 'what they do'.
        // The first = should be used as the delimiter between the key and the value.
        assert m_context.createFilter("(a==a)").match(dict) : "a = =a is in the dictionary.";
        assert m_context.createFilter("(b=b=a)").match(dict) : "a = b=a is in the dictionary.";
        assert !m_context.createFilter("(a=a)").match(dict) : "a = a is not in the dictionary.";
    }

    /**
     * Tests the handling of leading, trailing and embedded spaces.
     * @throws InvalidSyntaxException
     */
    @Test
    @Ignore("Ignore, because of long running time")
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.6"}),
        @OSGiSpec(version="4.2", sections={"3.2.7"})
    })
    public void testWhitespaceHandling() throws InvalidSyntaxException {

        testWhitespaceHandlingHelper("\t\t", "", "\t\t");

        String[] whiteSpace = {"", "\t" /*tab*/, "\n" /*linefeed*/, "\u000B" /*vertical tab*/,
            "\f" /*formfeed*/, "\r" /*carriage return*/, "\u001C" /*file separator*/,
            "\u001D" /*group separator*/, "\u001E" /*record separator*/, "\u001F" /*unit separator*//*, "\b" backspace*/};
        //Create combinations of up to two whitespace characters for each position.
        long startTime = System.currentTimeMillis();
        long counter = 0;
        for (String leading1 : whiteSpace) {
            for (String leading2 : whiteSpace) {
                for (String embedded1 : whiteSpace) {
                    for (String embedded2 : whiteSpace) {
                        for (String trailing1 : whiteSpace) {
                            for (String trailing2 : whiteSpace) {
                                testWhitespaceHandlingHelper(leading1+leading2, embedded1+embedded2, trailing1+trailing2);
                                if ((counter++ % 10000) == 0) {
                                    System.err.println(counter/10000+"%");
                                }
                            }
                        }
                    }
                }
            }
        }
        System.err.println("Ran 1 000 000 filters for whitespace handling in " + (System.currentTimeMillis() - startTime) + " ms.");
    }

    /**
     * Helper function that tests whether the whitespace conventions are honored
     * for a given combination of leading, embedded and trailing whitespace characters.
     * @throws InvalidSyntaxException
     */
    @SuppressWarnings("unchecked")
    private void testWhitespaceHandlingHelper(String leading, String embedded, String trailing) throws InvalidSyntaxException {
        // We use a specific dictionary here.
        Dictionary dict = new Hashtable();
        dict.put("a"+embedded+"b", "a");

        String filter = "(" + leading + "a" + embedded + "b" + trailing + "=a)";
        String filterWithoutEmbedded =  "(" + leading + "ab" + trailing + "=a)";

        // Create a human readable representation of what we got, and make sure all
        // characters received are actually whitespace.
        StringBuffer whatDidWeGet = new StringBuffer();
        boolean allValidWhitespace = true;
        whatDidWeGet.append("(leading: ");
        for (char c : leading.toCharArray()) {
            whatDidWeGet.append("character " + (int)c + ", ");
            if (!Character.isWhitespace(c)) {
                allValidWhitespace = false;
            }
        }
        whatDidWeGet.append(" embedded: ");
        for (char c : embedded.toCharArray()) {
            whatDidWeGet.append("character " + (int)c + ", ");
            if (!Character.isWhitespace(c)) {
                allValidWhitespace = false;
            }
        }
        whatDidWeGet.append(" trailing: ");
        for (char c : trailing.toCharArray()) {
            whatDidWeGet.append("character " + (int)c + ", ");
            if (!Character.isWhitespace(c)) {
                allValidWhitespace = false;
            }
        }
        whatDidWeGet.append(")");

        // First test whether the filter is syntactically correct or not, and whether is judged as such.
        try {
            m_context.createFilter(filter).match(dict);
            assert allValidWhitespace : "Whitespace placeholders contain non-whitespace characters, but is allowed. " + whatDidWeGet;
        }
        catch (InvalidSyntaxException e) {
            assert !allValidWhitespace : "Whitespace placeholders contain all valid whitespace characters, but is not allowed. " + whatDidWeGet;
        }

        // If the filter does not contain illegal whitespaces, check whether the dictionary handles it correctly.
        if (allValidWhitespace) {
            assert m_context.createFilter(filter).match(dict) : "Embedded whitespace should be honored, leading and trailing ignored. " + whatDidWeGet;

            if (embedded.length() > 0) {
                assert !m_context.createFilter(filterWithoutEmbedded).match(dict) : "Embedded whitespace is significant." + whatDidWeGet;
            }
        }
    }

    /**
     * Tests many allowable filters with some complexity and whitespace present; use
     * for performance testing. Running time on my machine: between four and five minutes.
     */
    @Test
    @Ignore("Ignore, because of long running time")
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.6"}),
        @OSGiSpec(version="4.2", sections={"3.2.7"})
    })
    public void testALotPerformance() throws InvalidSyntaxException {
        testALot(1, 2, 1, -2);
    }

    /**
     * Tests many allowable filters with low complexity and no whitespace; use
     * for integration testing.
     */
    @Test
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.6"}),
        @OSGiSpec(version="4.2", sections={"3.2.7"})
    })
    public void testALotIntegration() throws InvalidSyntaxException {
        testALot(2, 2, 2, 1000);
    }

    /**
     * Creates an awful load of complex, though legal, filter using the full
     * syntax, and calculates the running time.
     *
     * @throws InvalidSyntaxException
     * @param attributeLength The maximum length of a single attribute, including
     * whitespace characters.
     * @param attributeLength The maximum length of a single value, including
     * whitespace characters.
     * @param filterLength The maximum number of clauses in a single filter.
     */
    private void testALot(int attributeLength, int valueLength, int filterLength, int maxFilterCount) throws InvalidSyntaxException {
        // Generate a load of legal, single-operation operations, possibly including special characters.
        Set<String> params = new HashSet<String>(Arrays.asList(new String[] {"a","b"," ", "\t", "\n"}));
        Set<String> filtertypes = new HashSet<String>(Arrays.asList(new String[] {"=", "~=", "<=", ">="}));
        Set<String> allOperations = generateOperations(filtertypes, params, attributeLength, params, valueLength);

        // Test all possible combinations of filters using these characters, using timing.
        long startTime = System.currentTimeMillis();
        Counter counter = new Counter(maxFilterCount/2);
        testComplexFilters(allOperations, filterLength, "", "", counter);
        long endTime = System.currentTimeMillis();
        System.err.printf("testALot(%d, %d, %d, %d): Parsed %d filters in %d ms.\n", attributeLength, valueLength, filterLength, maxFilterCount, (counter.start < 0 ? -1 * counter.counter : counter.start*2), endTime - startTime);
    }


    /**
     * Helper function to test whether or not a certain filter is allowed.
     * @param f The filter to be used.
     * @param allowed Whether or not the filter in f should be allowed.
     * @param error An error string to show when things do not go as we wish.
     * @throws InvalidSyntaxException
     */
    private void testFilterSyntax(String f, boolean allowed, String error) throws InvalidSyntaxException {
        try {
            m_context.createFilter(f).match(dict);
            assert allowed : "Filter '" + f + "': " + error;
        }
        catch (InvalidSyntaxException e) {
            assert !allowed : "Filter '" + f + "': " + error + " (" + e.getMessage() + ")";
        }
    }

     /**
     * Some misc tests, not covered elsewhere.
     * Marked broken because a=** is allowed by current implementation, but specification is inconclusive
     *
     * @throws InvalidSyntaxException
     */
    @Test
    @Ignore("Ignore, because a=** is allowed by current implementation, but specification is inconclusive")
    @OSGiVersionSpecs({
        @OSGiSpec(version="4.1", sections={"3.2.6"}),
        @OSGiSpec(version="4.2", sections={"3.2.7"})
    })
    public void testMiscellaneous() throws InvalidSyntaxException {
        // A simple test, intended to make sure the following tests actually do what we intend to.
        testFilterSyntax("(a=b)", true, "(a=b) is a correct filter.");

        // Test handling of !, | and &
        testOperatorHandling('!', true);
        testOperatorHandling('|', false);
        testOperatorHandling('&', false);

        // Some constructs with substrings
        testFilterSyntax("(a=**)", false, "Construct ** should not be allowed.");
        testFilterSyntax("(a=b**)", false, "Construct b** should not be allowed.");
        testFilterSyntax("(a=*b*)", true, "Construct *b* should be allowed.");
        testFilterSyntax("(a=b*b)", true, "Construct b*b should be allowed.");

        // A construct with presence
        testFilterSyntax("(a=*)", true, "Construct =* should be allowed.");

        // Some more-or-less random, combined filters.
        testFilterSyntax("|(&(a=b))", false, "We need the outer parentheses.");
        testFilterSyntax("(|(&(a=b)))", true, "Is legal syntax.");
        testFilterSyntax("(|(&(a=b)(c=d)))", true, "Is legal syntax.");
        testFilterSyntax("(|(&(a=*)(c<=d))(!(c~=d)))", true, "Is legal syntax.");
    }

    /**
     * Helper function for testMiscellaneous
     * @param operator
     * @param isUnary
     * @throws InvalidSyntaxException
     */
    private void testOperatorHandling(char operator, boolean isUnary) throws InvalidSyntaxException {
        testFilterSyntax(operator + "(a=b)", false, operator + "(a=b) is not allowed, since filters need to be parenthesized.");
        testFilterSyntax("(" + operator + "(a=b))", true, "(" + operator + "(a=b)) is allowed, since " + operator + " allows a single filter.");
        testFilterSyntax("(" + operator + "(a=b)(b=c))", !isUnary, "(" + operator + "(a=b)(b=c)) is " + (isUnary ? "not" : "") + " allowed, since " + operator + " is " + (isUnary ? "" : "not") + " unary.");
    }

    /**
     * Returns a string array of all possible (legal) operations ((a=b)-like stuff)
     * using the operators, and using all possible combinations of up to two special characters.
     * It will automatically use * characters in the value at legal locations.
     * @param filtertypes
     * @param charsForAttribute
     * @param charsForValue
     * @return
     */
    private Set<String> generateOperations(Collection<String> filtertypes, Collection<String> charsForAttribute, int attLength, Collection<String> charsForValue, int valLength) {
        Set<String> allOperations = new HashSet<String>();
        Set<String> allValuesWithSubstring = createPermutations(charsForValue, valLength, true);
        Set<String> allValuesWithoutSubstring = createPermutations(charsForValue, valLength, false);
        for (String att : createPermutations(charsForAttribute, attLength, false)) {
            for (String fil : filtertypes) {
                if (fil.equals("=")) {
                    //Substrings are only allowed using '='
                    for (String val : allValuesWithSubstring) {
                        allOperations.add("("+att+fil+val+")");
                    }
                }
                else if (fil.equals("=*")) {
                    allOperations.add("("+att+fil);
                }
                else {
                    for (String val : allValuesWithoutSubstring) {
                        allOperations.add("("+att+fil+val+")");
                    }
                }
            }
        }

        return allOperations;
    }

    /**
     * Creates all possible permutations of input, up to a length of maxLength. useSmartStar will
     * insert a start at all allowable locations (so don't use * in input).
     */
    private Set<String> createPermutations(Collection<String> charsForValue, int maxLength, boolean useSmartStar) {
        Set<String> allPermutations = new HashSet<String>();
        if (maxLength == 1) {
            for (String s : charsForValue) {
                allPermutations.add(s);
                if (useSmartStar) {
                    allPermutations.add(s+"*");
                    allPermutations.add("*"+s);
                    allPermutations.add("*"+s+"*");
                }
            }
        }
        else
        {
            Set<String> theRest = createPermutations(charsForValue, maxLength - 1, useSmartStar);
            for (String s : charsForValue) {
                for (String t : theRest) {
                    allPermutations.add(s + t);
                    if (useSmartStar) {
                        allPermutations.add("*" + s + t);
                    }
                }
            }
            allPermutations.addAll(theRest);
        }

        //Remove stuff only consisting of whitespace
        Set <String> toRemove = new HashSet<String>();
        for (String s : allPermutations) {
            boolean onlyWhitespace = true;
            for (char c : s.toCharArray()) {
                if (!Character.isWhitespace(c)) {
                    onlyWhitespace = false;
                }
            }
            if ( onlyWhitespace ) {
                toRemove.add(s);
            }
        }
        allPermutations.removeAll(toRemove);

        return allPermutations;
    }

    /**
     * Tests all filter combinations based on the operation passed, up to a given nr. of clauses.
     * Note that the actually created clauses can be longer, because filters are used to construct
     * new filters.
     * pre and post are used internally.
     * @param maxCount TODO
     */
    private void testComplexFilters(Set<String> operations, int lengthToGo, String pre, String post, Counter counter) throws InvalidSyntaxException {
        if (counter.counter == 0) {
            return;
        }
        if (lengthToGo == 0) {
            for (String filter : operations)
            {
                if (counter.counter == 0) {
                    return;
                }
                counter.counter--;
                testFilterSyntax(pre + filter + post, true, "Is valid syntax, but creates an error.");
                testFilterSyntax("(!" + pre + filter + post + ")", true, "Is valid syntax, but creates an error.");
            }
        }
        else {
            for (String operation : operations) {
                testComplexFilters(operations, lengthToGo - 1, pre + "(&" + operation, ")" + post, counter);
                testComplexFilters(operations, lengthToGo - 1, pre + "(|" + operation, ")" + post, counter);
                testComplexFilters(operations, 0, pre, post, counter);
            }
        }
    }

    private static class Counter {
        long counter;
        long start;

        public Counter(long initial) {
            start = counter = initial;
        }
    }
}
