package org.apache.maven.surefire.junitplatform;

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

import java.util.Comparator;

import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;

/**
 * Orders test methods within a class by the {@code -Dsurefire.runOrder=testorder} specification.
 *
 * <p>The JUnit Platform (JUnit 5) provider hands whole classes to the Jupiter engine, which owns
 * method order via the {@link MethodOrderer} SPI. The class-level testorder run order therefore cannot
 * reach method order the way the JUnit 4 provider does. Registering this orderer as the default
 * ({@code junit.jupiter.testmethod.order.default}) closes that gap: it reuses the same
 * {@code comparatorForTestMethods()} the JUnit 4 path uses, so a {@code Class#method} order file
 * controls method order on JUnit 5 too. Methods with no entry in the specification keep their
 * discovery order (stable sort), and with no specification set this orderer does nothing.
 */
public class TestOrderMethodOrderer
    implements MethodOrderer
{
    /**
     * Set by {@link JUnitPlatformProvider} before execution when a testorder method comparator exists.
     * The provider and this orderer share the surefire-junit-platform classloader, so a static handoff
     * is sufficient and avoids serializing the whole specified order through configuration parameters.
     * Compares keys of the form {@code methodName(fullyQualifiedClassName)}.
     */
    static volatile Comparator<String> methodComparator;

    @Override
    public void orderMethods( MethodOrdererContext context )
    {
        final Comparator<String> comparator = methodComparator;
        if ( comparator == null )
        {
            return;
        }
        final String className = context.getTestClass().getName();
        context.getMethodDescriptors().sort( new Comparator<MethodDescriptor>()
        {
            @Override
            public int compare( MethodDescriptor a, MethodDescriptor b )
            {
                return comparator.compare( key( a ), key( b ) );
            }

            private String key( MethodDescriptor d )
            {
                return d.getMethod().getName() + "(" + className + ")";
            }
        } );
    }
}
