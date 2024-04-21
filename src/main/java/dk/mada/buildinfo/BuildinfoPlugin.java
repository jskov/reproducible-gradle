package dk.mada.buildinfo;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import dk.mada.buildinfo.tasks.GenerateBuildInfo;

/**
 * Plugin providing a generateBuildInfo task.
 *
 * The plugin also configures archive tasks to be reproducible.
 */
public final class BuildinfoPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("generateBuildInfo", GenerateBuildInfo.class)
                .configure(GenerateBuildInfo::lazyConfiguration);

        project.getTasks().withType(AbstractArchiveTask.class).configureEach(jar -> {
            jar.setReproducibleFileOrder(true);
            jar.setPreserveFileTimestamps(false);
        });
    }
}
