package dk.mada.buildinfo;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class BuildinfoPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getLogger().lifecycle("APPLY plugin");
        project.getTasks().register("generateBuildInfo", GenerateBuildInfo.class)
                .configure(GenerateBuildInfo::lazyConfiguration);
    }
}
