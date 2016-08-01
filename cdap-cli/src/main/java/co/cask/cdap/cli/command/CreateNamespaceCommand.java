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

package co.cask.cdap.cli.command;

import co.cask.cdap.cli.ArgumentName;
import co.cask.cdap.cli.CLIConfig;
import co.cask.cdap.cli.ElementType;
import co.cask.cdap.cli.util.AbstractCommand;
import co.cask.cdap.cli.util.ArgumentParser;
import co.cask.cdap.client.NamespaceClient;
import co.cask.cdap.proto.NamespaceMeta;
import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import com.google.inject.Inject;

import java.io.PrintStream;
import java.util.Map;

/**
 * {@link Command} to create a namespace.
 */
public class CreateNamespaceCommand extends AbstractCommand {
  private static final String SUCCESS_MSG = "Namespace '%s' created successfully.";

  private final NamespaceClient namespaceClient;
  private static final String NAMESPACE_PRINCIPAL = "principal";
  private static final String NAMESPACE_KEYTAB_PATH = "keytab-path";
  private static final String NAMESPACE_HBASE_NAMESPACE = "hbase-namespace";
  private static final String NAMESPACE_HIVE_DATABASE = "hive-database";
  private static final String NAMESPACE_ROOT_DIR = "root-dir";
  private static final String NAMESPACE_SCHEDULER_QUEUENAME = "queuename";

  @Inject
  public CreateNamespaceCommand(CLIConfig cliConfig, NamespaceClient namespaceClient) {
    super(cliConfig);
    this.namespaceClient = namespaceClient;
  }

  @Override
  public void perform(Arguments arguments, PrintStream output) throws Exception {
    String name = arguments.get(ArgumentName.NAMESPACE_NAME.toString());
    String description = arguments.get(ArgumentName.NAMESPACE_DESCRIPTION.toString(), "");
    String namespacePropertiesString = arguments.get(ArgumentName.NAMESPACE_PROPERTIES.toString(), "");
    Map<String, String> namespaceProperties = ArgumentParser.parseMap(namespacePropertiesString);

    String principal = namespaceProperties.get(NAMESPACE_PRINCIPAL);
    String keytabPath = namespaceProperties.get(NAMESPACE_KEYTAB_PATH);
    String hbaseNamespace = namespaceProperties.get(NAMESPACE_HBASE_NAMESPACE);
    String hiveDatabase = namespaceProperties.get(NAMESPACE_HIVE_DATABASE);
    String schedulerQueueName = namespaceProperties.get(NAMESPACE_SCHEDULER_QUEUENAME);
    String rootDir = namespaceProperties.get(NAMESPACE_ROOT_DIR);

    NamespaceMeta.Builder builder = new NamespaceMeta.Builder();
    builder.setName(name).setDescription(description).setPrincipal(principal).setKeytabURI(keytabPath)
      .setRootDirectory(rootDir).setHBaseNamespace(hbaseNamespace).setHiveDatabase(hiveDatabase)
      .setSchedulerQueueName(schedulerQueueName);
    namespaceClient.create(builder.build());
    output.println(String.format(SUCCESS_MSG, name));
  }

  @Override
  public String getPattern() {
    return String.format("create namespace <%s> [<%s>] [<%s>]",
                         ArgumentName.NAMESPACE_NAME, ArgumentName.NAMESPACE_DESCRIPTION,
                         ArgumentName.NAMESPACE_PROPERTIES);
  }

  @Override
  public String getDescription() {
    return String.format("Creates a %s in CDAP. <%s> is in the format 'key1=value1 key2=value2'. " +
                           "Valid property keys include [%s, %s, %s, %s, %s, %s]",
                         ElementType.NAMESPACE.getName(), ArgumentName.NAMESPACE_PROPERTIES,
                         NAMESPACE_PRINCIPAL, NAMESPACE_KEYTAB_PATH, NAMESPACE_HBASE_NAMESPACE,
                         NAMESPACE_HIVE_DATABASE, NAMESPACE_SCHEDULER_QUEUENAME, NAMESPACE_ROOT_DIR);
  }
}
