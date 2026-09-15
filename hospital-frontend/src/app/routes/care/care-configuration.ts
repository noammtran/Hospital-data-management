import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { EMPTY, catchError, combineLatest, finalize, switchMap, tap } from 'rxjs';
import { UserService } from '../user/user.service';
import { StaffWorkProfile, workSetupIssue } from '../user/user.models';
import { CareService } from './care.service';
import {
  CareAssignment,
  CareOptions,
  CarePoint,
  CareServiceItem,
  careError,
  careLabel,
} from './care.models';

@Component({
  selector: 'app-care-configuration',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatExpansionModule,
  ],
  templateUrl: './care-configuration.html',
  styleUrl: './care-configuration.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CareConfiguration {
  private readonly care = inject(CareService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly users = inject(UserService);
  readonly selectedUserId = signal<number | null>(null);
  readonly workProfile = signal<StaffWorkProfile | null>(null);
  readonly profileLoading = signal(false);
  readonly profileError = signal('');
  readonly creatingDoctor = signal(false);
  readonly showDoctorForm = signal(false);
  readonly doctorError = signal('');
  readonly issueLabel = workSetupIssue;
  readonly selectedStaff = computed(() =>
    this.options()?.staff.find(u => u.id === this.selectedUserId())
  );
  readonly visibleAssignments = computed(
    () =>
      this.options()?.assignments.filter(
        a => this.selectedUserId() == null || a.userId === this.selectedUserId()
      ) ?? []
  );
  readonly availableDoctors = computed(() => {
    const data = this.options();
    return (
      data?.doctors.filter(
        d => !data.staff.some(u => u.doctorId === d.id && u.id !== this.selectedUserId())
      ) ?? []
    );
  });
  readonly options = signal<CareOptions | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly saved = signal('');
  readonly pointId = signal<number | undefined>(undefined);
  readonly serviceId = signal<number | undefined>(undefined);
  readonly label = careLabel;
  readonly pointForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^[A-Z0-9_-]{1,30}$/)]],
    name: ['', [Validators.required, Validators.maxLength(100)]],
    kind: ['CLINIC', Validators.required],
    departmentId: [null as number | null, Validators.required],
    active: [true],
  });
  readonly serviceForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^[A-Z0-9_-]{1,30}$/)]],
    name: ['', [Validators.required, Validators.maxLength(100)]],
    pointId: [null as number | null, Validators.required],
    active: [true],
  });
  readonly assignmentForm = this.fb.group({
    userId: [null as number | null, Validators.required],
    pointId: [null as number | null, Validators.required],
    perform: [false],
    verify: [false],
    active: [true],
    doctorId: [null as number | null],
  });
  readonly doctorForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^[A-Z0-9_-]{1,20}$/)]],
    name: ['', [Validators.required, Validators.maxLength(100)]],
    departmentId: [null as number | null, Validators.required],
    specialization: ['', Validators.maxLength(100)],
    phoneNo: ['', Validators.maxLength(20)],
    email: ['', [Validators.email, Validators.maxLength(50)]],
  });
  constructor() {
    this.assignmentForm.controls.userId.valueChanges
      .pipe(
        tap(id => {
          this.selectedUserId.set(id);
          this.workProfile.set(null);
          this.profileError.set('');
          this.showDoctorForm.set(false);
          this.doctorError.set('');
          this.assignmentForm.controls.doctorId.setValue(this.selectedStaff()?.doctorId ?? null);
          this.loadAssignment();
        }),
        switchMap(id => {
          if (id == null) {
            this.profileLoading.set(false);
            return EMPTY;
          }
          this.profileLoading.set(true);
          return this.users.workProfile(id).pipe(
            catchError(error => {
              this.profileError.set(careError(error));
              return EMPTY;
            }),
            finalize(() => this.profileLoading.set(false))
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe(profile => this.workProfile.set(profile));
    combineLatest([this.care.options(), this.route.queryParamMap])
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: ([options, params]) => {
          this.options.set(options);
          const id = Number(params.get('userId'));
          if (Number.isSafeInteger(id) && id > 0) {
            if (options.staff.some(u => u.id === id))
              this.assignmentForm.controls.userId.setValue(id);
            else
              this.error.set(
                'The selected staff account is unavailable. Choose an existing staff account.'
              );
          }
        },
        error: error => this.error.set(careError(error)),
      });
    this.assignmentForm.controls.pointId.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.loadAssignment());
  }
  hasRolePermission(permission: string) {
    return this.workProfile()?.rolePermissions.includes(permission) ?? false;
  }
  canLinkDoctor() {
    return this.hasRolePermission('CARE_EXAMINE');
  }
  canValidate() {
    return (
      this.hasRolePermission('CARE_VERIFY') &&
      this.options()?.points.some(
        p => p.id === this.assignmentForm.controls.pointId.value && p.kind === 'SERVICE'
      )
    );
  }
  startDoctorProfile() {
    const staff = this.selectedStaff();
    if (!staff || staff.doctorId != null || !this.canLinkDoctor()) return;
    this.doctorForm.reset({
      code: '',
      name: staff.name,
      departmentId: null,
      specialization: '',
      phoneNo: '',
      email: '',
    });
    this.doctorError.set('');
    this.showDoctorForm.set(true);
  }
  createDoctorProfile() {
    const id = this.selectedUserId();
    if (id == null || !this.canLinkDoctor() || this.busy() || this.creatingDoctor()) return;
    const value = this.doctorForm.getRawValue();
    this.doctorForm.patchValue({
      code: value.code?.trim(),
      name: value.name?.trim(),
      email: value.email?.trim(),
    });
    this.doctorForm.markAllAsTouched();
    if (this.doctorForm.invalid) return;
    this.creatingDoctor.set(true);
    this.assignmentForm.disable({ emitEvent: false });
    this.doctorError.set('');
    const clean = (text: string | null) => text?.trim() || null;
    this.users
      .createDoctorProfile(id, {
        code: value.code!.trim(),
        name: value.name!.trim(),
        departmentId: value.departmentId!,
        specialization: clean(value.specialization),
        phoneNo: clean(value.phoneNo),
        email: clean(value.email),
      })
      .pipe(
        finalize(() => {
          this.creatingDoctor.set(false);
          this.assignmentForm.enable({ emitEvent: false });
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: doctor => {
          this.options.update(
            data =>
              data && {
                ...data,
                staff: data.staff.map(u => (u.id === id ? { ...u, doctorId: doctor.id } : u)),
                doctors: [...data.doctors, { id: doctor.id, name: doctor.name }],
              }
          );
          this.workProfile.update(
            profile =>
              profile && {
                ...profile,
                doctor,
                setupIssues: profile.setupIssues.filter(issue => issue !== 'DOCTOR_LINK_MISSING'),
              }
          );
          this.assignmentForm.controls.doctorId.setValue(doctor.id);
          this.showDoctorForm.set(false);
          this.saved.set(
            'Doctor profile created and linked. Save a care point assignment to finish work setup.'
          );
        },
        error: error => {
          this.doctorError.set(careError(error));
          for (const [field, message] of Object.entries(error.error?.errors ?? {})) {
            this.doctorForm.get(field)?.setErrors({ server: message });
          }
        },
      });
  }
  doctorFieldError(field: keyof typeof this.doctorForm.controls) {
    const control = this.doctorForm.controls[field];
    return (
      control.getError('server') ??
      (control.hasError('required')
        ? 'This field is required.'
        : control.hasError('pattern')
          ? 'Use up to 20 uppercase letters, digits, underscores or hyphens.'
          : control.hasError('email')
            ? 'Enter a valid email address.'
            : 'The value is too long.')
    );
  }
  private loadAssignment() {
    const { userId, pointId } = this.assignmentForm.getRawValue();
    const row = this.options()?.assignments.find(a => a.userId === userId && a.pointId === pointId);
    this.assignmentForm.patchValue(
      { perform: row?.perform ?? false, verify: row?.verify ?? false, active: row?.active ?? true },
      { emitEvent: false }
    );
  }
  editAssignment(row: CareAssignment) {
    this.assignmentForm.patchValue(row);
  }
  editPoint(row?: CarePoint) {
    this.pointId.set(row?.id);
    this.pointForm.reset(
      row ?? { code: '', name: '', kind: 'CLINIC', departmentId: null, active: true }
    );
  }
  editService(row?: CareServiceItem) {
    this.serviceId.set(row?.id);
    this.serviceForm.reset(row ?? { code: '', name: '', pointId: null, active: true });
  }
  save(resource: 'points' | 'services' | 'assignments') {
    const form =
      resource === 'points'
        ? this.pointForm
        : resource === 'services'
          ? this.serviceForm
          : this.assignmentForm;
    form.markAllAsTouched();
    if (form.invalid || this.busy() || this.creatingDoctor()) return;
    if (resource === 'assignments' && (this.profileLoading() || !this.workProfile())) return;
    if (resource === 'assignments') {
      const assignment = this.assignmentForm.getRawValue();
      if (assignment.perform && !this.hasRolePermission('CARE_PERFORM')) {
        this.error.set(
          'This role cannot perform clinical work. Clear the clinical execution assignment.'
        );
        return;
      }
      if (assignment.verify && !this.canValidate()) {
        this.error.set(
          'Result validation requires a role with validation permission and a service point.'
        );
        return;
      }
      if (assignment.perform && this.canLinkDoctor() && assignment.doctorId == null) {
        this.error.set(
          'Link an existing doctor profile or create a new profile before assigning clinical execution.'
        );
        return;
      }
    }
    this.error.set('');
    this.saved.set('');
    this.busy.set(true);
    if (resource === 'assignments') this.assignmentForm.disable({ emitEvent: false });
    this.care
      .configure(
        resource,
        resource === 'assignments'
          ? {
              ...form.getRawValue(),
              doctorId: this.canLinkDoctor() ? this.assignmentForm.controls.doctorId.value : null,
            }
          : form.getRawValue(),
        resource === 'points'
          ? this.pointId()
          : resource === 'services'
            ? this.serviceId()
            : undefined
      )
      .pipe(
        finalize(() => {
          this.busy.set(false);
          if (resource === 'assignments') this.assignmentForm.enable({ emitEvent: false });
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: options => {
          this.options.set(options);
          this.saved.set('Configuration saved.');
          if (resource === 'assignments') {
            this.assignmentForm.controls.userId.setValue(this.selectedUserId());
          }
        },
        error: error => this.error.set(careError(error)),
      });
  }
  staffName(id: number) {
    return this.options()?.staff.find(u => u.id === id)?.name ?? `Staff #${id}`;
  }
  pointName(id: number) {
    return this.options()?.points.find(p => p.id === id)?.name ?? `Point #${id}`;
  }
  servicePoints() {
    return this.options()?.points.filter(p => p.kind === 'SERVICE' && p.active) ?? [];
  }
}
