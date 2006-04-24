package org.apache.maven.plugins.release.phase;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.config.ReleaseConfiguration;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Check the dependencies of all projects being released to see if there are any unreleased snapshots.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo plugins with no version will be resolved to RELEASE which is not a snapshot, but remains unresolved to this point. This is a potential hole in the check, and should be revisited after the release pom writing is done and resolving versions to verify whether it is.
 * @todo plugins injected by the lifecycle are not tested here. They will be injected with a RELEASE version so are covered under the above point.
 */
public class CheckDependencySnapshotsPhase
    implements ReleasePhase
{
    public void execute( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        List reactorProjects = releaseConfiguration.getReactorProjects();
        Set reactorProjectKeys = createReactorProjectSet( reactorProjects );

        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            checkProject( project, reactorProjectKeys );
        }
    }

    private void checkProject( MavenProject project, Set reactorProjectKeys )
        throws ReleaseExecutionException
    {
        Set snapshotDependencies = new HashSet();

        if ( project.getParentArtifact() != null )
        {
            if ( checkArtifact( project.getParentArtifact(), reactorProjectKeys ) )
            {
                snapshotDependencies.add( project.getParentArtifact() );
            }
        }

        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( checkArtifact( artifact, reactorProjectKeys ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        for ( Iterator i = project.getPluginArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( checkArtifact( artifact, reactorProjectKeys ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        for ( Iterator i = project.getReportArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( checkArtifact( artifact, reactorProjectKeys ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        for ( Iterator i = project.getExtensionArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( checkArtifact( artifact, reactorProjectKeys ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        if ( !snapshotDependencies.isEmpty() )
        {
            List snapshotsList = new ArrayList( snapshotDependencies );

            Collections.sort( snapshotsList );

            StringBuffer message = new StringBuffer();

            for ( Iterator i = snapshotsList.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

                message.append( "    " );

                message.append( artifact );

                message.append( "\n" );
            }

            throw new ReleaseExecutionException(
                "Can't release project due to non released dependencies :\n" + message );
        }
    }

    private static boolean checkArtifact( Artifact artifact, Set reactorProjectKeys )
    {
        String versionlessArtifactKey = ArtifactUtils.versionlessKey( artifact.getGroupId(), artifact.getArtifactId() );

        // We are only looking at dependencies external to the project - ignore anything found in the reactor as
        // it's version will be updated
        // TODO: will it? what if it is a different snapshot version to the one being updated in the reactor?
        return !reactorProjectKeys.contains( versionlessArtifactKey ) &&
            ArtifactUtils.isSnapshot( artifact.getVersion() );
    }

    public void simulate( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        // It makes no modifications, so simulate is the same as execute
        execute( releaseConfiguration );
    }

    private static Set createReactorProjectSet( List reactorProjects )
    {
        Set reactorProjectSet = new HashSet();

        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            String versionlessArtifactKey =
                ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            reactorProjectSet.add( versionlessArtifactKey );
        }

        return reactorProjectSet;
    }

}
