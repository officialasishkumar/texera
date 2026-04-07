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

import com.univocity.parsers.csv.{CsvFormat, CsvParser, CsvParserSettings}
import org.apache.texera.amber.core.storage.DocumentFactory
import org.apache.texera.amber.core.tuple.{AttributeTypeUtils, Schema, TupleLike}
import org.apache.texera.amber.operator.source.scan.{AutoClosingIterator, InputFileSourceOpExec}
import org.apache.texera.amber.util.JSONUtils.objectMapper

import java.io.InputStreamReader
import java.net.URI
import scala.collection.immutable.ArraySeq

class InputCSVScanSourceOpExec private[csv] (descString: String) extends InputFileSourceOpExec {
  private val desc: InputCSVScanSourceOpDesc =
    objectMapper.readValue(descString, classOf[InputCSVScanSourceOpDesc])
  private val schema: Schema = desc.sourceSchema()

  override def produceTuple(): Iterator[TupleLike] = {
    resolvedInputFileNames.iterator.flatMap(produceTuplesForFile)
  }

  private def produceTuplesForFile(resolvedFileName: String): Iterator[TupleLike] = {
    val inputReader = new InputStreamReader(
      DocumentFactory.openReadonlyDocument(new URI(resolvedFileName)).asInputStream(),
      desc.fileEncoding.getCharset
    )

    val csvFormat = new CsvFormat()
    csvFormat.setDelimiter(desc.customDelimiter.get.charAt(0))
    csvFormat.setLineSeparator("\n")
    csvFormat.setComment(
      '\u0000'
    ) // disable skipping lines starting with # (default comment character)
    val csvSetting = new CsvParserSettings()
    csvSetting.setMaxCharsPerColumn(-1)
    csvSetting.setFormat(csvFormat)
    csvSetting.setHeaderExtractionEnabled(desc.hasHeader)

    val parser = new CsvParser(csvSetting)
    parser.beginParsing(inputReader)

    val rowIterator = new Iterator[Array[String]] {
      private var nextRow: Array[String] = _

      override def hasNext: Boolean = {
        if (nextRow != null) {
          return true
        }
        nextRow = parser.parseNext()
        nextRow != null
      }

      override def next(): Array[String] = {
        val ret = nextRow
        nextRow = null
        ret
      }
    }

    var tupleIterator = rowIterator
      .drop(desc.offset.getOrElse(0))
      .map(row => {
        try {
          TupleLike(
            ArraySeq.unsafeWrapArray(
              AttributeTypeUtils.parseFields(row.asInstanceOf[Array[Any]], schema)
            ): _*
          )
        } catch {
          case _: Throwable => null
        }
      })
      .filter(t => t != null)

    if (desc.limit.isDefined) {
      tupleIterator = tupleIterator.take(desc.limit.get)
    }

    new AutoClosingIterator(
      tupleIterator,
      () => {
        parser.stopParsing()
        inputReader.close()
      }
    )
  }
}
