import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MtxDialog } from '@ng-matero/extensions/dialog';
import { MtxGrid, MtxGridColumn } from '@ng-matero/extensions/grid';
import { HotToastService } from '@ngxpert/hot-toast';
import { debounceTime } from 'rxjs';

import { PatientDetailDialog } from './patient-detail-dialog';
import { PatientFormDialog } from './patient-form-dialog';
import { PatientHistoryDialog } from './patient-history-dialog';
import {
  GENDER_OPTIONS,
  PATIENT_STATUS_OPTIONS,
  PatientListItem,
  lookupLabel,
} from './patient.models';
import { PatientService } from './patient.service';

@Component({
  selector: 'app-patient-list',
  templateUrl: './patient-list.html',
  styleUrl: './patient-list.scss',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatSelectModule,
    MtxGrid,
  ],
})
export class PatientList implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly dialog = inject(MatDialog);
  private readonly mtxDialog = inject(MtxDialog);
  private readonly toast = inject(HotToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly genderOptions = GENDER_OPTIONS;
  readonly statusOptions = PATIENT_STATUS_OPTIONS;
  readonly searchFieldOptions = [
    { value: 'name', label: 'Name' },
    { value: 'phone', label: 'Phone' },
    { value: 'identity', label: 'ID card' },
  ];

  readonly filterForm = this.fb.group({
    searchField: 'name',
    search: '',
    status: null as number | null,
    gender: null as number | null,
  });

  readonly list = signal<PatientListItem[]>([]);
  readonly total = signal(0);
  readonly loading = signal(true);
  readonly selected = signal<PatientListItem[]>([]);

  page = 1;
  size = 20;

  readonly columns: MtxGridColumn<PatientListItem>[] = [
    { header: 'Patient ID', field: 'id', width: '110px' },
    { header: 'Full name', field: 'name' },
    {
      header: 'Gender',
      field: 'gender',
      width: '100px',
      formatter: row => lookupLabel(GENDER_OPTIONS, row.gender),
    },
    {
      header: 'Date of birth',
      field: 'birthDate',
      type: 'date',
      typeParameter: { format: 'dd/MM/yyyy' },
      width: '120px',
    },
    { header: 'Phone', field: 'phoneNo', width: '140px' },
    {
      header: 'Status',
      field: 'status',
      width: '130px',
      formatter: row => lookupLabel(PATIENT_STATUS_OPTIONS, row.status),
    },
    {
      header: 'Actions',
      field: 'operation',
      width: '120px',
      pinned: 'right',
      type: 'button',
      buttons: [
        {
          type: 'icon',
          icon: 'menu',
          tooltip: 'Actions',
          children: [
            { text: 'View details', icon: 'visibility', click: row => this.openDetail(row) },
            { text: 'Edit', icon: 'edit', click: row => this.openEdit(row) },
            { text: 'Delete', icon: 'delete', click: row => this.delete(row) },
          ],
        },
        {
          type: 'icon',
          icon: 'history',
          tooltip: 'Change history',
          // Lit only when the patient actually has recorded changes.
          disabled: row => !row.hasHistory,
          click: row => this.openHistory(row),
        },
      ],
    },
  ];

  ngOnInit() {
    this.load();
    // No explicit search button: the text box filters after a short typing
    // pause, the selects immediately on change.
    this.filterForm.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.applyFilter());
  }

  load() {
    this.loading.set(true);
    this.selected.set([]);
    const f = this.filterForm.getRawValue();
    this.patientService
      .list({
        page: this.page,
        size: this.size,
        search: f.search ?? '',
        searchField: f.searchField ?? 'name',
        status: f.status,
        gender: f.gender,
      })
      .subscribe({
        next: res => {
          this.list.set(res.items);
          this.total.set(res.pagination.totalItems);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  get searchFieldLabel(): string {
    const value = this.filterForm.controls.searchField.value;
    return this.searchFieldOptions.find(o => o.value === value)?.label ?? 'Name';
  }

  setSearchField(value: string) {
    this.filterForm.controls.searchField.setValue(value);
  }

  applyFilter() {
    this.page = 1;
    this.load();
  }

  onPage(e: PageEvent) {
    this.page = e.pageIndex + 1;
    this.size = e.pageSize;
    this.load();
  }

  onSelectionChange(rows: PatientListItem[]) {
    this.selected.set(rows);
  }

  openCreate() {
    this.dialog
      .open(PatientFormDialog, { width: '1080px', maxWidth: '95vw', autoFocus: false, data: {} })
      .afterClosed()
      .subscribe(saved => saved && this.load());
  }

  openEdit(row: PatientListItem) {
    // The list row only carries summary fields, so fetch the full record first.
    this.patientService.getDetail(row.id).subscribe(patient => {
      this.dialog
        .open(PatientFormDialog, {
          width: '1080px',
          maxWidth: '95vw',
          autoFocus: false,
          data: { patient },
        })
        .afterClosed()
        .subscribe(saved => saved && this.load());
    });
  }

  openDetail(row: PatientListItem) {
    this.dialog.open(PatientDetailDialog, {
      width: '1080px',
      maxWidth: '95vw',
      autoFocus: false,
      data: { id: row.id },
    });
  }

  openHistory(row: PatientListItem) {
    this.dialog.open(PatientHistoryDialog, {
      width: '1080px',
      maxWidth: '95vw',
      autoFocus: false,
      data: { id: row.id, name: row.name },
    });
  }

  delete(row: PatientListItem) {
    this.mtxDialog.confirm(
      'Permanently delete this patient?',
      `Patient "${row.name}" will be removed and cannot be restored.`,
      () => {
        this.patientService.delete(row.id).subscribe(() => {
          this.toast.success(`Deleted patient ${row.name}`);
          this.load();
        });
      }
    );
  }

  bulkDelete() {
    const rows = this.selected();
    if (rows.length === 0) {
      return;
    }
    this.mtxDialog.confirm(
      `Delete ${rows.length} selected patients?`,
      'This action cannot be undone.',
      () => {
        this.patientService.bulkDelete(rows.map(r => r.id)).subscribe(() => {
          this.toast.success(`Deleted ${rows.length} patients`);
          this.load();
        });
      }
    );
  }
}
