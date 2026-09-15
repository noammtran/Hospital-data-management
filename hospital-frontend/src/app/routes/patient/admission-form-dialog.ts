import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { HotToastService } from '@ngxpert/hot-toast';
import { format, parseISO } from 'date-fns';

import {
  AdmissionDoctorOption,
  AdmissionOptions,
  AdmissionRoomOption,
  CreateAdmissionPayload,
} from './patient.models';
import { PatientService } from './patient.service';

export interface AdmissionFormDialogData {
  patientId: string;
  patientName: string;
  admissionId?: string;
  careManaged?: boolean;
}

@Component({
  selector: 'app-admission-form-dialog',
  templateUrl: './admission-form-dialog.html',
  styleUrl: './admission-form-dialog.scss',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
})
export class AdmissionFormDialog {
  private readonly fb = inject(FormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly toast = inject(HotToastService);
  private readonly dialogRef = inject<MatDialogRef<AdmissionFormDialog>>(MatDialogRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly data = inject<AdmissionFormDialogData>(MAT_DIALOG_DATA);
  readonly isEditMode = this.data.admissionId != null;
  readonly today = new Date();
  readonly options = signal<AdmissionOptions | null>(null);
  readonly optionsLoading = signal(true);
  readonly saving = signal(false);

  readonly form = this.fb.group({
    admissionDate: [new Date() as Date | null, Validators.required],
    departmentId: [null as number | null, Validators.required],
    doctorId: [null as number | null, Validators.required],
    roomId: [null as number | null],
    type: [null as number | null],
    reason: ['', Validators.maxLength(500)],
    notes: ['', Validators.maxLength(500)],
  });

  constructor() {
    this.loadFormData();
    if (this.data.careManaged) {
      this.form.controls.departmentId.disable({ emitEvent: false });
      this.form.controls.doctorId.disable({ emitEvent: false });
    }

    this.form.controls.departmentId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(departmentId => {
        const roomId = this.form.controls.roomId.value;
        if (roomId != null && !this.roomBelongsToDepartment(roomId, departmentId)) {
          this.form.controls.roomId.setValue(null);
        }
      });
  }

  availableRooms(): AdmissionRoomOption[] {
    const departmentId = this.form.controls.departmentId.value;
    if (departmentId == null) {
      return [];
    }
    return this.options()?.rooms.filter(room => room.departmentId === departmentId) ?? [];
  }

  doctorLabel(doctor: AdmissionDoctorOption): string {
    return `${doctor.name} (${doctor.code})`;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const payload: CreateAdmissionPayload = {
      admissionDate: format(value.admissionDate!, 'yyyy-MM-dd'),
      departmentId: value.departmentId!,
      doctorId: value.doctorId!,
      roomId: value.roomId,
      type: value.type,
      reason: value.reason?.trim() || null,
      notes: value.notes?.trim() || null,
    };

    const request = this.data.admissionId
      ? this.patientService.updateAdmission(this.data.patientId, this.data.admissionId, payload)
      : this.patientService.createAdmission(this.data.patientId, payload);

    this.saving.set(true);
    request.subscribe({
      next: () => {
        this.toast.success(this.isEditMode ? 'Admission updated' : 'Admission created');
        this.dialogRef.close(true);
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.applyServerErrors(error);
      },
    });
  }

  errorOf(controlName: string): string {
    const control = this.form.get(controlName);
    if (!control?.errors) {
      return '';
    }
    if (control.hasError('required')) {
      return 'This field is required';
    }
    if (control.hasError('maxlength')) {
      return `Maximum ${control.getError('maxlength').requiredLength} characters`;
    }
    if (control.hasError('matDatepickerMax')) {
      return 'Admission date cannot be in the future';
    }
    if (control.hasError('server')) {
      return control.getError('server');
    }
    return 'Invalid value';
  }

  private roomBelongsToDepartment(roomId: number, departmentId: number | null): boolean {
    return (
      this.options()?.rooms.some(
        room => room.id === roomId && room.departmentId === departmentId
      ) ?? false
    );
  }

  private loadFormData(): void {
    const admissionId = this.data.admissionId;
    if (!admissionId) {
      this.patientService.getAdmissionOptions().subscribe({
        next: options => {
          this.options.set(options);
          this.optionsLoading.set(false);
        },
        error: () => this.optionsLoading.set(false),
      });
      return;
    }

    this.patientService.getAdmissionEditData(this.data.patientId, admissionId).subscribe({
      next: editData => {
        this.options.set(editData.options);
        this.form.patchValue(
          {
            admissionDate: parseISO(editData.admission.admissionDate),
            departmentId: editData.admission.departmentId,
            doctorId: editData.admission.doctorId,
            roomId: editData.admission.roomId,
            type: editData.admission.type,
            reason: editData.admission.reason ?? '',
            notes: editData.admission.notes ?? '',
          },
          { emitEvent: false }
        );
        this.optionsLoading.set(false);
      },
      error: () => this.optionsLoading.set(false),
    });
  }

  private applyServerErrors(error: HttpErrorResponse): void {
    const errors: Record<string, string> | undefined = error.error?.errors;
    if (!errors) {
      return;
    }
    for (const [field, message] of Object.entries(errors)) {
      this.form.get(field)?.setErrors({ server: message });
    }
  }
}
