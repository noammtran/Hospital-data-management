import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { HttpErrorResponse } from '@angular/common/http';
import { HotToastService } from '@ngxpert/hot-toast';
import { format, parseISO } from 'date-fns';

import { PatientService } from './patient.service';
import {
  BLOOD_TYPE_OPTIONS,
  GENDER_OPTIONS,
  PATIENT_STATUS_OPTIONS,
  PatientDetail,
  PatientPayload,
} from './patient.models';

export interface PatientFormDialogData {
  patient?: PatientDetail;
}

@Component({
  selector: 'app-patient-form-dialog',
  templateUrl: './patient-form-dialog.html',
  styleUrl: './patient-form-dialog.scss',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
  ],
})
export class PatientFormDialog {
  private readonly fb = inject(FormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly toast = inject(HotToastService);
  private readonly dialogRef = inject<MatDialogRef<PatientFormDialog>>(MatDialogRef);
  private readonly data = inject<PatientFormDialogData>(MAT_DIALOG_DATA);

  readonly genderOptions = GENDER_OPTIONS;
  readonly statusOptions = PATIENT_STATUS_OPTIONS;
  readonly bloodTypeOptions = BLOOD_TYPE_OPTIONS;

  readonly isEdit = !!this.data.patient;
  readonly saving = signal(false);
  readonly today = new Date();

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    birthDate: [null as Date | null],
    gender: [null as number | null],
    address: ['', Validators.maxLength(255)],
    phoneNo: ['', Validators.maxLength(20)],
    email: ['', [Validators.email, Validators.maxLength(50)]],
    identityNo: ['', Validators.maxLength(20)],
    bloodType: [null as number | null],
    allergies: ['', Validators.maxLength(500)],
    medicalHistory: ['', Validators.maxLength(1000)],
    insuranceNo: ['', Validators.maxLength(20)],
    insuranceExpiry: [null as Date | null],
    emergencyContactName: ['', Validators.maxLength(100)],
    emergencyContactPhone: ['', Validators.maxLength(20)],
    emergencyContactRelation: ['', Validators.maxLength(50)],
    status: [0, Validators.required],
    note: ['', Validators.maxLength(1000)],
  });

  constructor() {
    const p = this.data.patient;
    if (p) {
      this.form.patchValue({
        ...p,
        name: p.name ?? '',
        address: p.address ?? '',
        phoneNo: p.phoneNo ?? '',
        email: p.email ?? '',
        identityNo: p.identityNo ?? '',
        allergies: p.allergies ?? '',
        medicalHistory: p.medicalHistory ?? '',
        insuranceNo: p.insuranceNo ?? '',
        emergencyContactName: p.emergencyContactName ?? '',
        emergencyContactPhone: p.emergencyContactPhone ?? '',
        emergencyContactRelation: p.emergencyContactRelation ?? '',
        note: p.note ?? '',
        birthDate: p.birthDate ? parseISO(p.birthDate) : null,
        insuranceExpiry: p.insuranceExpiry ? parseISO(p.insuranceExpiry) : null,
      });
    }
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const payload: PatientPayload = {
      ...v,
      name: (v.name ?? '').trim(),
      address: v.address?.trim() || null,
      phoneNo: v.phoneNo?.trim() || null,
      email: v.email?.trim() || null,
      identityNo: v.identityNo?.trim() || null,
      allergies: v.allergies?.trim() || null,
      medicalHistory: v.medicalHistory?.trim() || null,
      insuranceNo: v.insuranceNo?.trim() || null,
      emergencyContactName: v.emergencyContactName?.trim() || null,
      emergencyContactPhone: v.emergencyContactPhone?.trim() || null,
      emergencyContactRelation: v.emergencyContactRelation?.trim() || null,
      note: v.note?.trim() || null,
      birthDate: v.birthDate ? format(v.birthDate, 'yyyy-MM-dd') : null,
      insuranceExpiry: v.insuranceExpiry ? format(v.insuranceExpiry, 'yyyy-MM-dd') : null,
      status: v.status ?? 0,
    };

    this.saving.set(true);
    const request$ = this.isEdit
      ? this.patientService.update(this.data.patient!.id, payload)
      : this.patientService.create(payload);

    request$.subscribe({
      next: () => {
        this.toast.success(this.isEdit ? 'Patient updated' : 'Patient created');
        this.dialogRef.close(true);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.applyServerErrors(err);
      },
    });
  }

  /** Map the backend validation map { field: message } onto the matching controls. */
  private applyServerErrors(err: HttpErrorResponse) {
    const errors: Record<string, string> | undefined = err.error?.errors;
    if (!errors) {
      return;
    }
    for (const [field, message] of Object.entries(errors)) {
      this.form.get(field)?.setErrors({ server: message });
    }
  }

  errorOf(controlName: string): string {
    const control = this.form.get(controlName);
    if (!control || !control.errors) {
      return '';
    }
    if (control.hasError('required')) {
      return 'This field is required';
    }
    if (control.hasError('maxlength')) {
      const max = control.getError('maxlength').requiredLength;
      return `Maximum ${max} characters`;
    }
    if (control.hasError('email')) {
      return 'Invalid email format';
    }
    if (control.hasError('matDatepickerMax')) {
      return 'Date of birth cannot be in the future';
    }
    if (control.hasError('server')) {
      return control.getError('server');
    }
    return 'Invalid value';
  }
}
