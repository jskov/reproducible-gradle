package dk.mada.buildinfo;

import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.tasks.GenerateMavenPom;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class GenerateBuildInfo extends DefaultTask {
    private static final String NL = System.lineSeparator();
    private final Logger logger;
    private final Project project;

    @OutputFile
    public abstract RegularFileProperty getBuildInfoFile();
    
    @Inject
    public GenerateBuildInfo(ProjectLayout layout) {
        dependsOn("publish");
        getOutputs().upToDateWhen(t -> false);
        project = getProject();
        this.logger = project.getLogger();
        
        getBuildInfoFile().convention(layout.getBuildDirectory().file("buildinfo/" +project.getName() + "-" + project.getVersion() + ".buildinfo"));
    }
    
    @TaskAction
    public void go() {
        Path outputFile = getBuildInfoFile().get().getAsFile().toPath();
        
        logger.lifecycle(" RUN TASK : {}", outputFile);

        PublishingExtension pubs = getProject().getExtensions().getByType(PublishingExtension.class);
        PublicationContainer publications = pubs.getPublications();
        logger.lifecycle(" publications: {}", publications);
        List<MavenPublication> mavenPublications = publications.stream()
            .filter(p -> p instanceof MavenPublication)
            .map(p -> MavenPublication.class.cast(p))
            .toList();

        if (mavenPublications.isEmpty()) {
            logger.warn("No maven publications to base buildinfo on");
            return;
        }
        
        MavenPublication primaryPub = mavenPublications.getFirst();

        try {
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, build(primaryPub, mavenPublications));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // Used by reproducible-central
    // See https://reproducible-builds.org/docs/jvm/ ('.buildinfo file' section)
    private String build(MavenPublication primaryPub, List<MavenPublication> publications) {
        Property<String> cloneConnection = getProject().getObjects().property(String.class);

        Map<MavenPom, Path> moduleLocations = getModulePaths();
        Map<MavenPom, Path> pomLocations = getPomFileLocations();

        logger.lifecycle("MODULES: {}", moduleLocations);
        logger.lifecycle("POMs: {}", pomLocations);
        
        primaryPub.getPom().scm(mps -> {
            cloneConnection.set(mps.getDeveloperConnection());
        });
        
        String header = """
            buildinfo.version=1.0-SNAPSHOT
    
            name=@NAME@
            group-id=@GROUP@
            artifact-id=@ARTIFACT@
            version=@VERSION@

            build-tool=gradle

            java.version=@JAVA_VERSION@
            java.vendor=@JAVA_VENDOR@
            os.name=@OS_NAME@

            source.scm.uri=@GIT_URI@
            source.scm.tag=@VERSION@

            """
                .replace("@NAME@", project.getName())
                .replace("@GROUP@", Objects.toString(project.getGroup()))
                .replace("@ARTIFACT@", primaryPub.getArtifactId())
                .replace("@VERSION@", Objects.toString(project.getVersion()))
                .replace("@GIT_URI@", cloneConnection.get())
                .replace("@JAVA_VERSION@", System.getProperty("java.version"))
                .replace("@JAVA_VENDOR@", System.getProperty("java.vendor"))
                .replace("@OS_NAME@", System.getProperty("os.name"))
                ;


        // ./gradlew -Pversion=0.0.0 generateBuildInfo ; cat build/buildinfo/mada-style-gradle-0.0.0.buildinfo
        
        String output = header;
        int pubNo = 0;
        for (MavenPublication pub : publications) {
            String coords = pub.getGroupId() + ":" + pub.getArtifactId();

            output = output + "outputs." + pubNo + ".coordinates=" + coords + NL;

            int artNo = 0;

            Path pomFile = pomLocations.get(pub.getPom());
            if (pomFile != null) {
                output = output + renderArtifact(pubNo, artNo++, pomFile, pub.getArtifactId() + "-" + project.getVersion() + ".pom");
            }
            Path moduleFile = moduleLocations.get(pub.getPom());
            if (moduleFile != null) {
                output = output + renderArtifact(pubNo, artNo++, moduleFile, pub.getArtifactId() + "-" + project.getVersion() + ".module");
            }
            
            for (MavenArtifact ma : pub.getArtifacts()) {
                output = output + renderArtifact(pubNo, artNo++, ma.getFile().toPath());
            }

            pubNo++;
        }
        
        return output;
    }

    private Map<MavenPom, Path> getModulePaths() {
        Map<MavenPom, Path> result = new HashMap<>();
        for (GenerateModuleMetadata task : project.getTasks().withType(GenerateModuleMetadata.class)) {
            Path moduleFile = task.getOutputFile().get().getAsFile().toPath();
            if (Files.isRegularFile(moduleFile)
                    && task.getPublication().getOrNull() instanceof MavenPublication mp) {
                result.put(mp.getPom(), moduleFile);
            }
        }
        return result;
    }
    
    private String renderArtifact(int pubNo, int artNo, Path file) {
        return renderArtifact(pubNo, artNo, file, file.getFileName().toString());
    }

    private String renderArtifact(int pubNo, int artNo, Path file, String filename) {
        String prefix = "outputs." + pubNo + "." + artNo;
        return prefix + ".filename=" + filename + NL
                + prefix + ".length=" + size(file) + NL
                + prefix + ".checksums.sha512=" + sha512sum(file) + NL;
    }
    
    private Map<MavenPom, Path> getPomFileLocations() {
        return project.getTasks().withType(GenerateMavenPom.class).stream()
            .collect(toMap(gmp -> gmp.getPom(), gmp -> gmp.getDestination().toPath()));
    }

    record X(String coord, List<Path> files) {
        
    }
    
    private long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get size of file " + file, e);
        }
    }
    
    private String sha512sum(Path file) {
        MessageDigest md;
        byte[] buffer = new byte[8192];
        try (InputStream is = Files.newInputStream(file)) {
            md = MessageDigest.getInstance("SHA-512");
            int read;
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to checksum file " + file, e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to get digester for sha-512", e);
        }
        return HexFormat.of().formatHex(md.digest());
    }
}
