/**
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

import { Component, OnInit } from "@angular/core";
import { FieldType, FieldTypeConfig } from "@ngx-formly/core";
import { UntilDestroy, untilDestroyed } from "@ngneat/until-destroy";
import { DashboardDataset } from "../../../dashboard/type/dashboard-dataset.interface";
import { DatasetVersion } from "../../../common/type/dataset";
import { DatasetService } from "../../../dashboard/service/user/dataset/dataset.service";

@UntilDestroy()
@Component({
  selector: "texera-dataset-version-selector-template",
  templateUrl: "./dataset-version-selector.component.html",
  styleUrls: ["./dataset-version-selector.component.scss"],
})
export class DatasetVersionSelectorComponent extends FieldType<FieldTypeConfig> implements OnInit {
  datasets: ReadonlyArray<DashboardDataset> = [];
  datasetVersions: ReadonlyArray<DatasetVersion> = [];
  selectedDataset?: DashboardDataset;
  selectedVersion?: DatasetVersion;
  isLoadingDatasets = false;
  isLoadingVersions = false;

  constructor(private datasetService: DatasetService) {
    super();
  }

  ngOnInit(): void {
    this.loadDatasets();
  }

  private loadDatasets(): void {
    this.isLoadingDatasets = true;
    this.datasetService
      .retrieveAccessibleDatasets()
      .pipe(untilDestroyed(this))
      .subscribe(datasets => {
        this.datasets = datasets;
        this.isLoadingDatasets = false;
        this.restoreSelectionFromValue();
      });
  }

  private restoreSelectionFromValue(): void {
    const parsed = this.parseDatasetVersionPath(this.formControl.value);
    if (!parsed) {
      return;
    }

    this.selectedDataset = this.datasets.find(
      dataset =>
        dataset.ownerEmail === parsed.ownerEmail && dataset.dataset.name === parsed.datasetName
    );

    if (this.selectedDataset?.dataset.did !== undefined) {
      this.loadVersions(this.selectedDataset.dataset.did, parsed.versionName);
    }
  }

  onDatasetChange(): void {
    this.selectedVersion = undefined;
    this.datasetVersions = [];
    this.formControl.setValue(null);

    if (this.selectedDataset?.dataset.did !== undefined) {
      this.loadVersions(this.selectedDataset.dataset.did);
    }
  }

  onVersionChange(): void {
    if (!this.selectedDataset || !this.selectedVersion) {
      this.formControl.setValue(null);
      return;
    }

    this.formControl.setValue(
      `/${this.selectedDataset.ownerEmail}/${this.selectedDataset.dataset.name}/${this.selectedVersion.name}`
    );
  }

  private loadVersions(did: number, versionNameToSelect?: string): void {
    this.isLoadingVersions = true;
    this.datasetService
      .retrieveDatasetVersionList(did)
      .pipe(untilDestroyed(this))
      .subscribe(versions => {
        this.datasetVersions = versions;
        this.isLoadingVersions = false;
        if (versionNameToSelect) {
          this.selectedVersion = versions.find(version => version.name === versionNameToSelect);
        } else if (versions.length > 0) {
          this.selectedVersion = versions[0];
        }
        if (this.selectedVersion) {
          this.onVersionChange();
        }
      });
  }

  private parseDatasetVersionPath(
    path: string | null | undefined
  ): { ownerEmail: string; datasetName: string; versionName: string } | undefined {
    const parts = (path ?? "")
      .split("/")
      .filter(Boolean);
    if (parts.length !== 3) {
      return undefined;
    }
    const [ownerEmail, datasetName, versionName] = parts;
    return { ownerEmail, datasetName, versionName };
  }
}
