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

import com.google.common.base.Strings;
import io.nuun.kernel.api.Kernel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.seedstack.seed.core.api.CommandRegistry;
import org.seedstack.seed.core.spi.command.Command;
import org.seedstack.seed.core.spi.command.PrettyCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class CmdMojoDelegate implements Callable<String> {
    private final CommandLineParser commandLineParser = new PosixParser();

    @Override
    @SuppressWarnings("unchecked")
    public String call() throws Exception {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Class<?> nuunCoreClass = contextClassLoader.loadClass("io.nuun.kernel.core.NuunCore");
        Class<?> kernelConfigurationClass = contextClassLoader.loadClass("io.nuun.kernel.api.config.KernelConfiguration");
        Class<?> injectorClass = contextClassLoader.loadClass("com.google.inject.Injector");

        Object kernelConfiguration = nuunCoreClass.getMethod("newKernelConfiguration").invoke(null);
        Kernel kernel = (Kernel) nuunCoreClass.getMethod("createKernel", kernelConfigurationClass).invoke(null, kernelConfiguration);

        try {
            kernel.init();
            kernel.start();

            Object injector = kernel.objectGraph().as(injectorClass);
            CommandRegistry commandRegistry = (CommandRegistry) injectorClass.getMethod("getInstance", Class.class).invoke(injector, CommandRegistry.class);
            Command command = createCommand(commandRegistry, "help", new String[]{});

            if (command instanceof PrettyCommand) {
                return ((PrettyCommand) command).prettify(command.execute(null));
            } else {
                return String.valueOf(command.execute(null));
            }
        } finally {
            kernel.stop();
        }
    }

    protected Command createCommand(CommandRegistry commandRegistry, String qualifiedName, String[] args) {
        if (Strings.isNullOrEmpty(qualifiedName)) {
            throw new IllegalArgumentException("No command named " + qualifiedName);
        }

        String commandScope;
        String commandName;

        if (qualifiedName.contains(":")) {
            String[] splitName = qualifiedName.split(":");
            commandScope = splitName[0].trim();
            commandName = splitName[1].trim();
        } else {
            commandScope = null;
            commandName = qualifiedName.trim();
        }

        // Build CLI options
        Options options = new Options();
        for (org.seedstack.seed.core.spi.command.Option option : commandRegistry.getOptionsInfo(commandScope, commandName)) {
            options.addOption(option.name(), option.longName(), option.hasArgument(), option.description());
        }

        // Parse the command options
        CommandLine cmd;
        try {
            cmd = commandLineParser.parse(options, args);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Syntax error in arguments", e);
        }

        Map<String, String> optionValues = new HashMap<String, String>();
        for (Option option : cmd.getOptions()) {
            optionValues.put(option.getOpt(), option.getValue());
        }

        return commandRegistry.createCommand(commandScope, commandName, cmd.getArgList(), optionValues);
    }
}
