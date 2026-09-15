import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MtxGrid, MtxGridColumn } from '@ng-matero/extensions/grid';
import { format, subDays } from 'date-fns';
import { Subscription, debounceTime } from 'rxjs';

import { PatientDetailDialog } from '../patient/patient-detail-dialog';
import { GENDER_OPTIONS, PATIENT_STATUS_OPTIONS, lookupLabel } from '../patient/patient.models';
import { StatisticsReportCriterion, StatisticsReportDialog } from './statistics-report-dialog';
import {
  DiagnosisOption,
  MedicineOption,
  PatientRef,
  StatisticsBreakdown,
  StatisticsResult,
} from './statistics.models';
import { StatisticsService } from './statistics.service';

const completeDateRangeValidator: ValidatorFn = (
  control: AbstractControl
): ValidationErrors | null => {
  const from = control.get('from')?.value as Date | null;
  const to = control.get('to')?.value as Date | null;
  if ((from && !to) || (!from && to)) {
    return { incompleteDateRange: true };
  }
  if (from && to && from.getTime() > to.getTime()) {
    return { reversedDateRange: true };
  }
  return null;
};

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
    MatProgressBarModule,
    MatSelectModule,
    MatDatepickerModule,
    MtxGrid,
  ],
})
export class Statistics implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(StatisticsService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private filterSubscription?: Subscription;

  readonly today = new Date();
  readonly defaultFrom = subDays(this.today, 29);

  readonly diagnosisOptions = signal<DiagnosisOption[]>([]);
  readonly medicineOptions = signal<MedicineOption[]>([]);

  readonly filterForm = this.fb.group(
    {
      from: [this.defaultFrom as Date | null],
      to: [this.today as Date | null],
      icdCode: [null as string | null],
      medicineId: [null as number | null],
    },
    { validators: completeDateRangeValidator }
  );

  readonly result = signal<StatisticsResult | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly activeCare = computed(() => {
    const statuses = this.result()?.summary.patientStatuses ?? [];
    return statuses
      .filter(item => item.key === 'examining' || item.key === 'admitted')
      .reduce((total, item) => total + item.count, 0);
  });

  // The criteria that produced the current result, shown on the A4 report sheet.
  readonly appliedSummary = signal<StatisticsReportCriterion[]>([]);
  // A sentence describing the applied filter, built only from the active criteria.
  readonly appliedIntro = signal('');

  readonly columns: MtxGridColumn<PatientRef>[] = [
    { header: 'Patient ID', field: 'id', width: '110px' },
    { header: 'Full name', field: 'name' },
    {
      header: 'Age',
      field: 'age',
      width: '80px',
      formatter: row => (row.age == null ? 'Not recorded' : String(row.age)),
    },
    {
      header: 'Gender',
      field: 'gender',
      width: '100px',
      formatter: row => lookupLabel(GENDER_OPTIONS, row.gender),
    },
    {
      header: 'Status',
      field: 'status',
      width: '120px',
      formatter: row => lookupLabel(PATIENT_STATUS_OPTIONS, row.status),
    },
    {
      header: 'Admissions',
      field: 'admissionCount',
      width: '100px',
    },
    {
      header: 'Medical record',
      field: 'operation',
      width: '130px',
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
    this.applyFilter();
    this.filterForm.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.applyFilter());
  }

  applyFilter() {
    if (this.filterForm.invalid) {
      this.filterSubscription?.unsubscribe();
      this.loading.set(false);
      this.result.set(null);
      this.appliedIntro.set('');
      this.appliedSummary.set([]);
      return;
    }
    const v = this.filterForm.getRawValue();
    const report = this.buildReport(v);
    this.appliedIntro.set(report.intro);
    this.appliedSummary.set(report.criteria);
    this.errorMessage.set('');
    this.loading.set(true);
    this.filterSubscription?.unsubscribe();
    this.filterSubscription = this.service
      .filter({
        from: v.from ? format(v.from, 'yyyy-MM-dd') : undefined,
        to: v.to ? format(v.to, 'yyyy-MM-dd') : undefined,
        icdCode: v.icdCode ?? undefined,
        medicineId: v.medicineId ?? undefined,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: r => {
          this.result.set(r);
          this.loading.set(false);
        },
        error: () => {
          this.result.set(null);
          this.errorMessage.set('Statistics could not be loaded. Check the filters and try again.');
          this.loading.set(false);
        },
      });
  }

  resetFilters() {
    this.filterForm.reset({ from: null, to: null, icdCode: null, medicineId: null });
  }

  dateRangeError(): string {
    if (this.filterForm.hasError('incompleteDateRange')) {
      return 'Enter both From and To dates, or leave both blank.';
    }
    if (this.filterForm.hasError('reversedDateRange')) {
      return 'To date must be on or after From date.';
    }
    return '';
  }

  percentage(item: StatisticsBreakdown, total: number): number {
    return total === 0 ? 0 : Math.round((item.count / total) * 100);
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
        summary: r.summary,
        patients: r.patients,
      },
    });
  }

  /** Builds the report intro sentence and the criteria lines from the active
   *  filters only, so an unchosen filter contributes neither a clause nor a line. */
  private buildReport(v: {
    from: Date | null;
    to: Date | null;
    icdCode: string | null;
    medicineId: number | null;
  }): { intro: string; criteria: StatisticsReportCriterion[] } {
    const criteria: StatisticsReportCriterion[] = [];
    const clauses: string[] = [];
    if (v.from && v.to) {
      const from = format(v.from, 'dd/MM/yyyy');
      const to = format(v.to, 'dd/MM/yyyy');
      criteria.push({ label: 'Clinical activity period', value: `${from} - ${to}` });
      clauses.push(`with clinical activity between ${from} and ${to}`);
    }
    if (v.icdCode) {
      const opt = this.diagnosisOptions().find(o => o.icdCode === v.icdCode);
      const label = opt?.label ?? v.icdCode;
      criteria.push({ label: 'Diagnosis (ICD-10)', value: label });
      clauses.push(`diagnosed with ${label}`);
    }
    if (v.medicineId != null) {
      const opt = this.medicineOptions().find(o => o.medicineId === v.medicineId);
      const label = opt?.label ?? String(v.medicineId);
      criteria.push({ label: 'Medicine', value: label });
      clauses.push(`prescribed ${label}`);
    }
    const intro = clauses.length
      ? `This dashboard covers patients ${clauses.join(', ')}.`
      : 'This dashboard covers all recorded patients and clinical activity.';
    return { intro, criteria };
  }
}
