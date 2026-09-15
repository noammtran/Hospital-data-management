import { DatePipe, DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import {
  Observable,
  Subject,
  catchError,
  debounceTime,
  filter,
  finalize,
  map,
  merge,
  of,
  switchMap,
  timer,
} from 'rxjs';
import { AuthService } from '@core';
import { CareService } from './care.service';
import { CareActionDialog } from './care-action-dialog';
import {
  CareDetail,
  CareInpatient,
  CareOptions,
  CarePage,
  CareTask,
  CareVisit,
  careError,
  careLabel,
  patientStatusLabel,
} from './care.models';
import { PatientFormDialog } from '../patient/patient-form-dialog';
import { PatientService } from '../patient/patient.service';

@Component({
  selector: 'app-care-worklist',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatButtonToggleModule,
  ],
  templateUrl: './care-worklist.html',
  styleUrl: './care-worklist.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CareWorklist {
  private readonly care = inject(CareService);
  private readonly patients = inject(PatientService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly auth = inject(AuthService);
  private readonly reload = new Subject<void>();
  readonly canRegister = this.auth.hasPermission('CARE_REGISTER');
  readonly canCreate = this.auth.hasPermission('CREATE_PATIENT');
  readonly canManage = this.auth.hasPermission('CARE_MANAGE');
  readonly canAdmit = this.auth.hasPermission('CARE_ADMIT');
  readonly options = signal<CareOptions | null>(null);
  readonly mode = signal<'visits' | 'tasks' | 'inpatients'>('visits');
  readonly visitRows = signal<CareVisit[]>([]);
  readonly taskRows = signal<CareTask[]>([]);
  readonly inpatientRows = signal<CareInpatient[]>([]);
  readonly patientId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly updatedAt = signal<Date | null>(null);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly label = careLabel;
  readonly patientStatus = patientStatusLabel;
  readonly filters = inject(FormBuilder).group({
    search: [''],
    pointId: [null as number | null],
    status: [''],
    arrival: [''],
    openOnly: [true],
  });

  constructor() {
    this.care
      .options()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: value => this.options.set(value),
        error: error => this.error.set(careError(error)),
      });
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      this.patientId.set(params.get('patientId'));
      if (params.get('mode') === 'inpatients') this.mode.set('inpatients');
      this.filters.controls.openOnly.setValue(!params.get('patientId'), { emitEvent: false });
      this.page.set(0);
      this.reload.next();
    });
    this.filters.valueChanges.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => {
      this.page.set(0);
      this.reload.next();
    });
    merge(
      this.reload,
      timer(0, 10000).pipe(filter(() => this.document.visibilityState !== 'hidden'))
    )
      .pipe(
        switchMap(() => {
          const mode = this.mode();
          const filters = this.filters.getRawValue();
          this.loading.set(true);
          const shared = {
            search: filters.search,
            page: this.page() + 1,
            pageSize: this.pageSize(),
          };
          let request: Observable<
            CarePage<CareVisit> | CarePage<CareTask> | CarePage<CareInpatient>
          >;
          if (mode === 'visits')
            request = this.care.visits({
              ...shared,
              pointId: filters.pointId,
              status: filters.status,
              openOnly: filters.openOnly,
              patientId: this.patientId(),
            });
          else if (mode === 'tasks')
            request = this.care.tasks({
              ...shared,
              pointId: filters.pointId,
              status: filters.status,
              arrival: filters.arrival,
              openOnly: filters.openOnly,
            });
          else
            request = this.care.inpatients({
              ...shared,
              search: this.patientId() || shared.search,
              departmentId: this.options()?.points.find(p => p.id === filters.pointId)
                ?.departmentId,
            });
          return request.pipe(
            map(data => ({ mode, data })),
            catchError(error => {
              this.error.set(careError(error));
              return of(null);
            }),
            finalize(() => this.loading.set(false))
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe(result => {
        if (!result) return;
        this.error.set('');
        this.total.set(result.data.pagination.totalItems);
        this.updatedAt.set(new Date());
        if (result.mode === 'visits') this.visitRows.set(result.data.items as CareVisit[]);
        else if (result.mode === 'tasks') this.taskRows.set(result.data.items as CareTask[]);
        else this.inpatientRows.set(result.data.items as CareInpatient[]);
      });
  }
  setMode(mode: 'visits' | 'tasks' | 'inpatients') {
    this.mode.set(mode);
    this.filters.patchValue({ status: '', arrival: '', pointId: null }, { emitEvent: false });
    this.page.set(0);
    this.reload.next();
  }
  refresh() {
    this.reload.next();
  }
  paginate(event: PageEvent) {
    this.page.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.reload.next();
  }
  statusOptions() {
    return this.mode() === 'visits'
      ? ['WAITING', 'ACTIVE', 'AWAITING_ADMISSION', 'INPATIENT', 'COMPLETED', 'CANCELLED']
      : [
          'QUEUED',
          'IN_PROGRESS',
          'WAITING_RESULTS',
          'AWAITING_VALIDATION',
          'RESULT_READY',
          'CANCEL_REQUESTED',
          'DECLINED',
          'COMPLETED',
          'CANCELLED',
        ];
  }
  pointOptions() {
    return (this.options()?.points ?? []).filter(
      p => this.mode() !== 'inpatients' || p.kind === 'WARD'
    );
  }
  register() {
    const options = this.options();
    if (!options) return;
    const selected = this.patientId();
    const source: Observable<{ id: string; name: string } | undefined> = selected
      ? this.patients.getDetail(selected)
      : of(undefined);
    source.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: patient => {
        this.dialog
          .open(CareActionDialog, {
            width: '700px',
            maxWidth: '95vw',
            data: { action: 'REGISTER', options, patient },
          })
          .afterClosed()
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe((detail?: CareDetail) => {
            if (detail) void this.router.navigate(['/care', detail.visit.id]);
          });
      },
      error: error => this.error.set(careError(error)),
    });
  }
  createPatient() {
    this.dialog
      .open(PatientFormDialog, { width: '800px', maxWidth: '95vw', data: {} })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(created => {
        if (created) this.register();
      });
  }
  adopt(inpatient: CareInpatient) {
    const options = this.options();
    if (!options) return;
    this.dialog
      .open(CareActionDialog, {
        width: '650px',
        maxWidth: '95vw',
        data: { action: 'ADOPT', options, inpatient },
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((detail?: CareDetail) => {
        if (detail) void this.router.navigate(['/care', detail.visit.id]);
      });
  }
}
