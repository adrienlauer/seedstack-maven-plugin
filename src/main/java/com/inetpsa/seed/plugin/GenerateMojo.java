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

import com.inetpsa.seed.plugin.components.ArtifactResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

/**
 * Defines the generate goal. This goal generates a SeedStack project from existing archetypes.
 *
 * @author adrien.lauer@mpsa.com
 */
@Mojo(name = "generate", requiresProject = false)
public class GenerateMojo extends AbstractMojo {
    public static List<String> possibleTypes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("web");
        add("batch");
        add("domain");
        add("rest");
    }});

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject mavenProject;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager buildPluginManager;

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private Prompter prompter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String type = mavenSession.getUserProperties().getProperty("type"),
                version = mavenSession.getUserProperties().getProperty("version"),
                archetypeGroupId = mavenSession.getUserProperties().getProperty("archetypeGroupId"),
                archetypeArtifactId = mavenSession.getUserProperties().getProperty("archetypeArtifactId"),
                archetypeVersion = mavenSession.getUserProperties().getProperty("archetypeVersion");

        boolean allowSnapshots = !mavenSession.getUserProperties().getProperty("allowSnapshots", "false").equals("false");

        if (StringUtils.isBlank(version)) {
            version = "1.0.0-SNAPSHOT";
        }

        if (StringUtils.isBlank(archetypeGroupId)) {
            archetypeGroupId = "org.seedstack";
        }

        if (StringUtils.isBlank(archetypeArtifactId)) {
            if (StringUtils.isBlank(type)) {
                try {
                    String answer = prompter.prompt("Enter the project type", possibleTypes);
                    if (answer == null || !possibleTypes.contains(answer)) {
                        return;
                    } else {
                        type = answer;
                    }
                } catch (PrompterException e) {
                    throw new MojoExecutionException("Project type is required", e);
                }
            }
            archetypeArtifactId = String.format("%s-archetype", type);
        }

        if (StringUtils.isBlank(archetypeVersion)) {
            try {
                archetypeVersion = artifactResolver.getHighestVersion(mavenProject, archetypeGroupId, archetypeArtifactId, allowSnapshots);
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to determine latest version of archetype, please specify it manually through the archetypeVersion property", e);
            }
        }

        // Using deprecated method to programmatically add a property to mojo execution
        mavenSession.getExecutionProperties().setProperty("version", version);

        String groupId = mavenSession.getUserProperties().getProperty("groupId");
        String artifactId = mavenSession.getUserProperties().getProperty("artifactId");
        try {
            if (StringUtils.isBlank(groupId)) {
                groupId = prompter.prompt("Generated project group id");
            }
            if (StringUtils.isBlank(artifactId)) {
                artifactId = prompter.prompt("Generated project artifact id");
            }
        } catch (PrompterException e) {
            throw new MojoExecutionException("Generated project group id and artifact id are required", e);
        }

        mavenSession.getExecutionProperties().put("groupId", groupId);
        mavenSession.getExecutionProperties().put("artifactId", artifactId);

        executeMojo(plugin(groupId("org.apache.maven.plugins"), artifactId("maven-archetype-plugin"), version("2.2")),

                goal("generate"),

                configuration(
                        element(name("interactiveMode"), "true"),
                        element(name("archetypeGroupId"), archetypeGroupId),
                        element(name("archetypeArtifactId"), archetypeArtifactId),
                        element(name("archetypeVersion"), archetypeVersion)
                ),

                executionEnvironment(mavenProject, mavenSession, buildPluginManager));
    }
}