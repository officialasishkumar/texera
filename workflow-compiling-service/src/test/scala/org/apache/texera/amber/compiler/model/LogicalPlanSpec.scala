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

package org.apache.texera.amber.compiler.model

import org.apache.texera.amber.core.virtualidentity.OperatorIdentity
import org.apache.texera.amber.core.workflow.PortIdentity
import org.apache.texera.amber.operator.source.scan.InputFileScanSourceOpDesc
import org.apache.texera.amber.operator.source.scan.text.TextInputSourceOpDesc
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable.ArrayBuffer

class LogicalPlanSpec extends AnyFlatSpec {

  it should "skip file resolution when a scan source receives filename from input" in {
    val scanSource = new InputFileScanSourceOpDesc()
    scanSource.setOperatorId("scan-source")

    val upstream = new TextInputSourceOpDesc()
    upstream.setOperatorId("filename-source")

    val logicalPlan = LogicalPlan(
      operators = List(upstream, scanSource),
      links = List(
        LogicalLink(
          fromOpId = OperatorIdentity("filename-source"),
          fromPortId = PortIdentity(0),
          toOpId = OperatorIdentity("scan-source"),
          toPortId = PortIdentity(0)
        )
      )
    )

    val errors = ArrayBuffer.empty[(OperatorIdentity, Throwable)]
    logicalPlan.resolveScanSourceOpFileName(Some(errors))

    assert(errors.isEmpty)
    assert(scanSource.fileName.isEmpty)
  }
}
