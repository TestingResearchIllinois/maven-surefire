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
import org.apache.maven.surefire.api.testset.TestListResolver;

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

    static class DubboLazyConnectTest
    {

    }

    public void testOrderTestClasses2()
    {
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null );
        System.setProperty( "test", "DubboLazyConnectTest#a2d,DubboLazyConnectTest#aBc,DubboLazyConnectTest#abc,DubboLazyConnectTest#a1b" );
        DefaultRunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        Comparator<String> testOrderRunOrderComparator = runOrderCalculator.comparatorForTestMethods();
        List<String> list = new ArrayList<String>();
        list.add( "abc(DubboLazyConnectTest)" );
        list.add( "a1b(DubboLazyConnectTest)" );
        list.add( "a2d(DubboLazyConnectTest)" );
        list.add( "aBc(DubboLazyConnectTest)" );
        list.sort( testOrderRunOrderComparator );
        assertEquals( list.get( 0 ), "a2d(DubboLazyConnectTest)" );
    }

    public void testOrderTestClasses3()
    {
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null );
        System.setProperty( "test", "DubboProtocolTest#a2d,DubboLazyConnectTest#aBc,DubboLazyConnectTest#abc,DubboLazyConnectTest#a1b" );
        DefaultRunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        Comparator<String> testOrderRunOrderComparator = runOrderCalculator.comparatorForTestMethods();
        List<String> list = new ArrayList<String>();
        list.add( "abc(DubboLazyConnectTest)" );
        list.add( "a1b(DubboLazyConnectTest)" );
        list.add( "a2d(DubboProtocolTest)" );
        list.add( "aBc(DubboLazyConnectTest)" );
        list.sort( testOrderRunOrderComparator );
        assertEquals( list.get( 0 ), "a2d(DubboProtocolTest)" );
    }

    public void testOrderTestClasses4()
    {
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null );
        System.setProperty( "test", "Dubbo*Test#a?c,My???Test#test*" );
        DefaultRunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        Comparator<String> testOrderRunOrderComparator = runOrderCalculator.comparatorForTestMethods();
        List<String> list = new ArrayList<String>();
        list.add( "abc(DubboLazyConnectTest)" );
        list.add( "testabc(MyabcTest)" );
        list.add( "a2c(DubboProtocolTest)" );
        list.add( "testefg(MyefgTest)" );
        list.add( "aBc(DubboLazyConnectTest)" );
        list.sort( testOrderRunOrderComparator );
        assertEquals( runOrderCalculator.getClassAndMethod( list.get( 0 ) )[0].substring( 0,5 ), "Dubbo" );
        assertEquals( runOrderCalculator.getClassAndMethod( list.get( 3 ) )[0].substring( 0,2 ), "My" );
    }
}
