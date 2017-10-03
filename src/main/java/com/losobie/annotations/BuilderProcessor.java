package com.losobie.annotations;

import com.google.auto.service.AutoService;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes( "com.losobie.annotations.BuilderMethod" )
@SupportedSourceVersion( SourceVersion.RELEASE_8 )
@AutoService( Processor.class )
public class BuilderProcessor extends AbstractProcessor {

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv
    ) {

        // Get all elements with the BuilderElement annotation
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith( BuilderMethod.class );
        if ( elements.isEmpty() ) {
            return false;
        }

        // Determine the class that we are working with
        TypeElement classElement = ( (TypeElement) elements.iterator().next().getEnclosingElement() );
        if ( ElementKind.CLASS != classElement.getKind() ) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Builder method annotation on unexpected element."
            );
            return false;
        }

        String fullClassName = classElement.getQualifiedName().toString();
        String className = classElement.getSimpleName().toString();
        PackageElement packageElement = (PackageElement) classElement.getEnclosingElement();
        String packageName = packageElement.getQualifiedName().toString();

        // Map the annotated methods
        //Map<String, ExecutableElement> methods = new HashMap<>();
        Set<ExecutableElement> methods = elements.stream().filter( (e) -> ( null != e.getKind() && e.getKind() == ElementKind.METHOD ) ).map( (e) -> {
                           ExecutableElement exeElement = (ExecutableElement) e;
                           if ( exeElement.getParameters().size() != 1 ) {
                               processingEnv.getMessager().printMessage(
                                       Diagnostic.Kind.ERROR,
                                       "Builder methods can only have one parameter.",
                                       e
                               );
                           }
                           return exeElement;
                       } ).collect( Collectors.toSet() );

        // Map a methods simple name to the full name of the first parameter
        Map<String, String> methodTypes = methods.stream().collect( Collectors.toMap(
                            (e) -> e.getSimpleName().toString(),
                            (e) -> e.getParameters().get( 0 ).asType().toString()
                    ) );

        createBuilderClass( packageName, fullClassName, className, methodTypes );

        return false;
    }

    private void createBuilderClass(
            String packageName,
            String fullClassName,
            String className,
            Map<String, String> methodTypes
    ) {
        // Attempt to load the velocity properties file
        Properties props = new Properties();
        URL url = this.getClass().getClassLoader().getResource( "velocity.properties" );
        try {
            props.load( url.openStream() );
        } catch ( IOException ex ) {
            Logger.getLogger( BuilderProcessor.class.getName() ).log( Level.SEVERE, null, ex );
            return;
        }

        // Initialize the Velocity engine and context
        VelocityEngine ve = new VelocityEngine( props );
        ve.init();

        VelocityContext vc = new VelocityContext();
        vc.put( "packageName", packageName );
        vc.put( "className", className );

        vc.put( "methods", methodTypes.entrySet() );

        // Load the template
        Template vt = ve.getTemplate( "BuilderTemplate.vm" );

        // Process the methods and template into a Java source file
        JavaFileObject jfo;
        try {
            jfo = processingEnv.getFiler().createSourceFile( fullClassName + "Builder" );
        } catch ( IOException ex ) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "error generating class file: " + fullClassName + "Builder"
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
                    "error writing class file: " + fullClassName + "Builder"
            );
        }
    }
}
