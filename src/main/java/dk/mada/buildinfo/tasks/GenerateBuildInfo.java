package dk.mada.buildinfo.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.tasks.GenerateMavenPom;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension;
import org.jspecify.annotations.Nullable;

import dk.mada.buildinfo.util.FileInfos;
import dk.mada.buildinfo.util.FileInfos.FileInfo;

/**
 * Task for generating buildinfo file.
 *
 * Takes header information from publishing data and adds output lines for all publishable artifacts.
 */
public abstract class GenerateBuildInfo extends DefaultTask {
    /** The line separator to use. */
    private static final String NL = System.lineSeparator();
    /** The Gradle logger. */
    private final Logger logger;
    /** The project the task is registered on. */
    private final Project project;
    /** The maven publications to work on. */
    private final List<MavenPublication> mavenPublications = new ArrayList<>();

    /** {@return the buildinfo file the result is written to} */
    @OutputFile
    public abstract RegularFileProperty getBuildInfoFile();

    /** {@return a list of module files that will be published} */
    @InputFiles
    public abstract ListProperty<RegularFile> getModuleFiles();

    /** {@return a list of pom files that will be published} */
    @InputFiles
    public abstract ListProperty<Path> getPomFiles();

    /**
     * Constructs new task instance.
     *
     * Note that it is expected to be lazy configured before activation.
     *
     * @see #lazyConfiguration()
     */
    public GenerateBuildInfo() {
        project = getProject();
        logger = project.getLogger();

        String buildinfoDefaultFilename = project.getName() + "-" + project.getVersion() + ".buildinfo";
        getBuildInfoFile()
                .convention(project.getLayout().getBuildDirectory().file("buildinfo/" + buildinfoDefaultFilename));
    }

    /**
     * Lazy configuration of the task before it gets activated.
     *
     * Should be called from the task registration.
     */
    public void lazyConfiguration() {
        PublishingExtension publishingExt = getProject().getExtensions().findByType(PublishingExtension.class);
        onlyIf("Publishing extension not active", t -> publishingExt != null);

        if (publishingExt != null) {
            List<MavenPublication> foundPublications = publishingExt.getPublications().stream()
                    .filter(p -> p instanceof MavenPublication)
                    .map(p -> MavenPublication.class.cast(p))
                    .toList();
            mavenPublications.addAll(foundPublications);

            onlyIf("No maven publications to base buildinfo on", t -> !foundPublications.isEmpty());
        }

        captureModuleTaskInputs();
        capturePomTaskInputs();
    }

    /**
     * Captures the outputs from GenerateModuleMetadata tasks.
     */
    private void captureModuleTaskInputs() {
        for (GenerateModuleMetadata moduleTask : project.getTasks().withType(GenerateModuleMetadata.class)) {
            if (moduleTask.getPublication().getOrNull() instanceof MavenPublication) {
                getModuleFiles().add(moduleTask.getOutputFile());
            }
        }
    }

    /**
     * Captures the outputs from GenerateMavenPom tasks.
     *
     * Also adds dependency to those tasks, since their output is a file, not a provider.
     */
    private void capturePomTaskInputs() {
        for (GenerateMavenPom pomTask : project.getTasks().withType(GenerateMavenPom.class)) {
            dependsOn(pomTask);
            getPomFiles().add(pomTask.getDestination().toPath());
        }
    }

