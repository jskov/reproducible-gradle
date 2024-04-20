package dk.mada.buildinfo;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Jar;

public final class BuildinfoPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getLogger().lifecycle("APPLY plugin");
        project.getTasks().register("generateBuildInfo", GenerateBuildInfo.class)
                .configure(GenerateBuildInfo::lazyConfiguration);
        
        // FIXME: disable with option
        project.getTasks().withType(AbstractArchiveTask.class).configureEach(jar -> {
            project.getLogger().lifecycle(" FIX jar: {}", jar.getName());
            jar.setReproducibleFileOrder(true);
            jar.setPreserveFileTimestamps(false);
        });
    }
}
