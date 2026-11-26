package com.tmsa.brujula.emision;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Fitness functions de arquitectura (capítulo 7): si alguien hace que el
 * dominio dependa de Spring o de la infraestructura, el build falla.
 */
@AnalyzeClasses(packages = "com.tmsa.brujula.emision", importOptions = ImportOption.DoNotIncludeTests.class)
class ReglasDeArquitecturaTest {

    @ArchTest
    static final ArchRule el_dominio_no_depende_de_frameworks_ni_de_otras_capas =
            noClasses().that().resideInAPackage("..dominio..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta..", "tools.jackson..",
                            "..aplicacion..", "..infraestructura..")
                    .because("el dominio debe poder probarse y evolucionar sin el framework");

    @ArchTest
    static final ArchRule la_aplicacion_no_conoce_la_infraestructura =
            noClasses().that().resideInAPackage("..aplicacion..")
                    .should().dependOnClassesThat().resideInAPackage("..infraestructura..")
                    .because("los casos de uso hablan con puertos, no con adaptadores");
}
