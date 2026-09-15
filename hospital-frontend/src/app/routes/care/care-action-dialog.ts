import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormRecord,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Observable, Subject, catchError, debounceTime, finalize, of, switchMap } from 'rxjs';
import { PatientService } from '../patient/patient.service';
import { PatientListItem } from '../patient/patient.models';
import { CareService } from './care.service';
import {
  CareDetail,
  CareHandoff,
  CareInpatient,
  CareOptions,
  CareReference,
  CareTask,
  careError,
} from './care.models';

export interface CareActionData {
  action: string;
  options: CareOptions;
  detail?: CareDetail;
  task?: CareTask;
  handoff?: CareHandoff;
  inpatient?: CareInpatient;
  patient?: { id: string; name: string };
}
interface Field {
  key: string;
  label: string;
  type: 'text' | 'textarea' | 'select' | 'multi' | 'datetime-local' | 'check';
  required?: boolean;
  max?: number;
  options?: { value: string | number | boolean; label: string }[];
}
const ACTIONS: Record<string, { title: string; path: string }> = {
  REGISTER: { title: 'Register an examination', path: '' },
  ORDER: { title: 'Request services', path: 'orders' },
  NOTE: { title: 'Record a nursing observation', path: 'notes' },
  REVIEW: { title: 'Record a clinical review', path: 'notes' },
  ROUTE: { title: 'Change the initial examination point', path: 'route' },
  CANCEL_VISIT: { title: 'Cancel registration', path: 'cancel' },
  FINISH: { title: 'Conclude the examination', path: 'finish' },
  DISCHARGE: { title: 'Complete inpatient treatment', path: 'discharge' },
  REQUEST_ADMISSION: { title: 'Request inpatient reception', path: 'admission/request' },
  ACCEPT_ADMISSION: { title: 'Receive into inpatient care', path: 'admission/accept' },
  CANCEL_ADMISSION: { title: 'Withdraw inpatient request', path: 'admission/cancel' },
  HANDOFF: { title: 'Request a doctor handoff or consultation', path: 'handoffs' },
  RESPOND: { title: 'Respond to the doctor request', path: '' },
  CANCEL_HANDOFF: { title: 'Withdraw the doctor request', path: '' },
  DIAGNOSIS: { title: 'Record a diagnosis and prescription', path: 'diagnoses' },
  RESULT: { title: 'Save a result draft', path: 'result' },
  PUBLISH: { title: 'Validate and publish this result', path: 'publish' },
  ACKNOWLEDGE: { title: 'Confirm clinical review of this result', path: 'acknowledge' },
  CANCEL_ORDER: { title: 'Request service cancellation', path: 'cancel' },
  DECLINE_ORDER: { title: 'Record why the service cannot be performed', path: 'decline' },
  RESOLVE_ORDER: { title: 'Resolve an unperformed service', path: 'resolve' },
  ADOPT: { title: 'Continue an existing inpatient episode', path: '' },
};

