package org.apache.maven.plugin.surefire;

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

import org.apache.maven.plugin.surefire.extensions.DefaultConsoleOutputReporter;
import org.apache.maven.plugin.surefire.extensions.DefaultStatelessReporter;
import org.apache.maven.plugin.surefire.extensions.DefaultStatelessTestsetInfoReporter;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.powermock.reflect.Whitebox.getInternalState;

public class CommonReflectorTest
{
    private StartupReportConfiguration startupReportConfiguration;
    private ConsoleLogger consoleLogger;
    private File reportsDirectory;
    private File statistics;
    private DefaultStatelessReporter xmlReporter;
    private DefaultConsoleOutputReporter consoleOutputReporter = new DefaultConsoleOutputReporter();
    private DefaultStatelessTestsetInfoReporter infoReporter = new DefaultStatelessTestsetInfoReporter();

    @Before
    public void setup()
    {
        File target = new File( System.getProperty( "user.dir" ), "target" );
        reportsDirectory = new File( target, "tmp6" );
        statistics = new File( reportsDirectory, "TESTHASH" );
        xmlReporter = new DefaultStatelessReporter();
        infoReporter = new DefaultStatelessTestsetInfoReporter();

        startupReportConfiguration = new StartupReportConfiguration( true, true, "PLAIN", false, reportsDirectory,
                false, null, statistics, false, 1, null, null, false,
                xmlReporter, consoleOutputReporter, infoReporter);

        consoleLogger = mock( ConsoleLogger.class );
    }

    @Test
    public void createReportingReporterFactory()
    {
        CommonReflector reflector = new CommonReflector( Thread.currentThread().getContextClassLoader() );
        DefaultReporterFactory factory = (DefaultReporterFactory) reflector.createReportingReporterFactory(
                startupReportConfiguration, consoleLogger );

        assertThat( factory )
                .isNotNull();

        StartupReportConfiguration reportConfiguration = getInternalState( factory, "reportConfiguration" );
        assertThat( reportConfiguration )
                .isNotSameAs( startupReportConfiguration );
        assertThat( reportConfiguration.isUseFile() ).isTrue();
        assertThat( reportConfiguration.isPrintSummary() ).isTrue();
        assertThat( reportConfiguration.getReportFormat() ).isEqualTo( "PLAIN" );
        assertThat( reportConfiguration.isRedirectTestOutputToFile() ).isFalse();
        assertThat( reportConfiguration.getReportsDirectory() ).isSameAs( reportsDirectory );
        assertThat( reportConfiguration.isTrimStackTrace() ).isFalse();
        assertThat( reportConfiguration.getReportNameSuffix() ).isNull();
        assertThat( reportConfiguration.getStatisticsFile() ).isSameAs( statistics );
        assertThat( reportConfiguration.isRequiresRunHistory() ).isFalse();
        assertThat( reportConfiguration.getRerunFailingTestsCount() ).isEqualTo( 1 );
        assertThat( reportConfiguration.getXsdSchemaLocation() ).isNull();
        assertThat( reportConfiguration.getEncoding() ).isEqualTo( UTF_8 );
        assertThat( reportConfiguration.isForkMode() ).isFalse();
        assertThat( reportConfiguration.getXmlReporter().toString() )
                .isEqualTo( xmlReporter.toString() );
        assertThat( reportConfiguration.getTestsetReporter().toString() )
                .isEqualTo( infoReporter.toString() );
        assertThat( reportConfiguration.getConsoleOutputReporter().toString() )
                .isEqualTo( consoleOutputReporter.toString() );
    }
}