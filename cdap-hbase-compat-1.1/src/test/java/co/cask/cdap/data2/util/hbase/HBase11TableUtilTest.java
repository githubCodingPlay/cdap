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

package co.cask.cdap.data2.util.hbase;

import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.data2.util.TableId;
import co.cask.cdap.proto.Id;
import co.cask.cdap.test.XSlowTests;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.junit.experimental.categories.Category;

/**
 *
 */
@Category(XSlowTests.class)
public class HBase11TableUtilTest extends AbstractHBaseTableUtilTest {

  private final HTableNameConverter nameConverter = new HTable11NameConverter();

  @Override
  protected HBaseTableUtil getTableUtil() {
    HBase11TableUtil hBaseTableUtil = new HBase11TableUtil();
    hBaseTableUtil.setCConf(cConf);
    hBaseTableUtil.setNamespaceQueryAdmin(getNamespaceQueryAdmin());
    return hBaseTableUtil;
  }

  @Override
  protected HTableNameConverter getNameConverter() {
    return nameConverter;
  }

  @Override
  protected String getTableNameAsString(TableId tableId) {
    Preconditions.checkArgument(tableId != null, "TableId should not be null.");
    String tablePrefix = cConf.get(Constants.Dataset.TABLE_PREFIX);
    if (Id.Namespace.DEFAULT.getId().equals(tableId.getNamespace())) {
      return nameConverter.toHBaseTableName(tablePrefix, tableId);
    }
    return Joiner.on(':').join(tableId.getNamespace(), nameConverter.toHBaseTableName(tablePrefix, tableId));
  }

  @Override
  protected boolean namespacesSupported() {
    return true;
  }
}
