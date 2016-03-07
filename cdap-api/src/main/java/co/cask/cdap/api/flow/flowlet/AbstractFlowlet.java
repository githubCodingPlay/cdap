/*
 * Copyright © 2014-2016 Cask Data, Inc.
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

package co.cask.cdap.api.flow.flowlet;

import co.cask.cdap.api.Resources;
import co.cask.cdap.api.dataset.Dataset;
import co.cask.cdap.internal.api.AbstractProgramDatasetConfigurable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * This abstract class provides a default implementation of {@link Flowlet} methods for easy extension.
 */
public abstract class AbstractFlowlet extends AbstractProgramDatasetConfigurable<FlowletConfigurer>
  implements Flowlet, Callback {

  private FlowletConfigurer configurer;
  private FlowletContext flowletContext;

  @Override
  public void configure(FlowletConfigurer configurer) {
    this.configurer = configurer;
    configure();
  }

  /**
   * Configure the {@link Flowlet}
   */
  protected void configure() {

  }

  /**
   * Returns the {@link FlowletConfigurer} used for configuration. Only available during configuration time.
   */
  @Override
  protected final FlowletConfigurer getConfigurer() {
    return configurer;
  }

  /**
   * Sets the name of the {@link Flowlet}.
   *
   * @param name the name of the flowlet
   */
  protected void setName(String name) {
    configurer.setName(name);
  }

  /**
   * Sets the description of the {@link Flowlet}.
   *
   * @param description the description of the flowlet
   */
  protected void setDescription(String description) {
    configurer.setDescription(description);
  }

  /**
   * Sets the resources requirements of the {@link Flowlet}.
   *
   * @param resources {@link Resources} requirements
   */
  protected void setResources(Resources resources) {
    configurer.setResources(resources);
  }

  /**
   * Sets the failure policy of the {@link Flowlet}.
   *
   * @param failurePolicy {@link FailurePolicy}
   */
  protected void setFailurePolicy(FailurePolicy failurePolicy) {
    configurer.setFailurePolicy(failurePolicy);
  }

  /**
   * Sets a set of properties that will be available through the {@link FlowletSpecification#getProperties()}.
   *
   * @param properties the properties to set
   */
  protected void setProperties(Map<String, String> properties) {
    configurer.setProperties(properties);
  }

  /**
   * Adds the names of {@link Dataset}s used by the Flowlet.
   *
   * @param dataset dataset name
   * @param datasets more dataset names
   * @deprecated Deprecated as of 3.4.0. Dataset can be requested directly through the method
   *             {@link FlowletContext#getDataset(String)} at runtime.
   */
  @Deprecated
  protected void useDatasets(String dataset, String...datasets) {
    List<String> datasetList = new ArrayList<>();
    datasetList.add(dataset);
    datasetList.addAll(Arrays.asList(datasets));
    useDatasets(datasetList);
  }

  /**
   * Adds the names of {@link Dataset}s used by the Flowlet.
   *
   * @param datasets dataset names
   * @deprecated Deprecated as of 3.4.0. Dataset can be requested directly through the method
   *             {@link FlowletContext#getDataset(String)} at runtime.
   */
  @Deprecated
  protected void useDatasets(Iterable<String> datasets) {
    configurer.useDatasets(datasets);
  }

  @Override
  public void initialize(FlowletContext context) throws Exception {
    this.flowletContext = context;
  }

  @Override
  public void destroy() {
    // Nothing to do.
  }

  @Override
  public void onSuccess(@Nullable Object input, @Nullable InputContext inputContext) {
    // No-op by default
  }

  @Override
  public FailurePolicy onFailure(@Nullable Object input, @Nullable InputContext inputContext, FailureReason reason) {
    // Return the policy as specified in the spec
    return flowletContext.getSpecification().getFailurePolicy();
  }

  /**
   * @return An instance of {@link FlowletContext} when this flowlet is running. Otherwise return
   *         {@code null} if it is not running or not yet initialized by the runtime environment.
   */
  protected final FlowletContext getContext() {
    return flowletContext;
  }
}
