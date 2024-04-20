package dk.mada.test;

import java.io.File;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

/**
 * NOTE:
 * To run from eclipse, first run:
 *  ./gradlew processResources
 *  echo "implementation-classpath=/home/jskov/git/buildinfo-gradle/bin/main:/home/jskov/git/buildinfo-gradle/build/resources/main" > ./build/pluginUnderTestMetadata/plugin-under-test-metadata.properties
 */

public class XTest {
    
    @Test
    void x() {
        BuildResult result = GradleRunner.create()
                .withProjectDir(new File("src/test/data/simple"))
                .withPluginClasspath()
                .withArguments("generateBuildInfo", "-s")
                .build();
        
        System.out.println("GOT " + result.getOutput());
    }
}
