/**
 * Copyright (c) 2013-2015 by The SeedStack authors. All rights reserved.
 *
 * This file is part of SeedStack, An enterprise-oriented full development stack.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.inetpsa.seed.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.cli.CommandLineUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Defines the run goal. This goal runs a SeedStack project.
 */
@Mojo(name = "run", requiresProject = true, threadSafe = true, defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class RunMojo extends AbstractExecutionMojo {

    @Parameter(property = "mainClass", defaultValue = SeedStackConstants.mainClassName, required = true)
    private String mainClass;

    @Parameter(property = "args")
    private String args;

    @Override
    protected void doRun(ClassLoader contextClassLoader) throws Exception {
        Method main = contextClassLoader.loadClass(mainClass).getMethod("main", String[].class);
        main.setAccessible(true);

        if (!Modifier.isStatic(main.getModifiers())) {
            throw new MojoExecutionException("Main method of class " + mainClass + " is not static");
        }

        main.invoke(null, new Object[]{CommandLineUtils.translateCommandline(args)});
    }
}
