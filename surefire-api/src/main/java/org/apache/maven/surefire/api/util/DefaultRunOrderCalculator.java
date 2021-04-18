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
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.util.MethodRunOrder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
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
        MethodRunOrder order = runOrderParameters.getMethodRunOrder();
        if ( MethodRunOrder.DEFAULT.equals( order ) )
        {
            return null;
        }
        else if ( MethodRunOrder.ALPHABETICAL.equals( order ) )
        {
            return new Comparator<String>()
            {
                @Override
                public int compare( String o1, String o2 )
                {
                    return o1.compareTo( o2 );
                }
            };
        }
        else if ( MethodRunOrder.REVERSE_ALPHABETICAL.equals( order ) )
        {
            return new Comparator<String>()
            {
                @Override
                public int compare( String o1, String o2 )
                {
                    return o2.compareTo( o1 );
                }
            };
        }
        else if ( MethodRunOrder.RANDOM.equals( order ) )
        {
            return new Comparator<String>()
            {
                HashMap<String, Integer> randomVals = new HashMap<>();

                private int getRandom( String obj )
                {
                    if ( !randomVals.containsKey( obj ) )
                    {
                        randomVals.put( obj, random.nextInt() );
                    }
                    return randomVals.get( obj );
                }

                @Override
                public int compare( String o1, String o2 )
                {
                    int i1 = getRandom( o1 );
                    int i2 = getRandom( o2 );
                    return ( i1 > i2 ? 1 : -1 );
                }
            };
        }
        else if ( MethodRunOrder.FLAKY_FINDING.equals( order ) )
        {
            String orderParam = parseFlakyTestOrder( System.getProperty( "test" ) );
            if ( orderParam == null )
            {
                throw new IllegalStateException( "Please set system property -Dtest to use fixed order" );
            }
            final LinkedHashMap<String, List<String>> orders = new LinkedHashMap<>();
            for ( String s : orderParam.split( "," ) )
            {
                String[] nameSplit = s.split( "#" );
                String className = nameSplit[0];
                String testName = nameSplit[1];
                String parenName = testName + "(" + className + ")";
                addTestToOrders( className, orders, parenName );
            }
            return new Comparator<String>()
            {

                @Override
                public int compare( String o1, String o2 )
                {
                    String className1 = o1;
                    if ( o1.contains( "(" ) )
                    {
                        String[] nameSplit1 = o1.split( "\\(" );
                        className1 = nameSplit1[1].substring( 0, nameSplit1[1].length() - 1 );
                        addTestToOrders( className1, orders, o1 );
                    }

                    String className2 = o2;
                    if ( o2.contains( "(" ) )
                    {
                        String[] nameSplit2 = o2.split( "\\(" );
                        className2 = nameSplit2[1].substring( 0, nameSplit2[1].length() - 1 );
                        addTestToOrders( className2, orders, o2 );
                    }

                    if ( ! className2.equals( className1 ) )
                    {
                        List<String> classOrders = new ArrayList<String>( orders.keySet() );
                        if ( classOrders.indexOf( className1 ) < classOrders.indexOf( className2 ) )
                        {
                            return -1;
                        }
                        else
                        {
                            return 1;
                        }
                    }
                    else
                    {
                        List<String> testOrders = orders.get( className2 );
                        if ( testOrders.indexOf( o1 ) < testOrders.indexOf( o2 ) )
                        {
                            return -1;
                        }
                        else
                        {
                            return 1;
                        }
                    }
                }
            };
        }
        else
        {
            throw new UnsupportedOperationException( "Unsupported method run order: " + order.name() );
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

    private void orderTestClasses( List<Class<?>> testClasses, RunOrder runOrder )
    {
        if ( System.getProperty( "surefire.methodRunOrder" ) != null
             && System.getProperty( "surefire.methodRunOrder" ).toLowerCase().equals( "fixed" ) )
        {
            List<Class<?>> sorted
                = sortClassesBySpecifiedOrder( testClasses, parseFlakyTestOrder( System.getProperty( "test" ) ) );
            testClasses.clear();
            testClasses.addAll( sorted );
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

    private String parseFlakyTestOrder( String s )
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
