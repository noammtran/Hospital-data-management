import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PatientService } from './patient.service';
import {
  ADMISSION_STATUS_OPTIONS,
  ADMISSION_TYPE_OPTIONS,
  BLOOD_TYPE_OPTIONS,
  GENDER_OPTIONS,
  PATIENT_STATUS_OPTIONS,
  PatientDetail,
  SEVERITY_OPTIONS,
  Visit,
  lookupLabel,
} from './patient.models';

export interface PatientDetailDialogData {
  id: string;
}

@Component({
  selector: 'app-patient-detail-dialog',
  templateUrl: './patient-detail-dialog.html',
  styleUrl: './patient-detail-dialog.scss',
  imports: [
    DatePipe,
    MatDialogModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
  ],
})
export class PatientDetailDialog {
  private readonly patientService = inject(PatientService);
  private readonly data = inject<PatientDetailDialogData>(MAT_DIALOG_DATA);

  readonly patient = signal<PatientDetail | null>(null);
  readonly loading = signal(true);

  readonly visits = signal<Visit[]>([]);
  readonly visitsLoading = signal(true);

  constructor() {
    this.patientService.getDetail(this.data.id).subscribe({
      next: detail => {
        this.patient.set(detail);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.patientService.getMedicalRecord(this.data.id).subscribe({
      next: visits => {
        this.visits.set(visits);
        this.visitsLoading.set(false);
      },
      error: () => this.visitsLoading.set(false),
    });
  }

  genderLabel(value: number | null) {
    return lookupLabel(GENDER_OPTIONS, value) || '—';
  }

  statusLabel(value: number) {
    return lookupLabel(PATIENT_STATUS_OPTIONS, value);
  }

  bloodTypeLabel(value: number | null) {
    return lookupLabel(BLOOD_TYPE_OPTIONS, value) || '—';
  }

  admissionTypeLabel(value: number | null) {
    return lookupLabel(ADMISSION_TYPE_OPTIONS, value) || '—';
  }

  admissionStatusLabel(value: number | null) {
    return lookupLabel(ADMISSION_STATUS_OPTIONS, value) || '—';
  }

  severityLabel(value: number | null) {
    return lookupLabel(SEVERITY_OPTIONS, value) || '—';
  }
}
