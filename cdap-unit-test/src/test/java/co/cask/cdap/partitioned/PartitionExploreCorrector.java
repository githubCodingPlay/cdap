/*
 * Copyright © 2016 Cask Data, Inc.
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

package co.cask.cdap.partitioned;

import co.cask.cdap.api.app.AbstractApplication;
import co.cask.cdap.api.dataset.lib.partitioned.PartitionExploreCorrector.PartitionWorker;

public class PartitionExploreCorrector extends AbstractApplication {

  @Override
  public void configure() {
    setDescription("An app to correct the Explore state of a partitioned file set. Run the worker with " +
                     "dataset.name=<name> [ verbose=<boolean> ] [ batch.size=<int> ] [ disable.explore=<boolean> ]");
    addWorker(new PartitionWorker());
  }
}
