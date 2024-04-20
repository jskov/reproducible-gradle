package dk.mada.test;

import java.io.File;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * NOTE:
 * To run from eclipse, first run:
 *  ./gradlew processResources
 *  echo "implementation-classpath=/home/jskov/git/buildinfo-gradle/bin/main:/home/jskov/git/buildinfo-gradle/build/resources/main" > ./build/pluginUnderTestMetadata/plugin-under-test-metadata.properties
 */

public class XTest {
    /** The name of the task to test. */
    private static final String TASK_NAME = ":generateBuildInfo";

    /**
     * Tests that the task is gracefully disabled if there is no
     * sensible data for its configuration.
     */
    @Test
    void pluginDoesNotBreakDownIfPublishingIsMissing() {
        BuildResult result = runTest("src/test/data/disabled");
        
        assertThat(result.task(TASK_NAME).getOutcome())
            .isEqualTo(TaskOutcome.SKIPPED);
    }

    private BuildResult runTest(String testDataPath) {
        return GradleRunner.create()
                .withProjectDir(new File(testDataPath))
                .withPluginClasspath()
                .withArguments(TASK_NAME, "-s")
                .build();
    }
}
