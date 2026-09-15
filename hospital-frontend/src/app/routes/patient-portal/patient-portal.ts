import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import {
  ADMISSION_STATUS_OPTIONS,
  ADMISSION_TYPE_OPTIONS,
  BLOOD_TYPE_OPTIONS,
  GENDER_OPTIONS,
  PATIENT_STATUS_OPTIONS,
  lookupLabel,
  recordKey,
  recordStart,
} from '../patient/patient.models';
import { PatientSelfRecord } from './patient-portal.models';
import { PatientPortalService } from './patient-portal.service';
import { MedicalRecord } from '../patient/medical-record';

@Component({
  selector: 'app-patient-portal',
  templateUrl: './patient-portal.html',
  styleUrl: './patient-portal.scss',
  imports: [
    DatePipe,
    MatCardModule,
    MatExpansionModule,
    MatIconModule,
    MatProgressBarModule,
    MedicalRecord,
  ],
})
export class PatientPortal {
  readonly recordKey = recordKey;
  readonly recordStart = recordStart;
  private readonly service = inject(PatientPortalService);

  readonly record = signal<PatientSelfRecord | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal('');

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.service.getMyRecord().subscribe({
      next: record => {
        this.record.set(record);
        this.loading.set(false);
      },
      error: () => {
        this.record.set(null);
        this.errorMessage.set('Your record could not be loaded.');
        this.loading.set(false);
      },
    });
  }

  genderLabel(value: number | null): string {
    return lookupLabel(GENDER_OPTIONS, value) || 'Not recorded';
  }

  bloodTypeLabel(value: number | null): string {
    return lookupLabel(BLOOD_TYPE_OPTIONS, value) || 'Not recorded';
  }

  patientStatusLabel(value: number): string {
    return lookupLabel(PATIENT_STATUS_OPTIONS, value) || 'Not recorded';
  }

  admissionTypeLabel(value: number | null): string {
    return lookupLabel(ADMISSION_TYPE_OPTIONS, value) || 'Not recorded';
  }

  admissionStatusLabel(value: number | null): string {
    return lookupLabel(ADMISSION_STATUS_OPTIONS, value) || 'Not recorded';
  }
}
