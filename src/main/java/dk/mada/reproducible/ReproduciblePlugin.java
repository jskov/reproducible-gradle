package dk.mada.reproducible;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

/**
 * Plugin configures archive tasks to be reproducible.
 */
public final class ReproduciblePlugin implements Plugin<Project> {

    /**
     * Creates a new instance.
     */
    public ReproduciblePlugin() {
        // empty
    }

    @Override
    public void apply(Project project) {
        Logger logger = project.getLogger();

        if (project.getParent() != null) {
            logger.warn("Plugin should only be applied on the main project");
            return;
        }

        project.allprojects(aProject -> aProject.afterEvaluate(postEvaluatedProject -> {
            logger.info("Configure jars in evaluated {}", postEvaluatedProject);
            postEvaluatedProject.getTasks().withType(AbstractArchiveTask.class).configureEach(jar -> {
                logger.info("Making {} reproducible", jar.getName());
                jar.setReproducibleFileOrder(true);
                jar.setPreserveFileTimestamps(false);
            });
        }));
    }
}
