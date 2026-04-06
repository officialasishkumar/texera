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

package org.apache.texera.amber.operator.source.scan

import org.apache.texera.amber.core.storage.FileResolver
import org.apache.texera.amber.core.tuple.{AttributeType, Schema, SchemaEnforceable, Tuple}
import org.apache.texera.amber.operator.TestOperators
import org.apache.texera.amber.util.JSONUtils.objectMapper
import org.scalatest.flatspec.AnyFlatSpec

class InputFileScanSourceOpDescSpec extends AnyFlatSpec {

  it should "require a filename input port" in {
    val desc = new InputFileScanSourceOpDesc()
    assert(desc.operatorInfo.inputPorts.length == 1)
    assert(desc.operatorInfo.inputPorts.head.displayName == "Filename")
  }

  it should "not expose fileName property in descriptor serialization" in {
    val desc = new InputFileScanSourceOpDesc()
    val descriptorJson = objectMapper.valueToTree[com.fasterxml.jackson.databind.JsonNode](desc)
    assert(!descriptorJson.has("fileName"))
  }

  it should "scan file content using filename from input tuple" in {
    val desc = new InputFileScanSourceOpDesc()
    desc.attributeType = FileAttributeType.SINGLE_STRING

    val executor = new InputFileScanSourceOpExec(objectMapper.writeValueAsString(desc))
    val inputTuple = Tuple(
      Schema().add("filename", AttributeType.STRING),
      Array(FileResolver.resolve(TestOperators.TestTextFilePath).toASCIIString)
    )

    executor.open()
    executor.processTupleMultiPort(inputTuple, 0)
    val tuples = executor
      .produceTuple()
      .map(tupleLike => tupleLike.asInstanceOf[SchemaEnforceable].enforceSchema(desc.sourceSchema()))
      .toSeq
    executor.close()

    assert(tuples.length == 1)
    assert(
      tuples.head
        .getField[String]("line")
        .equals("line1\nline2\nline3\nline4\nline5\nline6\nline7\nline8\nline9\nline10")
    )
  }

  it should "reject execution when no filename input tuple is provided" in {
    val desc = new InputFileScanSourceOpDesc()
    desc.attributeType = FileAttributeType.SINGLE_STRING

    val executor = new InputFileScanSourceOpExec(objectMapper.writeValueAsString(desc))
    executor.open()
    assertThrows[IllegalStateException](executor.produceTuple().toList)
    executor.close()
  }
}
