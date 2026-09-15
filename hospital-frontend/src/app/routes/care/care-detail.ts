import { DatePipe, DOCUMENT, NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subject, catchError, filter, finalize, map, merge, of, switchMap, timer } from 'rxjs';
import { AuthService } from '@core';
import { CareService } from './care.service';
import { CareActionDialog } from './care-action-dialog';
import {
  CareDetail as CareDetailData,
  CareHandoff,
  CareOptions,
  CareTask,
  careError,
  careLabel,
  patientStatusLabel,
} from './care.models';
import { PatientService } from '../patient/patient.service';
import { PatientDetail } from '../patient/patient.models';
import { PatientDetailDialog } from '../patient/patient-detail-dialog';
import { AdmissionFormDialog } from '../patient/admission-form-dialog';
import {
  arrivalChoices,
  canStartTask,
  taskClinicalActions,
  taskExceptionActions,
  taskStepHint,
  visitClinicalActions,
  visitExceptionActions,
} from './care-action-visibility';

const VISIT_ACTIONS: Record<string, string> = {
  ROUTE: 'Change examination point',
  CANCEL_VISIT: 'Cancel registration',
  NOTE: 'Nursing observation',
  REVIEW: 'Clinical review',
  ORDER: 'Request services',
  DIAGNOSIS: 'Diagnosis / prescription',
  FINISH: 'Conclude examination',
  REQUEST_ADMISSION: 'Request inpatient care',
  CANCEL_ADMISSION: 'Withdraw inpatient request',
  ACCEPT_ADMISSION: 'Receive into ward',
  DISCHARGE: 'Discharge',
  HANDOFF: 'Handoff / consultation',
  WARD_ARRIVAL: 'Confirm arrival at ward',
};
const TASK_ACTIONS: Record<string, string> = {
  RESULT: 'Result draft / correction',
  PUBLISH: 'Validate & publish',
  ACKNOWLEDGE: 'Confirm reviewed',
  CANCEL_ORDER: 'Cancel service',
  DECLINE_ORDER: 'Unable to perform',
  RESOLVE_ORDER: 'Resolve unperformed service',
};

