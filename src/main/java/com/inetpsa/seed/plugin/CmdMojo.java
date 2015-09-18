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

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Defines the run goal. This goal runs a SeedStack project.
 */
@Mojo(name = "cmd", requiresProject = true, threadSafe = true, defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class CmdMojo extends AbstractExecutionMojo {
    @Parameter(property = "cmd", required = true)
    private String cmd;

    @Parameter(property = "args")
    private String args;

    @Override
    @SuppressWarnings("unchecked")
    protected void doRun(ClassLoader contextClassLoader) throws Exception {
        PrintStream stdOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
        PrintStream stdErr = new PrintStream(new FileOutputStream(FileDescriptor.err));

        System.setOut(new PrintStream(new NullOutputStream()));
        System.setErr(new PrintStream(new NullOutputStream()));

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n");
            sb.append((((Class<CmdMojoDelegate>) contextClassLoader.loadClass("com.inetpsa.seed.plugin.CmdMojoDelegate")).newInstance().call()));
            sb.append("\n\n");
            stdOut.print(sb.toString());
        } catch (Exception e) {
            e.printStackTrace(stdErr);
        }
    }

    private static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            // just eat the bytes
        }
    }
}
