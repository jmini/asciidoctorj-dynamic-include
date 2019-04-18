package fr.jmini.asciidoctorj.dynamicinclude;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.asciidoctor.extension.spi.ExtensionRegistry;

public class DynamicIncludeExtension implements ExtensionRegistry {

    @Override
    public void register(Asciidoctor asciidoctor) {
        JavaExtensionRegistry javaExtensionRegistry = asciidoctor.javaExtensionRegistry();

        IncludeProcessor includeProcessor = new DynamicIncludeProcessor();
        javaExtensionRegistry.includeProcessor(includeProcessor);
    }
}
