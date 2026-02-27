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

package org.apache.texera.amber.operator.loop

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle
import org.apache.texera.amber.core.executor.OpExecWithCode
import org.apache.texera.amber.core.tuple.Schema
import org.apache.texera.amber.core.virtualidentity.{ExecutionIdentity, WorkflowIdentity}
import org.apache.texera.amber.core.workflow.{InputPort, OutputPort, PhysicalOp, PortIdentity, SchemaPropagationFunc}
import org.apache.texera.amber.operator.LogicalOp
import org.apache.texera.amber.operator.metadata.{OperatorGroupConstants, OperatorInfo}

class LoopStartv2OpDesc extends LogicalOp {
  override def getPhysicalOp(
      workflowId: WorkflowIdentity,
      executionId: ExecutionIdentity
  ): PhysicalOp = {
    val pythonCode =
      try {
        generatePythonCode()
      } catch {
        case ex: Throwable =>
          s"#EXCEPTION DURING CODE GENERATION: ${ex.getMessage}"
      }
      PhysicalOp.oneToOnePhysicalOp(
        workflowId,
        executionId,
        operatorIdentifier,
        OpExecWithCode(pythonCode, "python")
      )
      .withInputPorts(operatorInfo.inputPorts)
      .withOutputPorts(operatorInfo.outputPorts)
      .withSuggestedWorkerNum(1)
      .withParallelizable(false)
  }

  @JsonSchemaTitle("Output columns")
  var lambdaAttributeUnits: List[LambdaAttributeUnit] = List()


  override def operatorInfo: OperatorInfo =
    OperatorInfo(
      "Python Table Reducer",
      "Reduce Table to Tuple",
      OperatorGroupConstants.PYTHON_GROUP,
      inputPorts = List(InputPort()),
      outputPorts = List(OutputPort())
    )

  def generatePythonCode(): String = {
    s"""
       |from pytexera import *
       |class ProcessTableOperator(UDFTableOperator):
       |
       |    @overrides
       |    def process_table(self, table: Table, port: int) -> Iterator[Optional[TableLike]]:
       |        yield table
       |""".stripMargin
  }
}
