import { Component, OnInit, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MtxGrid, MtxGridColumn } from '@ng-matero/extensions/grid';

import { PatientDetailDialog } from './patient-detail-dialog';
import { FIELD_LABELS, PatientHistoryEntry, formatHistoryValue } from './patient.models';
import { PatientService } from './patient.service';

@Component({
  selector: 'app-patient-history-page',
  templateUrl: './patient-history-page.html',
  imports: [MatCardModule, MtxGrid],
})
export class PatientHistoryPage implements OnInit {
  private readonly patientService = inject(PatientService);
  private readonly dialog = inject(MatDialog);

  readonly items = signal<PatientHistoryEntry[]>([]);
  readonly total = signal(0);
  readonly loading = signal(true);

  page = 1;
  size = 20;

  readonly columns: MtxGridColumn<PatientHistoryEntry>[] = [
    { header: 'Patient', field: 'patientName', width: '180px' },
    {
      header: 'Changed field',
      field: 'fieldName',
      formatter: row => FIELD_LABELS[row.fieldName] ?? row.fieldName,
      width: '150px',
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
      width: '110px',
    },
    {
      header: 'Actions',
      field: 'operation',
      width: '90px',
      type: 'button',
      buttons: [
        {
          type: 'icon',
          icon: 'visibility',
          tooltip: 'View patient',
          click: row => this.openDetail(row),
        },
      ],
    },
  ];

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.patientService.getAllHistory(this.page, this.size).subscribe({
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

  openDetail(row: PatientHistoryEntry) {
    this.dialog.open(PatientDetailDialog, {
      width: '1080px',
      maxWidth: '95vw',
      autoFocus: false,
      data: { id: row.patientId },
    });
  }
}
