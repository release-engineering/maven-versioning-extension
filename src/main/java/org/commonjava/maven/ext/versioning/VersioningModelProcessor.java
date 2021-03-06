package org.commonjava.maven.ext.versioning;

import static org.commonjava.maven.ext.versioning.IdUtils.ga;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.locator.ModelLocator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.logging.Logger;

/**
 * {@link ModelProcessor} implementation to override {@link DefaultModelProcessor} and inject versioning modifications.
 * This is a hook implementation used from within Maven's core. It will not be referenced directly in this project, 
 * BUT THAT DOES NOT MEAN IT'S NOT USED.
 * 
 * @author jdcasey
 */
@Component( role = ModelProcessor.class )
public class VersioningModelProcessor
    implements ModelProcessor
{

    @Requirement
    private ModelLocator locator;

    @Requirement
    private ModelReader reader;

    @Requirement
    private VersioningModifier modder;

    @Requirement
    private Logger logger;

    @Override
    public File locatePom( final File projectDirectory )
    {
        return locator.locatePom( projectDirectory );
    }

    @Override
    public Model read( final File input, final Map<String, ?> options )
        throws IOException, ModelParseException
    {
        final Model model = reader.read( input, options );

        applyVersioning( model );

        return model;
    }

    private void applyVersioning( final Model model )
        throws IOException
    {
        final VersioningSession session = VersioningSession.getInstance();
        if ( !session.isEnabled() )
        {
            logger.debug( "[VERSION-EXT] " + model.toString() + ": Versioning session disabled. Skipping modification." );
            return;
        }

        final Set<String> changed = session.getChangedGAVs();
        try
        {
            if ( modder.applyVersioningChanges( model, session.getVersioningChanges() ) )
            {
                logger.debug( "[VERSION-EXT] " + model.toString() + ": Version modified." );
                changed.add( ga( model ) );
            }
            else
            {
                logger.debug( "[VERSION-EXT] " + model.toString() + ": No version modifications. Skipping modification." );
            }
        }
        catch ( final InterpolationException e )
        {
            throw new IOException( "Interpolation failed while applying versioning changes: " + e.getMessage(), e );
        }
    }

    @Override
    public Model read( final Reader input, final Map<String, ?> options )
        throws IOException, ModelParseException
    {
        final Model model = reader.read( input, options );

        applyVersioning( model );

        return model;
    }

    @Override
    public Model read( final InputStream input, final Map<String, ?> options )
        throws IOException, ModelParseException
    {
        final Model model = reader.read( input, options );

        applyVersioning( model );

        return model;
    }

}
