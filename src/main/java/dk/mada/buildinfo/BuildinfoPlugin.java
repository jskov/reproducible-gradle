package dk.mada.buildinfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;

public final class BuildinfoPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getLogger().lifecycle("APPLY plugin");
        project.getTasks().register("generateBuildInfo", GenerateBuildInfo.class)
            .configure(gbi -> configureTask(project, gbi));
    }

    private void configureTask(Project project, GenerateBuildInfo gbi) {
//        MapProperty<String, RegularFile> moduleFiles = gbi.getModuleFiles();
        ListProperty<RegularFile> moduleFiles = gbi.getModuleFiles();
        for (GenerateModuleMetadata moduleTask : project.getTasks().withType(GenerateModuleMetadata.class)) {
            if (moduleTask.getPublication().getOrNull() instanceof MavenPublication mp) {
                
                RegularFileProperty outputFile = moduleTask.getOutputFile();
                String id = mp.getGroupId()+mp.getArtifactId();
                project.getLogger().lifecycle(" See {} : {}", id, outputFile);
 //               moduleFiles.put(id, outputFile);
//                if (id.equals("dk.mada.stylemada-style-gradle")) {
                    moduleFiles.add(outputFile);
//                }
            }
        }
    }
}
