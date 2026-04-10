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

package org.apache.texera.amber.operator.source.dataset

import io.lakefs.clients.sdk.model.ObjectStats
import org.apache.texera.amber.core.executor.SourceOperatorExecutor
import org.apache.texera.amber.core.storage.util.LakeFSStorageClient
import org.apache.texera.amber.core.tuple.TupleLike
import org.apache.texera.amber.util.JSONUtils.objectMapper
import org.apache.texera.dao.SqlServer
import org.apache.texera.dao.SqlServer.withTransaction
import org.apache.texera.dao.jooq.generated.tables.Dataset.DATASET
import org.apache.texera.dao.jooq.generated.tables.DatasetVersion.DATASET_VERSION
import org.apache.texera.dao.jooq.generated.tables.User.USER
import org.apache.texera.dao.jooq.generated.tables.pojos.{Dataset, DatasetVersion}

class DatasetSelectorSourceOpExec private[dataset] (descString: String) extends SourceOperatorExecutor {
  private val desc: DatasetSelectorSourceOpDesc =
    objectMapper.readValue(descString, classOf[DatasetSelectorSourceOpDesc])

  override def produceTuple(): Iterator[TupleLike] = {
    DatasetSelectorSourceOpExec
      .listFileNames(desc.datasetVersionPath)
      .iterator
      .map(fileName => TupleLike("filename" -> fileName))
  }
}

object DatasetSelectorSourceOpExec {


  private def isRealFile(obj: ObjectStats): Boolean = {
    val path = Option(obj.getPath).getOrElse("").trim
    path.nonEmpty && !path.endsWith("/")
  }

  def listFileNames(datasetVersionPath: String): Seq[String] = {
    val Array(ownerEmail, datasetName, versionName) = datasetVersionPath.trim.split("/")
    val (dataset, datasetVersion) = resolveDatasetVersion(ownerEmail, datasetName, versionName)
    val versionPrefix = s"/$ownerEmail/$datasetName/$versionName"
    LakeFSStorageClient
      .retrieveObjectsOfVersion(dataset.getRepositoryName, datasetVersion.getVersionHash)
      .iterator
      .filter(isRealFile)
      .toSeq
      .sortBy(_.getPath)
      .map(obj => s"$versionPrefix/${obj.getPath}")
  }

  private def resolveDatasetVersion(
      ownerEmail: String,
      datasetName: String,
      versionName: String
  ): (Dataset, DatasetVersion) =
    withTransaction(SqlServer.getInstance().createDSLContext()) { ctx =>
      val dataset = ctx
        .select(DATASET.fields: _*)
        .from(DATASET)
        .leftJoin(USER)
        .on(USER.UID.eq(DATASET.OWNER_UID))
        .where(USER.EMAIL.eq(ownerEmail))
        .and(DATASET.NAME.eq(datasetName))
        .fetchOneInto(classOf[Dataset])


      val datasetVersion = ctx
        .selectFrom(DATASET_VERSION)
        .where(DATASET_VERSION.DID.eq(dataset.getDid))
        .and(DATASET_VERSION.NAME.eq(versionName))
        .fetchOneInto(classOf[DatasetVersion])

      (dataset, datasetVersion)
    }
}
