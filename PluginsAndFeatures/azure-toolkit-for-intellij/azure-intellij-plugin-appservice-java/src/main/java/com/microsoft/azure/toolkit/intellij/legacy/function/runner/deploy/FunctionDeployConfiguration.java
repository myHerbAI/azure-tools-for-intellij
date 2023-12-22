/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy;

import com.azure.resourcemanager.appservice.models.JavaVersion;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.SerializationFilter;
import com.intellij.util.xmlb.XmlSerializer;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.intellij.connector.IConnectionAware;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class FunctionDeployConfiguration extends AzureRunConfigurationBase<FunctionDeployModel>
        implements RunProfileWithCompileBeforeLaunchOption, IConnectionAware {

    private FunctionDeployModel functionDeployModel;
    private Module module;

    public FunctionDeployConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        functionDeployModel = new FunctionDeployModel();
    }

    @NotNull
    @Override
    public Module[] getModules() {
        return ModuleManager.getInstance(getProject()).getModules();
    }

    @Override
    public FunctionDeployModel getModel() {
        return this.functionDeployModel;
    }

    @Override
    public String getTargetName() {
        return null;
    }

    @Override
    public String getTargetPath() {
        return null;
    }

    @Override
    public String getSubscriptionId() {
        return Optional.ofNullable(functionDeployModel.getFunctionAppConfig())
                .map(FunctionAppConfig::getSubscription).map(Subscription::getId).orElse(StringUtils.EMPTY);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new FunctionDeploymentSettingEditor(getProject(), this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new FunctionDeploymentState(getProject(), this);
    }

    public void setDeploymentStagingDirectory(String deploymentStagingDirectory) {
        this.functionDeployModel.setDeploymentStagingDirectoryPath(deploymentStagingDirectory);
    }

    public String getDeploymentStagingDirectory() {
        return this.functionDeployModel.getDeploymentStagingDirectoryPath();
    }

    @Override
    public Module getModule() {
        Module module = ReadAction.compute(() ->
                Optional.ofNullable(getConfigurationModule()).map(JavaRunConfigurationModule::getModule).orElse(null));
        if (module == null && StringUtils.isNotEmpty(this.functionDeployModel.getModuleName())) {
            module = Arrays.stream(ModuleManager.getInstance(getProject()).getModules())
                    .filter(m -> StringUtils.equals(this.functionDeployModel.getModuleName(), m.getName()))
                    .findFirst().orElse(null);
        }
        return module;
    }

    public void saveTargetModule(Module module) {
        if (module != null) {
            this.module = module;
            functionDeployModel.setModuleName(module.getName());
        }
    }

    @Override
    public void validate() throws ConfigurationException {
        checkAzurePreconditions();
        if (this.module == null) {
            throw new ConfigurationException(message("function.deploy.validate.noModule"));
        }
        final FunctionAppConfig functionAppConfig = functionDeployModel.getFunctionAppConfig();
        if (StringUtils.isAllEmpty(functionAppConfig.getResourceId(), functionAppConfig.getName())) {
            throw new ConfigurationException(message("function.deploy.validate.noTarget"));
        }
        final Runtime runtime = functionAppConfig.getRuntime();
        final OperatingSystem operatingSystem = Optional.ofNullable(runtime).map(Runtime::getOperatingSystem).orElse(null);
        final JavaVersion javaVersion = Optional.ofNullable(runtime).map(Runtime::getJavaVersion).orElse(JavaVersion.OFF);
        if (operatingSystem == OperatingSystem.DOCKER) {
            throw new ConfigurationException(message("function.validate_deploy_configuration.dockerRuntime"));
        }
        if (functionAppConfig.getServicePlan() == null) {
            // Service plan could be null as lazy loading, throw exception in this case
            throw new ConfigurationException(message("function.validate_deploy_configuration.loading"));
        }
        if (Objects.equals(javaVersion, JavaVersion.OFF)) {
            throw new ConfigurationException(message("function.validate_deploy_configuration.invalidRuntime"));
        }
    }
    public Map<String, String> getAppSettings() {
        return Optional.ofNullable(functionDeployModel.getFunctionAppConfig()).map(FunctionAppConfig::getAppSettings).orElse(Collections.emptyMap());
    }

    public String getAppSettingsKey() {
        return functionDeployModel.getAppSettingsKey();
    }

    public String getFunctionId() {
        return Optional.ofNullable(functionDeployModel.getFunctionAppConfig()).map(FunctionAppConfig::getResourceId).orElse(StringUtils.EMPTY);
    }

    public String getAppName() {
        return Optional.ofNullable(functionDeployModel.getFunctionAppConfig()).map(FunctionAppConfig::getName).orElse(StringUtils.EMPTY);
    }

    public FunctionAppConfig getConfig() {
        return functionDeployModel.getFunctionAppConfig();
    }

    public void saveConfig(FunctionAppConfig config) {
        functionDeployModel.setFunctionAppConfig(config);
        FunctionUtils.saveAppSettingsToSecurityStorage(getAppSettingsKey(), config.getAppSettings());
    }

    public void setAppSettingsKey(String appSettingsKey) {
        functionDeployModel.setAppSettingsKey(appSettingsKey);
    }

    public void setFunctionId(String id) {
        functionDeployModel.getFunctionAppConfig().setResourceId(id);
    }

    public String getHostJsonPath() {
        return functionDeployModel.getHostJsonPath();
    }

    public void setHostJsonPath(String hostJsonPath) {
        functionDeployModel.setHostJsonPath(hostJsonPath);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void readExternal(Element element) throws InvalidDataException {
        final Element modelEle = element.getChild("FunctionDeployModel");
        if (Objects.isNull(this.functionDeployModel)) {
            this.functionDeployModel = new FunctionDeployModel();
        }
        if (Objects.nonNull(modelEle)) {
            try {
                final Element runtime = (Element) XPath.newInstance("option[@name='functionAppConfig']/FunctionAppConfig/option[@name='runtime']/Runtime").selectSingleNode(modelEle);
                if (runtime != null) {
                    runtime.detach();
                }
                this.functionDeployModel = XmlSerializer.deserialize(modelEle, FunctionDeployModel.class);
                if (runtime != null) {
                    final Attribute osAttr = (Attribute) XPath.newInstance("option[@name='operatingSystem']/@value").selectSingleNode(runtime);
                    Attribute javaAttr = (Attribute) XPath.newInstance("option[@name='javaVersion']/JavaVersion/option[@name='value']/@value").selectSingleNode(runtime);
                    if (Objects.isNull(javaAttr)) {
                        javaAttr = (Attribute) XPath.newInstance("option[@name='javaVersion']/@value").selectSingleNode(runtime);
                        if (ObjectUtils.allNotNull(osAttr, javaAttr)) {
                            final String os = osAttr.getValue();
                            final String java = javaAttr.getValue();
                            this.functionDeployModel.getFunctionAppConfig().setRuntime(FunctionAppRuntime.fromUserText(os, java));
                        }
                    }
                }
            } catch (final Throwable ignored) {
            }
        }
        Optional.ofNullable(this.getAppSettingsKey())
            .ifPresent(key -> functionDeployModel.getFunctionAppConfig().setAppSettings(FunctionUtils.loadAppSettingsFromSecurityStorage(getAppSettingsKey())));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        if (Objects.isNull(this.functionDeployModel)) {
            return;
        }
        final SerializationFilter filter = (accessor, o) -> !"appSettings".equalsIgnoreCase(accessor.getName()) && !"runtime".equalsIgnoreCase(accessor.getName());
        final Element modelEle = XmlSerializer.serialize(this.functionDeployModel, filter);
        final Runtime runtime = this.functionDeployModel.getFunctionAppConfig().getRuntime();
        if (Objects.nonNull(runtime)) {
            try {
                final Element configEle = (Element) XPath.newInstance("option[@name='functionAppConfig']/FunctionAppConfig").selectSingleNode(modelEle);
                configEle.addContent(
                    new Element("option").setAttribute("name", "runtime")
                        .addContent(new Element("Runtime")
                            .addContent(new Element("option").setAttribute("name", "operatingSystem").setAttribute("value", runtime.getOperatingSystem().getValue()))
                            .addContent(new Element("option").setAttribute("name", "javaVersion").setAttribute("value", runtime.getJavaVersion().toString()))));
            } catch (final Throwable t) {
                // ignore
            }
        }
        element.addContent(modelEle);
    }

    public void setAppSettings(Map<String, String> appSettings) {
        functionDeployModel.getFunctionAppConfig().setAppSettings(appSettings);
        functionDeployModel.setAppSettingsHash(DigestUtils.md5Hex(JsonUtils.toJson(appSettings)).toUpperCase());
        FunctionUtils.saveAppSettingsToSecurityStorage(getAppSettingsKey(), appSettings);
    }
}
