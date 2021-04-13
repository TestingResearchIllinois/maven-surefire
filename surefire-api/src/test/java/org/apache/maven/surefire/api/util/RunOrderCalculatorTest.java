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

    public void testOrderTestMethodsNonRegex()
    {
        List<String> orderParamList = new ArrayList<String>();
        orderParamList.add( "DubboLazyConnectTest#testa2d" );
        orderParamList.add( "DubboLazyConnectTest#testabc" );
        orderParamList.add( "DubboLazyConnectTest#testa1b" );
        orderParamList.add( "DubboProtocolTest#testa1b" );
        orderParamList.add( "DubboProtocolTest#testaBc" );
        TestListResolver testListResolver = new TestListResolver( orderParamList );
        String className = "DubboLazyConnectTest";
        String className2 = "DubboProtocolTest";
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className, "testa2d", "testa1b" ), -1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className, "testa2d", "testabc" ), -1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className, "testa1b", "testabc" ), 1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className, "testa2d", "testaBc" ), 1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className, "testa3d", "testa1b" ), -1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className2, "testa2d", "testa1b" ), -1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className2, className, "testaBc", "testa1b" ), 1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className2, "testa3d", "testa1b" ), -1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className2, "testa2d", "testabc" ), 1 );
    }

    public void testOrderTestMethodsRegexNoneWrap()
    {
        List<String> orderParamList = new ArrayList<String>();
        orderParamList.add( "DubboLazyConnectTest#testa?c" );
        orderParamList.add( "DubboLazyConnectTest#testa?b" );
        orderParamList.add( "DubboProtocolTest#test?1*" );
        orderParamList.add( "!DubboLazyConnectTest#testa4b" );
        orderParamList.add( "!DubboProtocolTest#test11MyTest" );
        TestListResolver testListResolver = new TestListResolver( orderParamList );
        String className = "DubboLazyConnectTest";
        String className2 = "DubboProtocolTest";
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className, "testabc", "testa1b" ), -1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className, "testaBc", "testa2b" ), -1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className, "testa1b", "testa3c" ), 1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className, "testa1b", "testa4b" ), 1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className, "testa4b", "testabc" ), -1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className2, "testa1b", "test1123" ), -1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className2, className, "testa1b", "testa1b" ), 1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className2, className2, "testa1b", "test1123" ), 1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className2, className2, "test1123", "test11MyTest" ), 1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className2, className2, "test11MyTest", "test456" ), -1 );
    }

    public void testOrderTestClassesRegexNoneWrap()
    {
        List<String> orderParamList = new ArrayList<String>();
        orderParamList.add( "DubboLazy2*Test.java" );
        orderParamList.add( "???DubboLazy1*Test" );
        orderParamList.add( "!abcDubboLazy1PeaceTest" );
        TestListResolver testListResolver = new TestListResolver( orderParamList );
        String className = "DubboLazy2ConnectTest";
        String className2 = "456DubboLazy1ConnectTest";
        String className3 = "abcDubboLazy1PeaceTest";
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className2, null, null ), -1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className2, className, null, null  ), 1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className3, className2, null, null ), -1 );
        assertEquals( ( int ) testListResolver.testOrderComparator( className, className3, null, null ), 1 );
    }
}
