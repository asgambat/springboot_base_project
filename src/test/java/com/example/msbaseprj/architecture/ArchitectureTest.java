package com.example.msbaseprj.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;

/**
 * Architecture tests that guard the layering and coding conventions of the
 * service. They run as part of the standard test suite and fail the build if a
 * change violates the rules.
 */
@AnalyzeClasses(packages = "com.example.msbaseprj", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule controllersAreRestControllers = classes().that().haveSimpleNameEndingWith("Controller")
			.should().beAnnotatedWith(RestController.class)
			.because("HTTP controllers must be annotated with @RestController");

	@ArchTest
	static final ArchRule repositoriesResideInRepositoryPackage = classes().that()
			.haveSimpleNameEndingWith("Repository").should().resideInAPackage("..repository..")
			.because("Persistence access must live in the repository package");

	@ArchTest
	static final ArchRule servicesDoNotDependOnControllers = noClasses().that().resideInAPackage("..service..").should()
			.dependOnClassesThat().resideInAPackage("..controller..")
			.because("Services must not depend on the web/controller layer");

	@ArchTest
	static final ArchRule noFieldInjection = fields().should().notBeAnnotatedWith(Autowired.class)
			.because("Constructor injection is preferred over field injection");

	@ArchTest
	static final ArchRule noStandardStreams = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

	@ArchTest
	static final ArchRule noJavaUtilLogging = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
}
