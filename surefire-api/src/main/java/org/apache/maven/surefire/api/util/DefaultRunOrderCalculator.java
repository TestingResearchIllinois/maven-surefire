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

import org.apache.maven.surefire.api.runorder.RunEntryStatisticsMap;
import org.apache.maven.surefire.api.testset.ResolvedTest;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestListResolver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Applies the final runorder of the tests
 *
 * @author Kristian Rosenvold
 */
public class DefaultRunOrderCalculator
    implements RunOrderCalculator
{
    private final Comparator<Class> sortOrder;

    private final RunOrder[] runOrder;

    private final RunOrderParameters runOrderParameters;

    private final int threadCount;

    private final Random random;

    private final TestListResolver testListResolver;

    public DefaultRunOrderCalculator( RunOrderParameters runOrderParameters, int threadCount )
    {
        this.runOrderParameters = runOrderParameters;
        this.threadCount = threadCount;
        this.runOrder = runOrderParameters.getRunOrder();
        this.sortOrder = this.runOrder.length > 0 ? getSortOrderComparator( this.runOrder[0] ) : null;
        Long runOrderRandomSeed = runOrderParameters.getRunOrderRandomSeed();
        if ( runOrderRandomSeed == null )
        {
            runOrderRandomSeed = System.nanoTime();
            runOrderParameters.setRunOrderRandomSeed( runOrderRandomSeed );
        }
        this.random = new Random( runOrderRandomSeed );
        this.testListResolver = getTestListResolver();
    }

    @Override
    @SuppressWarnings( "checkstyle:magicnumber" )
    public TestsToRun orderTestClasses( TestsToRun scannedClasses )
    {
        List<Class<?>> result = new ArrayList<>( 512 );

        for ( Class<?> scannedClass : scannedClasses )
        {
            result.add( scannedClass );
        }

        orderTestClasses( result, runOrder.length != 0 ? runOrder[0] : null );
        return new TestsToRun( new LinkedHashSet<>( result ) );
    }

    @Override
    public Comparator<String> comparatorForTestMethods()
    {
        if ( runOrder.length != 1 )
        {
            throw new IllegalStateException( "Unsupported number of runOrders. Expected 1. Got: " + runOrder.length );
        }
        RunOrder methodRunOrder = runOrder[0];
        if ( RunOrder.TESTORDER.equals( methodRunOrder ) )
        {
            return new Comparator<String>()
            {

                @Override
                public int compare( String o1, String o2 )
                {
                    String[] classAndMethod1 = getClassAndMethod( o1 );
                    String className1 = classAndMethod1[0];
                    String methodName1 = classAndMethod1[1];
                    String[] classAndMethod2 = getClassAndMethod( o2 );
                    String className2 = classAndMethod2[0];
                    String methodName2 = classAndMethod2[1];

                    return testListResolver.testOrderComparator( className1, className2, methodName1, methodName2 );
                }
            };
        }
        else
        {
            return null;
        }
    }

    public void addTestToOrders( String className, LinkedHashMap<String, List<String>> orders, String parenName )
    {
        List<String> classOrders = orders.get( className );
        if ( classOrders == null )
        {
            classOrders = new ArrayList<String>();
        }
        if ( ! classOrders.contains( parenName ) )
        {
            classOrders.add( parenName );
        }
        if ( ! orders.containsKey( className ) )
        {
            orders.put( className, classOrders );
        }
    }

    public TestListResolver getTestListResolver()
    {
        String orderParam = parseTestOrder( System.getProperty( "test" ) );
        if ( orderParam == null  )
        {
            throw new IllegalStateException( "Please set system property -Dtest to use fixed order"  );
        }
        List<String> list = Arrays.asList( orderParam.split( "," ) );
        return new TestListResolver( list );
    }

    public String[] getClassAndMethod( String request )
    {
        String[] classAndMethod = { request, request };
        if ( request.contains( "(" ) )
        {
            String[] nameSplit1 = request.split( "\\(" );
            classAndMethod[0] = nameSplit1[1].substring( 0, nameSplit1[1].length() - 1 );
            classAndMethod[1] = nameSplit1[0];
        }
        return classAndMethod;
    }

    private void orderTestClasses( List<Class<?>> testClasses, RunOrder runOrder )
    {
        if ( RunOrder.TESTORDER.equals( runOrder ) )
        {
            Collections.sort( testClasses, new Comparator<Class<?>>()
                    {
                        @Override
                        public int compare( Class<?> o1, Class<?> o2 )
                        {
                            return testListResolver.testOrderComparator( o1.getName(), o2.getName(), null, null );
                        }
                    });
        }
        else if ( RunOrder.RANDOM.equals( runOrder ) )
        {
            Collections.shuffle( testClasses, random );
        }
        else if ( RunOrder.FAILEDFIRST.equals( runOrder ) )
        {
            RunEntryStatisticsMap stat = RunEntryStatisticsMap.fromFile( runOrderParameters.getRunStatisticsFile() );
            List<Class<?>> prioritized = stat.getPrioritizedTestsByFailureFirst( testClasses );
            testClasses.clear();
            testClasses.addAll( prioritized );

        }
        else if ( RunOrder.BALANCED.equals( runOrder ) )
        {
            RunEntryStatisticsMap stat = RunEntryStatisticsMap.fromFile( runOrderParameters.getRunStatisticsFile() );
            List<Class<?>> prioritized = stat.getPrioritizedTestsClassRunTime( testClasses, threadCount );
            testClasses.clear();
            testClasses.addAll( prioritized );

        }
        else if ( sortOrder != null )
        {
            Collections.sort( testClasses, sortOrder );
        }
    }

    private String parseTestOrder( String s )
    {
        if ( s != null && s != "" )
        {
            File f = new File( s );
            if ( f.exists() && !f.isDirectory ( ) )
            {
                try
                {
                    List<String> l = Files.readAllLines( f.toPath(), Charset.defaultCharset( ) );
                    StringBuilder sb = new StringBuilder( );
                    for ( String sd : l )
                    {
                        sb.append( sd + "," );
                    }
                    String sd = sb.toString( );
                    return sd.substring( 0 , sd.length( ) - 1 );
                }
                catch ( IOException e )
                {
                }
            }
        }
        return s;
    }

    private List<Class<?>> sortClassesBySpecifiedOrder( List<Class<?>> testClasses, String flakyTestOrder )
    {
        HashMap<String, Class<?>> classes = new HashMap<>();
        for ( Class<?> each : testClasses )
        {
            classes.put( each.getName(), each );
        }
        LinkedList<Class<?>> ret = new LinkedList<>();
        for ( String s : flakyTestOrder.split( "," ) )
        {
            String testClass = s.substring( 0, s.indexOf( '#' ) );
            Class<?> c = classes.remove( testClass );
            if ( c != null )
            {
                ret.add( c );
            }
        }
        return ret;
    }

    private Comparator<Class> getSortOrderComparator( RunOrder runOrder )
    {
        if ( RunOrder.ALPHABETICAL.equals( runOrder ) )
        {
            return getAlphabeticalComparator();
        }
        else if ( RunOrder.REVERSE_ALPHABETICAL.equals( runOrder ) )
        {
            return getReverseAlphabeticalComparator();
        }
        else if ( RunOrder.HOURLY.equals( runOrder ) )
        {
            final int hour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
            return ( ( hour % 2 ) == 0 ) ? getAlphabeticalComparator() : getReverseAlphabeticalComparator();
        }
        else
        {
            return null;
        }
    }

    private Comparator<Class> getReverseAlphabeticalComparator()
    {
        return new Comparator<Class>()
        {
            @Override
            public int compare( Class o1, Class o2 )
            {
                return o2.getName().compareTo( o1.getName() );
            }
        };
    }

    private Comparator<Class> getAlphabeticalComparator()
    {
        return new Comparator<Class>()
        {
            @Override
            public int compare( Class o1, Class o2 )
            {
                return o1.getName().compareTo( o2.getName() );
            }
        };
    }
}