@Component({
  selector: 'app-care-detail',
  imports: [
    DatePipe,
    NgTemplateOutlet,
    RouterLink,
    MatButtonModule,
    MatExpansionModule,
    MatProgressBarModule,
  ],
  templateUrl: './care-detail.html',
  styleUrl: './care-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CareDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly care = inject(CareService);
  private readonly patients = inject(PatientService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly auth = inject(AuthService);
  private readonly reload = new Subject<void>();
  private dialogOpen = false;
  readonly canViewPatient = this.auth.hasPermission('VIEW_PATIENT');
  readonly canViewMedical = this.auth.hasPermission('VIEW_MEDICAL_RECORD');
  readonly canUpdateAdmission = this.auth.hasPermission('ADMISSION_UPDATE');
  readonly detail = signal<CareDetailData | null>(null);
  readonly profile = signal<PatientDetail | null>(null);
  readonly options = signal<CareOptions | null>(null);
  readonly busy = signal(false);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly updatedAt = signal<Date | null>(null);
  readonly label = careLabel;
  readonly patientStatus = patientStatusLabel;
  readonly visitActions = VISIT_ACTIONS;
  readonly taskActions = TASK_ACTIONS;
  readonly arrivalChoices = arrivalChoices;
  readonly canStartTask = canStartTask;
  readonly taskClinicalActions = taskClinicalActions;
  readonly taskExceptionActions = taskExceptionActions;
  readonly taskStepHint = taskStepHint;
  readonly visitClinicalActions = visitClinicalActions;
  readonly visitExceptionActions = visitExceptionActions;
  readonly arrivalLabels: Record<string, string> = {
    ARRIVED: 'Confirm arrived',
    CALLED: 'Call patient',
    ABSENT: 'Mark absent',
    LEFT: 'Mark left',
  };

  constructor() {
    this.care
      .options()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: options => this.options.set(options),
        error: error => this.error.set(careError(error)),
      });
    this.route.paramMap
      .pipe(
        switchMap(params => {
          this.detail.set(null);
          this.profile.set(null);
          const id = params.get('id')!;
          return merge(
            this.reload,
            timer(0, 10000).pipe(
              filter(
                () => !this.busy() && !this.dialogOpen && this.document.visibilityState !== 'hidden'
              )
            )
          ).pipe(
            switchMap(() => {
              this.loading.set(true);
              return this.care.detail(id).pipe(
                catchError(error => {
                  this.error.set(careError(error));
                  this.detail.set(null);
                  return of(null);
                }),
                finalize(() => this.loading.set(false))
              );
            })
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe(value => {
        if (!value) return;
        this.detail.set(value);
        this.error.set('');
        this.updatedAt.set(new Date());
        if (
          this.canViewPatient &&
          this.canViewMedical &&
          this.profile()?.id !== value.visit.patientId
        ) {
          this.patients
            .getDetail(value.visit.patientId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({ next: p => this.profile.set(p), error: () => this.profile.set(null) });
        }
      });
  }
  refresh() {
    this.reload.next();
  }
  action(action: string, task?: CareTask, handoff?: CareHandoff) {
    const detail = this.detail(),
      options = this.options();
    if (!detail || !options || this.busy()) return;
    if (action === 'WARD_ARRIVAL') {
      this.direct('ward-arrival');
      return;
    }
    this.dialogOpen = true;
    this.dialog
      .open(CareActionDialog, {
        width: '760px',
        maxWidth: '95vw',
        data: { action, detail, options, task, handoff },
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value?: CareDetailData) => {
        this.dialogOpen = false;
        if (value) this.detail.set(value);
        this.reload.next();
      });
  }
  direct(action: string, task?: CareTask, state?: string) {
    const detail = this.detail();
    if (!detail || this.busy()) return;
    if (task) {
      const current = detail.tasks.find(row => row.id === task.id);
      if (!current) return;
      if (action === 'arrival' && (!state || !arrivalChoices(current).includes(state))) return;
      if (action === 'start' && !canStartTask(current)) return;
    }
    this.busy.set(true);
    this.error.set('');
    const request = task
      ? this.care.taskAction(task.id, action, state ? { state } : {})
      : this.care.visitAction(detail.visit.id, action);
    request
      .pipe(
        map(value => value),
        finalize(() => this.busy.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: value => {
          this.detail.set(value);
          this.reload.next();
        },
        error: error => {
          this.error.set(careError(error));
        },
      });
  }
  openRecord() {
    const detail = this.detail();
    if (!detail) return;
    this.dialog.open(PatientDetailDialog, {
      width: '950px',
      maxWidth: '95vw',
      data: { id: detail.visit.patientId },
    });
  }
  editInpatientStay() {
    const detail = this.detail();
    if (
      !this.canUpdateAdmission ||
      !detail?.visit.admissionId ||
      !detail.actions.includes('DISCHARGE') ||
      this.busy()
    )
      return;
    this.dialogOpen = true;
    this.dialog
      .open(AdmissionFormDialog, {
        width: '720px',
        maxWidth: '95vw',
        data: {
          patientId: detail.visit.patientId,
          patientName: detail.visit.patientName,
          admissionId: detail.visit.admissionId,
          careManaged: true,
        },
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.dialogOpen = false;
        this.reload.next();
      });
  }
  serviceTasks() {
    return this.detail()?.tasks.filter(t => t.kind === 'ORDER') ?? [];
  }
  examTasks() {
    return (
      this.detail()?.tasks.filter(
        t => t.kind === 'EXAM' && !['COMPLETED', 'CANCELLED'].includes(t.status)
      ) ?? []
    );
  }
  isSuperseded(task: CareTask, revision: number) {
    return task.results.some(r => !!r.publishedAt && r.revision > revision);
  }
}
