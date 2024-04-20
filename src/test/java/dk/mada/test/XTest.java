package dk.mada.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

public class XTest {
    /** The name of the task to test. */
    private static final String TASK_NAME = ":generateBuildInfo";

    @Test
    void pluginWorks() {
        Path testDataDir = Paths.get("src/test/data/plugin");
        BuildResult result = runTest(testDataDir);
        
        System.out.println(result.getOutput());
        
        assertThat(result.task(TASK_NAME).getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    }

    
    /**
     * Tests that the task is gracefully disabled if there is no
     * sensible data for its configuration.
     */
    @Test
    void pluginDoesNotBreakDownIfPublishingIsMissing() {
        BuildResult result = runTest(Paths.get("src/test/data/disabled"));
        
        assertThat(result.task(TASK_NAME).getOutcome())
            .isEqualTo(TaskOutcome.SKIPPED);
    }

    private BuildResult runTest(Path testDataDir) {
        return GradleRunner.create()
                .withProjectDir(testDataDir.toFile())
                .withPluginClasspath()
                .withArguments(TASK_NAME, "-s")
                .build();
    }
}
