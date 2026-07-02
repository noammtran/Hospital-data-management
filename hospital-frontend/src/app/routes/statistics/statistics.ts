import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MtxGrid, MtxGridColumn } from '@ng-matero/extensions/grid';
import { format } from 'date-fns';

import { PatientDetailDialog } from '../patient/patient-detail-dialog';
import { StatisticsReportCriterion, StatisticsReportDialog } from './statistics-report-dialog';
import { DiagnosisOption, MedicineOption, PatientRef, StatisticsResult } from './statistics.models';
import { StatisticsService } from './statistics.service';

@Component({
  selector: 'app-statistics',
  templateUrl: './statistics.html',
  styleUrl: './statistics.scss',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatSelectModule,
    MatDatepickerModule,
    MtxGrid,
  ],
})
export class Statistics implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(StatisticsService);
  private readonly dialog = inject(MatDialog);

  readonly today = new Date();

  readonly diagnosisOptions = signal<DiagnosisOption[]>([]);
  readonly medicineOptions = signal<MedicineOption[]>([]);

  // Every filter the page can offer. Adding a new statistic later means adding
  // one entry here, one control on the form, and one request parameter.
  readonly filterDefs = [
    { key: 'dates', label: 'Examination date' },
    { key: 'disease', label: 'Disease (ICD-10)' },
    { key: 'medicine', label: 'Medicine' },
  ];

  // Only the filters the user has added are shown; the rest stay in the menu.
  readonly activeFilters = signal<string[]>([]);

  readonly filterForm = this.fb.group({
    from: [null as Date | null],
    to: [null as Date | null],
    icdCode: [null as string | null],
    medicineId: [null as number | null],
  });

  readonly result = signal<StatisticsResult | null>(null);
  readonly loading = signal(false);

  // The criteria that produced the current result, shown on the A4 report sheet.
  readonly appliedSummary = signal<StatisticsReportCriterion[]>([]);
  // A sentence describing the applied filter, built only from the active criteria.
  readonly appliedIntro = signal('');

  readonly columns: MtxGridColumn<PatientRef>[] = [
    { header: 'Patient ID', field: 'id', width: '110px' },
    { header: 'Full name', field: 'name' },
    {
      header: 'Record',
      field: 'operation',
      width: '110px',
      type: 'button',
      buttons: [
        {
          type: 'icon',
          icon: 'visibility',
          tooltip: 'View full medical record',
          click: row => this.openPatient(row.id),
        },
      ],
    },
  ];

  ngOnInit() {
    this.service.diagnosisOptions().subscribe(opts => this.diagnosisOptions.set(opts));
    this.service.medicineOptions().subscribe(opts => this.medicineOptions.set(opts));
  }

  /** Filter types not yet added — these populate the "Add filter" menu. */
  availableFilters() {
    const active = this.activeFilters();
    return this.filterDefs.filter(d => !active.includes(d.key));
  }

  addFilter(key: string) {
    this.activeFilters.update(list => (list.includes(key) ? list : [...list, key]));
  }

  removeFilter(key: string) {
    this.activeFilters.update(list => list.filter(k => k !== key));
    if (key === 'dates') {
      this.filterForm.patchValue({ from: null, to: null });
    } else if (key === 'disease') {
      this.filterForm.patchValue({ icdCode: null });
    } else if (key === 'medicine') {
      this.filterForm.patchValue({ medicineId: null });
    }
  }

  applyFilter() {
    const v = this.filterForm.getRawValue();
    const active = this.activeFilters();
    const report = this.buildReport(v, active);
    this.appliedIntro.set(report.intro);
    this.appliedSummary.set(report.criteria);
    this.loading.set(true);
    this.service
      .filter({
        from: active.includes('dates') && v.from ? format(v.from, 'yyyy-MM-dd') : undefined,
        to: active.includes('dates') && v.to ? format(v.to, 'yyyy-MM-dd') : undefined,
        icdCode: active.includes('disease') ? (v.icdCode ?? undefined) : undefined,
        medicineId: active.includes('medicine') ? (v.medicineId ?? undefined) : undefined,
      })
      .subscribe({
        next: r => {
          this.result.set(r);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  clearFilter() {
    this.activeFilters.set([]);
    this.filterForm.reset();
    this.result.set(null);
  }

  /** Opens the patient detail popup, which includes the full medical record. */
  openPatient(id: string) {
    this.dialog.open(PatientDetailDialog, {
      width: '1080px',
      maxWidth: '95vw',
      autoFocus: false,
      data: { id },
    });
  }

  /** Opens the current result as an A4 report sheet in a popup. */
  openReport() {
    const r = this.result();
    if (!r) {
      return;
    }
    this.dialog.open(StatisticsReportDialog, {
      width: '900px',
      maxWidth: '95vw',
      autoFocus: false,
      data: {
        reportNo: format(new Date(), 'yyyyMMdd-HHmmss'),
        generatedAt: new Date(),
        intro: this.appliedIntro(),
        criteria: this.appliedSummary(),
        total: r.total,
        patients: r.patients,
      },
    });
  }

  /** Builds the report intro sentence and the criteria lines from the active
   *  filters only, so an unchosen filter contributes neither a clause nor a line. */
  private buildReport(
    v: { from: Date | null; to: Date | null; icdCode: string | null; medicineId: number | null },
    active: string[]
  ): { intro: string; criteria: StatisticsReportCriterion[] } {
    const criteria: StatisticsReportCriterion[] = [];
    const clauses: string[] = [];
    if (active.includes('dates') && (v.from || v.to)) {
      const from = v.from ? format(v.from, 'dd/MM/yyyy') : '…';
      const to = v.to ? format(v.to, 'dd/MM/yyyy') : '…';
      criteria.push({ label: 'Examination period', value: `${from} - ${to}` });
      clauses.push(`examined between ${from} and ${to}`);
    }
    if (active.includes('disease') && v.icdCode) {
      const opt = this.diagnosisOptions().find(o => o.icdCode === v.icdCode);
      const label = opt?.label ?? v.icdCode;
      criteria.push({ label: 'Diagnosis (ICD-10)', value: label });
      clauses.push(`diagnosed with ${label}`);
    }
    if (active.includes('medicine') && v.medicineId != null) {
      const opt = this.medicineOptions().find(o => o.medicineId === v.medicineId);
      const label = opt?.label ?? String(v.medicineId);
      criteria.push({ label: 'Medicine', value: label });
      clauses.push(`prescribed ${label}`);
    }
    const intro = clauses.length
      ? `This report covers patients ${clauses.join(', ')}.`
      : 'This report covers all patients.';
    return { intro, criteria };
  }
}