    /**
     * Generate the buildinfo file.
     */
    @TaskAction
    public void generateBuildInfo() {
        MavenPublication primaryPub = mavenPublications.getFirst();
        String buildinfo = createBuildinfo(primaryPub);

        Path outputFile = getBuildInfoFile().getAsFile().get().toPath();
        try {
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, buildinfo);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write " + outputFile, e);
        }
    }

    private String createBuildinfo(MavenPublication primaryPub) {
        logger.info("See modules: {}", getModuleFiles().get());
        logger.info("See Poms: {}", getPomFiles().get());

        Property<String> cloneConnection = getProject().getObjects().property(String.class);
        GradlePluginDevelopmentExtension pluginExt = getProject().getExtensions().findByType(GradlePluginDevelopmentExtension.class);
        if (pluginExt != null) {
            cloneConnection.set(pluginExt.getVcsUrl());
        } else {
            primaryPub.getPom().scm(mps -> {
                cloneConnection.set(mps.getDeveloperConnection());
            });
        }

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
                .replace("@OS_NAME@", System.getProperty("os.name"));

        String output = header;
        int pubNo = 0;
        for (MavenPublication pub : mavenPublications) {
            String coords = pub.getGroupId() + ":" + pub.getArtifactId();

            output = output + "outputs." + pubNo + ".coordinates=" + coords + NL;

            int artNo = 0;

            Path pomFile = findMatchingPomFile(pub);
            if (pomFile != null) {
                String pomFilename = pub.getArtifactId() + "-" + project.getVersion() + ".pom";
                output = output + renderOutputInfo(pubNo, artNo++, pomFile, pomFilename);
            }
            Path moduleFile = findMatchingModuleFile(pub);
            if (moduleFile != null) {
                String moduleFilename = pub.getArtifactId() + "-" + project.getVersion() + ".module";
                output = output + renderOutputInfo(pubNo, artNo++, moduleFile, moduleFilename);
            }
            List<MavenArtifact> sortedArtifacts = pub.getArtifacts().stream()
                    .sorted((a, b) -> a.getFile().compareTo(b.getFile()))
                    .toList();
            for (MavenArtifact ma : sortedArtifacts) {
                output = output + renderOutputInfo(pubNo, artNo++, ma.getFile().toPath());
            }

            pubNo++;
        }

        return output;
    }

    /**
     * Look for the module file associated with the given maven publication.
     *
     * This assumes that the module file is generated in a folder named after the maven publication. This assumption may not
     * always be true.
     *
     * I tried using a @Input MapPropety<String, RegularFile> but this could not handle non-existing files. Maybe use the
     * current @InputFiles lists and a separate map for association if this does not work out?!
     *
     * @param pub the maven publication to find a module file for
     * @return the found module file, or null
     */
    private @Nullable Path findMatchingModuleFile(MavenPublication pub) {
        return getModuleFiles().get().stream()
                .map(rf -> rf.getAsFile().toPath())
                .filter(Files::isRegularFile)
                .filter(path -> pub.getName().equals(path.getName(path.getNameCount() - 2).getFileName().toString()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Look for the POM file associated with the given maven publication.
     *
     * This assumes that the module file is generated in a folder named after the maven publication. This assumption may not
     * always be true.
     *
     * @param pub the maven publication to find a module file for
     * @return the found POM file, or null
     */
    private @Nullable Path findMatchingPomFile(MavenPublication pub) {
        return getPomFiles().get().stream()
                .filter(Files::isRegularFile)
                .filter(path -> pub.getName().equals(path.getName(path.getNameCount() - 2).getFileName().toString()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Renders information for an output file.
     *
     * @param pubNo the publication number
     * @param artNo the artifact number
     * @param file  the file to render information for
     * @return a buildinfo output-information line
     */
    private String renderOutputInfo(int pubNo, int artNo, Path file) {
        return renderOutputInfo(pubNo, artNo, file, file.getFileName().toString());
    }

    /**
     * Renders information for an output file, overriding the filename.
     *
     * @param pubNo    the publication number
     * @param artNo    the artifact number
     * @param file     the file to render information for
     * @param filename the filename to use
     * @return a buildinfo output-information line
     */
    private String renderOutputInfo(int pubNo, int artNo, Path file, String filename) {
        FileInfo info = FileInfos.from(file);
        String prefix = "outputs." + pubNo + "." + artNo;
        return prefix + ".filename=" + filename + NL
                + prefix + ".length=" + info.size() + NL
                + prefix + ".checksums.sha512=" + info.sha512sum() + NL;
    }
}
