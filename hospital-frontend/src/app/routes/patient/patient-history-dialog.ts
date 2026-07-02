import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MtxGrid, MtxGridColumn } from '@ng-matero/extensions/grid';

import { PatientService } from './patient.service';
import { FIELD_LABELS, PatientHistoryItem, formatHistoryValue } from './patient.models';

export interface PatientHistoryDialogData {
  id: string;
  name: string;
}

@Component({
  selector: 'app-patient-history-dialog',
  templateUrl: './patient-history-dialog.html',
  imports: [MatDialogModule, MatButtonModule, MtxGrid],
})
export class PatientHistoryDialog {
  private readonly patientService = inject(PatientService);
  readonly data = inject<PatientHistoryDialogData>(MAT_DIALOG_DATA);

  readonly items = signal<PatientHistoryItem[]>([]);
  readonly total = signal(0);
  readonly loading = signal(true);

  page = 1;
  size = 10;

  readonly columns: MtxGridColumn<PatientHistoryItem>[] = [
    {
      header: 'Field',
      field: 'fieldName',
      formatter: row => FIELD_LABELS[row.fieldName] ?? row.fieldName,
    },
    {
      header: 'Old value',
      field: 'oldValue',
      formatter: row => formatHistoryValue(row.fieldName, row.oldValue),
    },
    {
      header: 'New value',
      field: 'newValue',
      formatter: row => formatHistoryValue(row.fieldName, row.newValue),
    },
    { header: 'Changed by', field: 'changedBy', width: '140px' },
    {
      header: 'Date',
      field: 'changedDate',
      type: 'date',
      typeParameter: { format: 'dd/MM/yyyy' },
      width: '120px',
    },
  ];

  constructor() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.patientService.getHistory(this.data.id, this.page, this.size).subscribe({
      next: res => {
        this.items.set(res.items);
        this.total.set(res.pagination.totalItems);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PageEvent) {
    this.page = e.pageIndex + 1;
    this.size = e.pageSize;
    this.load();
  }
}
