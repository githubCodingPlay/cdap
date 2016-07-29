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

package co.cask.cdap.etl.spark.batch;

import co.cask.cdap.api.data.DatasetContext;
import co.cask.cdap.api.spark.JavaSparkExecutionContext;
import co.cask.cdap.api.spark.SparkClientContext;
import co.cask.cdap.etl.api.LookupProvider;
import co.cask.cdap.etl.batch.AbstractAggregatorContext;
import co.cask.cdap.etl.common.DatasetContextLookupProvider;

/**
 * Spark Aggregator Context.
 */
public class SparkAggregatorContext extends AbstractAggregatorContext {

  public SparkAggregatorContext(SparkClientContext context, LookupProvider lookup, String stageName) {
    super(context, context, context.getMetrics(),
          lookup, stageName, context.getLogicalStartTime(), context.getRuntimeArguments(), context.getAdmin());
  }

  public SparkAggregatorContext(JavaSparkExecutionContext sec, DatasetContext datasetContext,
                                String stageName, long logicalStartTime) {
    super(sec.getPluginContext(), datasetContext, sec.getMetrics(),
          new DatasetContextLookupProvider(datasetContext), stageName,
          logicalStartTime, sec.getRuntimeArguments(), sec.getAdmin());
  }

  @Override
  public <T> T getHadoopJob() {
    throw new UnsupportedOperationException("Hadoop Job is not available in Spark");
  }
}
