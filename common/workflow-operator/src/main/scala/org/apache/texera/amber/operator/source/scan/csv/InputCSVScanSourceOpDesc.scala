/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.texera.amber.operator.source.scan.csv

import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty, JsonPropertyDescription}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle
import org.apache.texera.amber.core.executor.OpExecWithClassName
import org.apache.texera.amber.core.storage.FileResolver
import org.apache.texera.amber.core.tuple.{Attribute, Schema}
import org.apache.texera.amber.core.virtualidentity.{ExecutionIdentity, WorkflowIdentity}
import org.apache.texera.amber.core.workflow.{
  InputPort,
  OutputPort,
  PhysicalOp,
  SchemaPropagationFunc
}
import org.apache.texera.amber.operator.metadata.{OperatorGroupConstants, OperatorInfo}
import org.apache.texera.amber.operator.source.SourceOperatorDescriptor
import org.apache.texera.amber.operator.source.scan.FileDecodingMethod
import org.apache.texera.amber.util.JSONUtils.objectMapper

class InputCSVScanSourceOpDesc extends SourceOperatorDescriptor {

  @JsonProperty(defaultValue = "UTF_8", required = true)
  @JsonSchemaTitle("Encoding")
  var fileEncoding: FileDecodingMethod = FileDecodingMethod.UTF_8

  @JsonProperty(defaultValue = ",")
  @JsonSchemaTitle("Delimiter")
  @JsonPropertyDescription("delimiter to separate each line into fields")
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  var customDelimiter: Option[String] = None

  @JsonProperty(defaultValue = "true")
  @JsonSchemaTitle("Header")
  @JsonPropertyDescription("whether the CSV file contains a header line")
  var hasHeader: Boolean = true

  @JsonProperty()
  @JsonSchemaTitle("Limit")
  @JsonPropertyDescription("max output count")
  @JsonDeserialize(contentAs = classOf[Int])
  var limit: Option[Int] = None

  @JsonProperty()
  @JsonSchemaTitle("Offset")
  @JsonPropertyDescription("starting point of output")
  @JsonDeserialize(contentAs = classOf[Int])
  var offset: Option[Int] = None

  @JsonProperty()
  @JsonSchemaTitle("Columns")
  @JsonPropertyDescription("output columns of the CSV files")
  var columns: List[Attribute] = List.empty

  override def getPhysicalOp(
      workflowId: WorkflowIdentity,
      executionId: ExecutionIdentity
  ): PhysicalOp = {
    require(
      columns != null && columns.nonEmpty,
      "Columns must not be empty. Use a Text Input with a literal filename or configure Columns manually."
    )
    if (customDelimiter.isEmpty || customDelimiter.get.isEmpty) {
      customDelimiter = Option(",")
    }

    PhysicalOp
      .sourcePhysicalOp(
        workflowId,
        executionId,
        operatorIdentifier,
        OpExecWithClassName(
          "org.apache.texera.amber.operator.source.scan.csv.InputCSVScanSourceOpExec",
          objectMapper.writeValueAsString(this)
        )
      )
      .withInputPorts(operatorInfo.inputPorts)
      .withOutputPorts(operatorInfo.outputPorts)
      .withPropagateSchema(
        SchemaPropagationFunc(_ => Map(operatorInfo.outputPorts.head.id -> sourceSchema()))
      )
  }

  override def sourceSchema(): Schema = {
    if (columns != null && columns.nonEmpty) {
      Schema().add(columns)
    } else {
      Schema()
    }
  }

  def inferColumnsFromFileName(fileName: String): Unit = {
    if (customDelimiter.isEmpty || customDelimiter.get.isEmpty) {
      customDelimiter = Option(",")
    }

    val csvScanSourceOpDesc = new CSVScanSourceOpDesc()
    csvScanSourceOpDesc.customDelimiter = customDelimiter
    csvScanSourceOpDesc.hasHeader = hasHeader
    csvScanSourceOpDesc.fileEncoding = fileEncoding
    csvScanSourceOpDesc.limit = limit
    csvScanSourceOpDesc.setResolvedFileName(FileResolver.resolve(fileName))
    columns = csvScanSourceOpDesc.sourceSchema().getAttributes
  }

  override def operatorInfo: OperatorInfo =
    OperatorInfo(
      userFriendlyName = "CSV File Scan From Input",
      operatorDescription = "Scan CSV files from file paths provided by input tuples",
      operatorGroupName = OperatorGroupConstants.INPUT_GROUP,
      inputPorts = List(InputPort(displayName = "Filename")),
      outputPorts = List(OutputPort())
    )
}
