/*
 * Copyright © 2015 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.internal.flow;

import co.cask.cdap.api.Resources;
import co.cask.cdap.api.data.stream.Stream;
import co.cask.cdap.api.data.stream.StreamSpecification;
import co.cask.cdap.api.dataset.Dataset;
import co.cask.cdap.api.dataset.DatasetCreationSpec;
import co.cask.cdap.api.dataset.DatasetProperties;
import co.cask.cdap.api.dataset.module.DatasetModule;
import co.cask.cdap.api.flow.flowlet.FailurePolicy;
import co.cask.cdap.api.flow.flowlet.Flowlet;
import co.cask.cdap.api.flow.flowlet.FlowletConfigurer;
import co.cask.cdap.api.flow.flowlet.FlowletSpecification;
import co.cask.cdap.internal.flowlet.DefaultFlowletSpecification;
import co.cask.cdap.internal.lang.Reflections;
import co.cask.cdap.internal.specification.PropertyFieldExtractor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link FlowletConfigurer}.
 */
public class DefaultFlowletConfigurer implements FlowletConfigurer {

  private final String className;
  private final Map<String, String> propertyFields;
  private final Map<String, StreamSpecification> streams;
  private final Map<String, String> dataSetModules;
  private final Map<String, DatasetCreationSpec> dataSetInstances;

  private String name;
  private String description;
  private Resources resources;
  private FailurePolicy failurePolicy;
  private Map<String, String> properties;
  private Set<String> datasets;

  public DefaultFlowletConfigurer(Flowlet flowlet) {
    this.name = flowlet.getClass().getSimpleName();
    this.className = flowlet.getClass().getName();
    this.failurePolicy = FailurePolicy.RETRY;
    this.propertyFields = Maps.newHashMap();
    this.description = "";
    this.resources = new Resources();
    this.properties = ImmutableMap.of();
    this.datasets = Sets.newHashSet();
    this.streams = Maps.newHashMap();
    this.dataSetModules = Maps.newHashMap();
    this.dataSetInstances = Maps.newHashMap();

    // Grab all @Property fields
    Reflections.visit(flowlet, TypeToken.of(flowlet.getClass()), new PropertyFieldExtractor(propertyFields));
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public void setResources(Resources resources) {
    Preconditions.checkArgument(resources != null, "Resources cannot be null.");
    this.resources = resources;
  }

  @Override
  public void setFailurePolicy(FailurePolicy failurePolicy) {
    Preconditions.checkArgument(failurePolicy != null, "FailurePolicy cannot be null");
    this.failurePolicy = failurePolicy;
  }

  @Override
  public void setProperties(Map<String, String> properties) {
    this.properties = ImmutableMap.copyOf(properties);
  }

  @Override
  public void useDatasets(Iterable<String> datasets) {
    Iterables.addAll(this.datasets, datasets);
  }

  public FlowletSpecification createSpecification() {
    Map<String, String> properties = Maps.newHashMap(this.properties);
    properties.putAll(propertyFields);
    return new DefaultFlowletSpecification(this.className, this.name, this.description, this.failurePolicy,
                                           this.datasets, this.properties, this.resources, this.streams,
                                           this.dataSetModules, this.dataSetInstances);
  }

  @Override
  public void addStream(Stream stream) {
    Preconditions.checkArgument(stream != null, "Stream cannot be null.");
    StreamSpecification spec = stream.configure();
    streams.put(spec.getName(), spec);
  }

  @Override
  public void addDatasetModule(String moduleName, Class<? extends DatasetModule> moduleClass) {
    Preconditions.checkArgument(moduleName != null, "Dataset module name cannot be null.");
    Preconditions.checkArgument(moduleClass != null, "Dataset module class cannot be null.");
    dataSetModules.put(moduleName, moduleClass.getName());
  }

  @Override
  public void addDatasetType(Class<? extends Dataset> datasetClass) {
    Preconditions.checkArgument(datasetClass != null, "Dataset class cannot be null.");
    dataSetModules.put(datasetClass.getName(), datasetClass.getName());
  }

  @Override
  public void createDataset(String datasetInstanceName, String typeName, DatasetProperties properties) {
    Preconditions.checkArgument(datasetInstanceName != null, "Dataset instance name cannot be null.");
    Preconditions.checkArgument(typeName != null, "Dataset type name cannot be null.");
    Preconditions.checkArgument(properties != null, "Instance properties name cannot be null.");
    dataSetInstances.put(datasetInstanceName,
                         new DatasetCreationSpec(datasetInstanceName, typeName, properties));
  }

  @Override
  public void createDataset(String datasetInstanceName, Class<? extends Dataset> datasetClass,
                            DatasetProperties properties) {
    Preconditions.checkArgument(datasetInstanceName != null, "Dataset instance name cannot be null.");
    Preconditions.checkArgument(datasetClass != null, "Dataset class name cannot be null.");
    Preconditions.checkArgument(properties != null, "Instance properties name cannot be null.");
    dataSetInstances.put(datasetInstanceName,
                         new DatasetCreationSpec(datasetInstanceName, datasetClass.getName(), properties));
    dataSetModules.put(datasetClass.getName(), datasetClass.getName());
  }
}