@Component({
  selector: 'app-care-action-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
  ],
  templateUrl: './care-action-dialog.html',
  styleUrl: './care-action-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CareActionDialog {
  readonly data = inject<CareActionData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<CareActionDialog>);
  private readonly care = inject(CareService);
  private readonly patients = inject(PatientService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly patientSearch = new Subject<void>();
  private readonly medicineSearch = new Subject<string>();
  private readonly requestId = crypto.randomUUID();
  readonly form = new FormRecord<FormControl<unknown>>({});
  readonly search = new FormControl('', { nonNullable: true });
  readonly searchField = new FormControl('name', { nonNullable: true });
  readonly medicineTerm = new FormControl('', { nonNullable: true });
  readonly patientRows = signal<PatientListItem[]>([]);
  readonly medicines = signal<CareReference[]>([]);
  readonly saving = signal(false);
  readonly searching = signal(false);
  readonly error = signal('');
  readonly fields: Field[] = [];
  readonly prescriptionRows = new FormArray([this.prescription()]);
  readonly title = ACTIONS[this.data.action]?.title ?? this.data.action;
  readonly result = this.data.task?.results.at(-1);

  constructor() {
    this.setup();
    this.patientSearch
      .pipe(
        debounceTime(300),
        switchMap(() => {
          this.searching.set(true);
          return this.patients
            .list({
              page: 1,
              size: 20,
              search: this.search.value,
              searchField: this.searchField.value,
            })
            .pipe(
              catchError(error => {
                this.error.set(careError(error));
                return of(null);
              }),
              finalize(() => this.searching.set(false))
            );
        }),
        takeUntilDestroyed()
      )
      .subscribe(page => this.patientRows.set(page?.items ?? []));
    this.search.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.patientSearch.next());
    this.searchField.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.patientSearch.next());
    this.medicineSearch
      .pipe(
        debounceTime(300),
        switchMap(term =>
          this.care.medicines(term).pipe(
            catchError(error => {
              this.error.set(careError(error));
              return of(null);
            })
          )
        ),
        takeUntilDestroyed()
      )
      .subscribe(page => {
        const selected = this.prescriptionRows.controls.map(row => row.controls.medicineId.value);
        const retained = this.medicines().filter(m => selected.includes(m.id));
        this.medicines.set([
          ...new Map([...retained, ...(page?.items ?? [])].map(m => [m.id, m])).values(),
        ]);
      });
    this.medicineTerm.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(term => this.medicineSearch.next(term));
    if (this.data.action === 'REGISTER' && !this.data.patient) this.patientSearch.next();
    if (this.data.action === 'DIAGNOSIS') this.medicineSearch.next('');
  }

  private setup() {
    const action = this.data.action;
    const add = (
      key: string,
      label: string,
      type: Field['type'] = 'textarea',
      required = true,
      max = 1000,
      value: unknown = '',
      options?: Field['options']
    ) => {
      this.fields.push({ key, label, type, required, max, options });
      this.form.addControl(
        key,
        new FormControl(value, [
          ...(required ? [Validators.required] : []),
          ...(['text', 'textarea'].includes(type) ? [Validators.maxLength(max)] : []),
        ])
      );
    };
    const point = (key: string, label: string, kind?: string) =>
      add(
        key,
        label,
        'select',
        true,
        0,
        null,
        this.data.options.points
          .filter(p => p.active && (!kind || p.kind === kind))
          .map(p => ({ value: p.id, label: p.name }))
      );
    const reason = () => add('reason', 'Reason / clinical indication');
    if (action === 'REGISTER') {
      add('patientId', 'Patient', 'select', true, 0, this.data.patient?.id ?? null);
      point('pointId', 'Initial examination point', 'CLINIC');
      add('complaint', 'Reason for visit', 'textarea', true, 500);
      add('priority', 'Queue priority', 'select', true, 0, 0, [
        { value: 0, label: 'Routine' },
        { value: 1, label: 'Priority' },
        { value: 2, label: 'Urgent · inform the clinical team immediately' },
      ]);
    } else if (action === 'ORDER') {
      add(
        'serviceIds',
        'Requested services',
        'multi',
        true,
        0,
        [],
        this.data.options.services
          .filter(
            s => s.active && this.data.options.points.some(p => p.id === s.pointId && p.active)
          )
          .map(s => ({ value: s.id, label: s.name }))
      );
      reason();
    } else if (action === 'NOTE' || action === 'REVIEW') {
      add(
        'kind',
        'Note type',
        'select',
        true,
        0,
        action === 'REVIEW' ? 'REVIEW' : 'VITALS',
        action === 'REVIEW'
          ? [
              { value: 'REVIEW', label: 'Clinical review' },
              { value: 'EXAM', label: 'Examination findings' },
            ]
          : [
              { value: 'VITALS', label: 'Vitals / observation' },
              { value: 'COORDINATION', label: 'Coordination' },
            ]
      );
      add('body', 'Observations / assessment', 'textarea', true, 4000);
    } else if (action === 'ROUTE') {
      point('pointId', 'Destination', 'CLINIC');
      reason();
    } else if (['FINISH', 'DISCHARGE', 'REQUEST_ADMISSION'].includes(action)) {
      add(
        'conclusion',
        'Clinical conclusion',
        'textarea',
        true,
        2000,
        this.data.detail?.conclusion ?? ''
      );
      add(
        'plan',
        'Treatment plan / instructions',
        'textarea',
        true,
        2000,
        this.data.detail?.plan ?? ''
      );
      if (action === 'REQUEST_ADMISSION') point('wardPointId', 'Receiving ward', 'WARD');
      else {
        add('outcome', 'Disposition', 'select', true, 0, 'OUTPATIENT', [
          { value: 'OUTPATIENT', label: 'Outpatient treatment' },
          { value: 'FOLLOW_UP', label: 'Scheduled follow-up' },
          { value: 'REFERRED', label: 'Referred to another facility' },
          { value: 'LEFT_INCOMPLETE', label: 'Left before completion' },
        ]);
        add('followupPlan', 'Plan for pending services / follow-up', 'textarea', false, 1000);
        add('followupAt', 'Review due at (local time)', 'datetime-local', false, 0, null);
      }
    } else if (action === 'ACCEPT_ADMISSION' || action === 'ADOPT') {
      if (action === 'ADOPT') point('wardPointId', 'Current inpatient ward', 'WARD');
      add('doctorUserId', 'Receiving / responsible doctor', 'select', true, 0, null);
      if (action === 'ACCEPT_ADMISSION')
        add('roomId', 'Bed room (may be assigned later)', 'select', false, 0, null);
    } else if (action === 'HANDOFF') {
      add('kind', 'Request type', 'select', true, 0, 'CONSULT', [
        { value: 'CONSULT', label: 'Consultation · retain responsibility' },
        { value: 'TRANSFER', label: 'Handoff · recipient must accept' },
      ]);
      point('pointId', 'Receiving clinical point');
      add('toUserId', 'Receiving doctor', 'select', true, 0, null);
      reason();
    } else if (action === 'RESPOND') {
      add('accept', 'Decision', 'select', true, 0, true, [
        { value: true, label: 'Accept / provide clinical opinion' },
        { value: false, label: 'Decline with reason' },
      ]);
      add('response', 'Clinical opinion / handoff response', 'textarea', true, 2000);
    } else if (action === 'RESULT') {
      add('body', 'Result', 'textarea', true, 4000, this.result?.body ?? '');
      add(
        'critical',
        'Critical result requiring prompt clinical review',
        'check',
        false,
        0,
        this.result?.critical ?? false
      );
      add(
        'correctionReason',
        'Reason for correcting a published result',
        'textarea',
        !!this.data.task?.results.some(r => r.publishedAt),
        1000,
        this.result?.publishedAt ? '' : (this.result?.correctionReason ?? '')
      );
    } else if (action === 'DIAGNOSIS') {
      add('description', 'Diagnosis / clinical assessment', 'textarea', true, 500);
      add('icdCode', 'ICD-10 code (optional)', 'text', false, 10);
    } else if (!['PUBLISH', 'ACKNOWLEDGE'].includes(action)) reason();
  }

  choices(field: Field): NonNullable<Field['options']> {
    if (field.key === 'patientId')
      return this.data.patient
        ? [
            {
              value: this.data.patient.id,
              label: `${this.data.patient.name} · ${this.data.patient.id}`,
            },
          ]
        : this.patientRows().map(p => ({
            value: p.id,
            label: `${p.name} · ${p.id} · ${p.birthDate ?? 'DOB not recorded'}`,
          }));
    const values = this.form.getRawValue();
    const pointId = Number(
      values['pointId'] ?? values['wardPointId'] ?? this.data.detail?.visit.wardPointId
    );
    if (['doctorUserId', 'toUserId'].includes(field.key)) {
      return this.data.options.staff
        .filter(
          u =>
            u.active &&
            u.doctorId != null &&
            ['DOCTOR', 'ADMIN'].includes(u.role) &&
            this.data.options.assignments.some(
              a => a.active && a.perform && a.userId === u.id && a.pointId === pointId
            )
        )
        .map(u => ({ value: u.id, label: u.name }));
    }
    if (field.key === 'roomId') {
      const departmentId = this.data.options.points.find(p => p.id === pointId)?.departmentId;
      return this.data.options.rooms
        .filter(r => r.departmentId === departmentId)
        .map(r => ({ value: r.id, label: `${r.name} · ${r.availableBeds} beds available` }));
    }
    if (field.key === 'pointId' && this.data.action === 'HANDOFF')
      return (field.options ?? []).filter(
        o => this.data.options.points.find(p => p.id === o.value)?.kind !== 'SERVICE'
      );
    if (field.key === 'wardPointId' && this.data.inpatient)
      return (field.options ?? []).filter(
        o =>
          this.data.options.points.find(p => p.id === o.value)?.departmentId ===
          this.data.inpatient?.departmentId
      );
    return field.options ?? [];
  }

  private prescription() {
    return this.fb.group({
      medicineId: [null as number | null],
      dosage: ['', Validators.maxLength(100)],
      frequency: ['', Validators.maxLength(100)],
      duration: ['', Validators.maxLength(50)],
      instructions: ['', Validators.maxLength(500)],
    });
  }
  addPrescription() {
    if (this.prescriptionRows.length < 30) this.prescriptionRows.push(this.prescription());
  }
  removePrescription(index: number) {
    this.prescriptionRows.removeAt(index);
  }

  submit() {
    if (this.saving()) return;
    this.error.set('');
    this.form.markAllAsTouched();
    this.prescriptionRows.markAllAsTouched();
    if (this.form.invalid || this.prescriptionRows.invalid) return;
    let body: Record<string, unknown> = this.form.getRawValue();
    const action = this.data.action;
    if (action === 'ORDER')
      body = {
        requestId: this.requestId,
        items: (body['serviceIds'] as number[]).map(serviceId => ({
          serviceId,
          reason: body['reason'],
        })),
      };
    if (action === 'ACKNOWLEDGE') body = { resultId: this.result?.id };
    if (['FINISH', 'DISCHARGE'].includes(action)) body['followupAt'] = body['followupAt'] || null;
    if (action === 'DIAGNOSIS') {
      const rows = this.prescriptionRows
        .getRawValue()
        .filter(
          row =>
            row.medicineId != null ||
            row.dosage ||
            row.frequency ||
            row.duration ||
            row.instructions
        );
      if (
        rows.some(
          row =>
            !row.medicineId ||
            !row.dosage?.trim() ||
            !row.frequency?.trim() ||
            !row.duration?.trim()
        )
      ) {
        this.error.set('Each prescription needs a medicine, dosage, frequency and duration.');
        return;
      }
      body['prescriptions'] = rows;
    }
    let request: Observable<CareDetail>;
    if (action === 'REGISTER') request = this.care.register(body);
    else if (action === 'ADOPT')
      request = this.care.adoptAdmission(this.data.inpatient!.admissionId, body);
    else if (this.data.task)
      request = this.care.taskAction(this.data.task.id, ACTIONS[action].path, body);
    else {
      const path =
        action === 'RESPOND'
          ? `handoffs/${this.data.handoff!.id}/response`
          : action === 'CANCEL_HANDOFF'
            ? `handoffs/${this.data.handoff!.id}/cancel`
            : ACTIONS[action].path;
      request = this.care.visitAction(this.data.detail!.visit.id, path, body);
    }
    this.saving.set(true);
    this.ref.disableClose = true;
    request
      .pipe(
        finalize(() => {
          this.saving.set(false);
          this.ref.disableClose = false;
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: detail => this.ref.close(detail),
        error: error => this.error.set(careError(error)),
      });
  }
}
