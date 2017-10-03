package com.losobie.annotations;

import com.google.auto.service.AutoService;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes(
         "com.losobie.annotations.Wrapped"
)
@SupportedSourceVersion( SourceVersion.RELEASE_8 )
@AutoService( Processor.class )
public class WrappedProcessor extends AbstractProcessor {

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv
    ) {
        // Get all elements with the BuilderElement annotation
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith( Wrapped.class );
        if ( elements.isEmpty() ) {
            return false;
        }

        for ( Element element : elements ) {
            if ( !ElementKind.FIELD.equals( element.getKind() ) ) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Element is not a field."
                );
                return false;
            }
            DeclaredType type = (DeclaredType) element.asType();
            TypeElement typeElement = (TypeElement) type.asElement();

            String packageName = "com.losobie.wrappers";
            String interfaceName = typeElement.getSimpleName().toString();

            Set<Class<?>> imports = new HashSet<>();
            try {
                // Get the interface being delegated so we can include it as an import
                Class<?> c = Class.forName( processingEnv.getElementUtils().getBinaryName( typeElement ).toString() );
                imports.add( c );

                Set<Class<?>> possibleImports = new HashSet<>();
                for ( Method m : c.getMethods() ) {
                    possibleImports.add( m.getReturnType() );
                    possibleImports.addAll( Arrays.asList( m.getParameterTypes() ) );
                    possibleImports.addAll( Arrays.asList( m.getExceptionTypes() ) );
                }
                for ( Class<?> i : possibleImports ) {
                    Package p = i.getPackage();
                    if ( p == null ) {
                        continue;
                    } else if ( p.getName().startsWith( "java.lang" ) ) {
                        continue;
                    }
                    imports.add( i );
                }
            } catch ( ClassNotFoundException ex ) {
                Logger.getLogger( WrappedProcessor.class.getName() ).log( Level.SEVERE, null, ex );
            }

            Class<Connection> clazz = Connection.class;

            Collection<Method> methods = Arrays.asList( clazz.getMethods() );
            createBuilderClass( packageName,
                                interfaceName,
                                imports,
                                methods );
        }

        return false;
    }

    private void createBuilderClass(
            String packageName,
            String interfaceName,
            Collection<Class<?>> imports,
            Collection<Method> methods
    ) {
        // Attempt to load the velocity properties file
        Properties props = new Properties();
        URL url = this.getClass().getClassLoader().getResource( "velocity.properties" );
        try {
            props.load( url.openStream() );
        } catch ( IOException ex ) {
            Logger.getLogger( WrappedProcessor.class.getName() ).log( Level.SEVERE, null, ex );
            return;
        }

        // Initialize the Velocity engine and context
        VelocityEngine ve = new VelocityEngine( props );
        ve.init();

        VelocityContext vc = new VelocityContext();
        vc.put( "packageName", packageName );
        vc.put( "interfaceName", interfaceName );
        vc.put( "imports", imports );
        vc.put( "methods", methods );

        // Load the template
        Template vt = ve.getTemplate( "WrappedTemplate.vm" );

        // Process the methods and template into a Java source file
        String outputFile = packageName + "." + interfaceName + "Wrapper";
        JavaFileObject jfo;
        try {
            jfo = processingEnv.getFiler().createSourceFile( outputFile );
        } catch ( IOException ex ) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "error generating class file: " + outputFile
            );
            return;
        }

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "creating source file: " + jfo.toUri()
        );

        // Write the source file
        try ( Writer writer = jfo.openWriter() ) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "applying velocity template: " + vt.getName() );

            vt.merge( vc, writer );
        } catch ( IOException ex ) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "error writing class file: " + outputFile
            );
        }
    }
}
