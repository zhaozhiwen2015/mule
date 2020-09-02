/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static java.lang.String.valueOf;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.addSharedLibraryDependency;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.createDeployablePomFile;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.updateArtifactPom;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.application.DeployableMavenClassLoaderModelLoader;
import org.mule.runtime.module.tooling.api.ArtifactAgnosticServiceBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

import org.apache.maven.model.Model;

public abstract class AbstractArtifactAgnosticServiceBuilder<T extends ArtifactAgnosticServiceBuilder, S>
    implements ArtifactAgnosticServiceBuilder<T, S> {

  private static final String TMP_APP_ARTIFACT_ID = "temp-artifact-id";
  private static final String TMP_APP_GROUP_ID = "temp-group-id";
  private static final String TMP_APP_VERSION = "temp-version";
  private static final String TMP_APP_MODEL_VERSION = "4.0.0";

  private final DefaultApplicationFactory defaultApplicationFactory;

  private ArtifactDeclaration artifactDeclaration;
  private Model model;
  private Map<String, String> artifactProperties = emptyMap();

  private File toolingServiceAppsFolder;
  private File toolingClassLoaderModelFolder;

  private Gson gson;

  protected AbstractArtifactAgnosticServiceBuilder(DefaultApplicationFactory defaultApplicationFactory,
                                                   File toolingServiceAppsFolder, File toolingClassLoaderModelFolder) {
    this.defaultApplicationFactory = defaultApplicationFactory;
    this.toolingServiceAppsFolder = toolingServiceAppsFolder;
    this.toolingClassLoaderModelFolder = toolingClassLoaderModelFolder;
    createTempMavenModel();

    this.gson = new GsonBuilder().create();
  }

  @Override
  public T setArtifactProperties(Map<String, String> artifactProperties) {
    checkState(artifactProperties != null, "artifactProperties cannot be null");
    this.artifactProperties = artifactProperties;
    return getThis();
  }

  @Override
  public T setArtifactDeclaration(ArtifactDeclaration artifactDeclaration) {
    checkState(artifactDeclaration != null, "artifactDeclaration cannot be null");
    this.artifactDeclaration = artifactDeclaration;
    return getThis();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T addDependency(String groupId, String artifactId, String artifactVersion,
                         String classifier, String type) {
    org.apache.maven.model.Dependency dependency = new org.apache.maven.model.Dependency();
    dependency.setGroupId(groupId);
    dependency.setArtifactId(artifactId);
    dependency.setVersion(artifactVersion);
    dependency.setType(type);
    dependency.setClassifier(classifier);

    addMavenModelDependency(dependency);
    return getThis();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T addDependency(Dependency dependency) {
    org.apache.maven.model.Dependency mavenModelDependency = new org.apache.maven.model.Dependency();
    mavenModelDependency.setGroupId(dependency.getGroupId());
    mavenModelDependency.setArtifactId(dependency.getArtifactId());
    mavenModelDependency.setVersion(dependency.getVersion());
    mavenModelDependency.setType(dependency.getType());
    mavenModelDependency.setClassifier(dependency.getClassifier());
    mavenModelDependency.setOptional(dependency.getOptional());
    mavenModelDependency.setScope(dependency.getScope());
    mavenModelDependency.setSystemPath(dependency.getSystemPath());
    mavenModelDependency.setExclusions(dependency.getExclusions().stream().map(exclusion -> {
      org.apache.maven.model.Exclusion mavenModelExclusion = new org.apache.maven.model.Exclusion();
      mavenModelExclusion.setGroupId(exclusion.getGroupId());
      mavenModelExclusion.setArtifactId(exclusion.getArtifactId());
      return mavenModelExclusion;
    }).collect(toList()));

    addMavenModelDependency(mavenModelDependency);
    return getThis();
  }

  private void addMavenModelDependency(org.apache.maven.model.Dependency dependency) {
    if (!MULE_PLUGIN_CLASSIFIER.equals(dependency.getClassifier())) {
      addSharedLibraryDependency(model, dependency);
    }
    model.getDependencies().add(dependency);
  }

  @Override
  public S build() {
    checkState(artifactDeclaration != null, "artifact configuration cannot be null");
    return createService(() -> {
      String applicationName = UUID.getUUID() + "-artifact-temp-app";
      File applicationFolder = new File(toolingServiceAppsFolder, applicationName);
      Properties deploymentProperties = new Properties();
      deploymentProperties.putAll(forcedDeploymentProperties());
      ApplicationDescriptor applicationDescriptor = new ApplicationDescriptor(applicationName, of(deploymentProperties));
      applicationDescriptor.setArtifactDeclaration(artifactDeclaration);
      applicationDescriptor.setConfigResources(singleton("empty-app.xml"));
      applicationDescriptor.setArtifactLocation(applicationFolder);
      applicationDescriptor.setAppProperties(artifactProperties);
      createDeployablePomFile(applicationFolder, model);
      updateArtifactPom(applicationFolder, model);
      applicationDescriptor.setClassLoaderModel(getClassLoaderModel(applicationFolder, model));
      return defaultApplicationFactory.createArtifact(applicationDescriptor);
    });
  }

  private ClassLoaderModel getClassLoaderModel(File applicationFolder, Model model)
      throws InvalidDescriptorLoaderException, IOException {
    StringJoiner stringJoiner = new StringJoiner("--");
    model.getDependencies().stream().forEach(d -> stringJoiner.add(valueOf(toId(d.getGroupId(),
                                                                                d.getArtifactId(),
                                                                                d.getType(),
                                                                                d.getClassifier(),
                                                                                d.getVersion())
                                                                                    .hashCode())));
    String hashId = stringJoiner.toString();
    File classLoaderModelCacheFile = new File(toolingClassLoaderModelFolder, hashId);

    if (classLoaderModelCacheFile.exists()) {
      ClassLoaderModel classLoaderModel =
          gson.fromJson(readFileToString(classLoaderModelCacheFile, defaultCharset()), ClassLoaderModel.class);
      return new ClassLoaderModel.ClassLoaderModelBuilder(classLoaderModel)
          .containing(applicationFolder.toURI().toURL())
          .build();
    }

    MavenClientProvider mavenClientProvider =
        MavenClientProvider.discoverProvider(AbstractArtifactAgnosticServiceBuilder.class.getClassLoader());

    ClassLoaderModel classLoaderModel = new DeployableMavenClassLoaderModelLoader(mavenClientProvider
        .createMavenClient(GlobalConfigLoader.getMavenConfig()))
            .load(applicationFolder, singletonMap(BundleDescriptor.class.getName(),
                                                  createTempBundleDescriptor()),
                  ArtifactType.APP);
    writeStringToFile(classLoaderModelCacheFile,
                      gson.toJson(new ClassLoaderModel.ClassLoaderModelBuilder()
                             .dependingOn(classLoaderModel.getDependencies())
                             .exportingPackages(classLoaderModel.getExportedPackages())
                             .exportingPrivilegedPackages(classLoaderModel.getPrivilegedExportedPackages(), classLoaderModel.getPrivilegedArtifacts())
                             .exportingResources(classLoaderModel.getExportedResources())
                             .includeTestDependencies(classLoaderModel.isIncludeTestDependencies())
                             .withLocalPackages(classLoaderModel.getLocalPackages())
                             .withLocalResources(classLoaderModel.getLocalResources())
                          .build()),
                      defaultCharset());
    return classLoaderModel;
  }

  protected Map<String, String> forcedDeploymentProperties() {
    return emptyMap();
  }

  protected abstract S createService(ApplicationSupplier applicationSupplier);

  private void createTempMavenModel() {
    model = new Model();
    model.setArtifactId(TMP_APP_ARTIFACT_ID);
    model.setGroupId(TMP_APP_GROUP_ID);
    model.setVersion(TMP_APP_VERSION);
    model.setDependencies(new ArrayList<>());
    model.setModelVersion(TMP_APP_MODEL_VERSION);
  }

  private BundleDescriptor createTempBundleDescriptor() {
    return new BundleDescriptor.Builder().setArtifactId(TMP_APP_ARTIFACT_ID).setGroupId(TMP_APP_GROUP_ID)
        .setVersion(TMP_APP_VERSION).setClassifier("mule-application").build();
  }

  private T getThis() {
    return (T) this;
  }

}
