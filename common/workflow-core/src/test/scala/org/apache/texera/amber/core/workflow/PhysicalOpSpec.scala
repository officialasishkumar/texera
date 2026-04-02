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

package org.apache.texera.amber.core.workflow

import org.apache.texera.amber.core.executor.OpExecWithClassName
import org.apache.texera.amber.core.tuple.{AttributeType, Schema}
import org.apache.texera.amber.core.virtualidentity.{ExecutionIdentity, OperatorIdentity, WorkflowIdentity}
import org.scalatest.flatspec.AnyFlatSpec

class PhysicalOpSpec extends AnyFlatSpec {

  it should "propagate output schema when an input port is unlinked" in {
    val expectedSchema = Schema().add("filename", AttributeType.STRING)
    val physicalOp = PhysicalOp
      .sourcePhysicalOp(
        WorkflowIdentity(1),
        ExecutionIdentity(1),
        OperatorIdentity("scan-source"),
        OpExecWithClassName("test.OpExec", "{}")
      )
      .withInputPorts(List(InputPort(displayName = "Filename")))
      .withOutputPorts(List(OutputPort()))
      .withPropagateSchema(
        SchemaPropagationFunc(_ => Map(PortIdentity(0) -> expectedSchema))
      )

    val propagated = physicalOp.propagateSchema()
    val outputSchema = propagated.outputPorts(PortIdentity(0))._3.toOption

    assert(outputSchema.contains(expectedSchema))
  }
}
