package org.apache.maven.surefire.api.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

import org.apache.maven.surefire.api.testset.RunOrderParameters;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class RunOrderCalculatorTest
    extends TestCase
{

    public void testOrderTestClasses()
    {
        getClassesToRun();
        TestsToRun testsToRun = new TestsToRun( getClassesToRun() );
        RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( RunOrderParameters.alphabetical(), 1 );
        final TestsToRun testsToRun1 = runOrderCalculator.orderTestClasses( testsToRun );
        assertEquals( A.class, testsToRun1.iterator().next() );
    }

    private Set<Class<?>> getClassesToRun()
    {
        Set<Class<?>> classesToRun = new LinkedHashSet<>();
        classesToRun.add( B.class );
        classesToRun.add( A.class );
        return classesToRun;
    }

    static class A
    {

    }

    static class B
    {

    }

    public void reverseAlphabeticalRunOrderTestClasses()
    {
        getClassesToRun();
        TestsToRun testsToRun = new TestsToRun( getClassesToRun() );
        RunOrderParameters runOrderParameters = new RunOrderParameters( "reversealphabetical" , null );
        RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        final TestsToRun testsToRun1 = runOrderCalculator.orderTestClasses( testsToRun );
        assertEquals( B.class, testsToRun1.iterator().next() );
    }

    public void reverseAlphabeticalRunOrderTestMethods()
    {
        RunOrderParameters runOrderParameters = new RunOrderParameters( "reversealphabetical" , null );
        RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        System.setProperty( "test", "org.apache.maven.surefire.api.util.RunOrderCalculatorTest$B#C,org.apache.maven.surefire.api.util.RunOrderCalculatorTest$B#D,org.apache.maven.surefire.api.util.RunOrderCalculatorTest$A#A,org.apache.maven.surefire.api.util.RunOrderCalculatorTest$A#B" );
        Comparator<String> reverseAlphabeticalRunOrderComparator = runOrderCalculator.comparatorForTestMethods();
        List<String> list = new ArrayList<String>();
        list.add( "org.apache.maven.surefire.api.util.RunOrderCalculatorTest$B#A" );
        list.add( "org.apache.maven.surefire.api.util.RunOrderCalculatorTest$B#B" );
        list.add( "org.apache.maven.surefire.api.util.RunOrderCalculatorTest$A#C" );
        list.add( "org.apache.maven.surefire.api.util.RunOrderCalculatorTest$A#D" );
        list.sort( reverseAlphabeticalRunOrderComparator );
        assertEquals( list.get(0), "org.apache.maven.surefire.api.util.RunOrderCalculatorTest$B#B" );
    }

    public void shouldThrowExceptionForNullTestMethod()
    {
        RunOrderParameters runOrderParameters = new RunOrderParameters( "reversealphabetical" , null );
        RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        System.clearProperty( "test" );
        try
        {
            runOrderCalculator.comparatorForTestMethods();
        }
        catch ( IllegalStateException expected )
        {
            assertEquals( expected.getMessage(), "Please set system property -Dtest to use fixed order" );
        }
    }
}
