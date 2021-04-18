package org.apache.maven.surefire.util;

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

/**
 * TODO docs
 */
public class MethodRunOrder
{

    public static final MethodRunOrder ALPHABETICAL = new MethodRunOrder( "alphabetical" );
    public static final MethodRunOrder REVERSE_ALPHABETICAL = new MethodRunOrder( "reversealphabetical" );
    public static final MethodRunOrder RANDOM = new MethodRunOrder( "random" );
    public static final MethodRunOrder FLAKY_FINDING = new MethodRunOrder( "fixed" );
    public static final MethodRunOrder DEFAULT = new MethodRunOrder( "default" );

    private final String name;

    MethodRunOrder( String name )
    {
        this.name = name;
    }

    private static MethodRunOrder[] values()
    {
        return new MethodRunOrder[] {ALPHABETICAL, REVERSE_ALPHABETICAL, RANDOM, FLAKY_FINDING, DEFAULT};
    }

    public boolean matches( String anotherName )
    {
        return name.equalsIgnoreCase( anotherName );
    }

    public static MethodRunOrder valueOf( String name )
    {
        if ( name == null )
        {
            return null;
        }
        else
        {
            for ( MethodRunOrder each : values() )
            {
                if ( each.matches( name ) )
                {
                    return each;
                }
            }
        }
        String errorMessage = createMessageForMissingRunOrder( name );
        throw new IllegalArgumentException( errorMessage );

    }

    private static String createMessageForMissingRunOrder( String name )
    {
        MethodRunOrder[] methodRunOrders = values();
        StringBuilder message = new StringBuilder( "There's no MethodRunOrder with the name " );
        message.append( name );
        message.append( ". Please use one of the following RunOrders: " );
        for ( int i = 0; i < methodRunOrders.length; i++ )
        {
            if ( i != 0 )
            {
                message.append( ", " );
            }
            message.append( methodRunOrders[i] );
        }
        message.append( '.' );
        return message.toString();
    }

    @Override
    public String toString()
    {
        return name;
    }

    public String name()
    {
        return name;
    }
}
