import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MtxDialog } from '@ng-matero/extensions/dialog';
import { HotToastService } from '@ngxpert/hot-toast';
import { AuthService } from '@core/authentication';
import { PatientService } from './patient.service';
import { CareService } from '../care/care.service';
import { MedicalRecord } from './medical-record';
import { AdmissionFormDialog } from './admission-form-dialog';
import {
  BLOOD_TYPE_OPTIONS,
  GENDER_OPTIONS,
  PATIENT_STATUS_OPTIONS,
  PatientDetail,
  Visit,
  lookupLabel,
  recordKey,
  recordStart,
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
    RouterLink,
    MatDialogModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    MedicalRecord,
  ],
})
export class PatientDetailDialog {
  private readonly patientService = inject(PatientService);
  private readonly auth = inject(AuthService);
  private readonly confirmDialog = inject(MtxDialog);
  private readonly dialog = inject(MatDialog);
  private readonly toast = inject(HotToastService);
  private readonly data = inject<PatientDetailDialogData>(MAT_DIALOG_DATA);
  private readonly destroyRef = inject(DestroyRef);
  readonly canViewMedicalRecord = this.auth.hasPermission('VIEW_MEDICAL_RECORD');
  readonly canUseCare = this.auth.hasPermission('CARE_VIEW');
  readonly canDeleteAdmission = this.auth.hasPermission('ADMISSION_DELETE');
  readonly canUpdateAdmission = this.auth.hasPermission('ADMISSION_UPDATE');
  readonly canDischargeAdmission = this.auth.hasPermission('ADMISSION_DISCHARGE');
  private readonly care = this.canUseCare ? inject(CareService) : null;
  readonly activeCareId = signal<string | null>(null);
  readonly careChecked = signal(!this.canUseCare);
  readonly patient = signal<PatientDetail | null>(null);
  readonly loading = signal(true);
  readonly visits = signal<Visit[]>([]);
  readonly visitsLoading = signal(this.canViewMedicalRecord);
  readonly visitsError = signal('');
  readonly deletingAdmissionId = signal<string | null>(null);
  readonly recordKey = recordKey;
  readonly recordStart = recordStart;

  constructor() {
    this.loadPatient();
    this.care
      ?.visits({ patientId: this.data.id, openOnly: true, pageSize: 100 })
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: page => {
          this.activeCareId.set(page.items.find(v => !v.closedAt)?.id ?? null);
          this.careChecked.set(true);
        },
        error: () => this.activeCareId.set(null),
      });
    if (this.canViewMedicalRecord) this.loadMedicalRecord();
  }

  loadPatient(): void {
    this.loading.set(true);
    this.patientService
      .getDetail(this.data.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: detail => {
          this.patient.set(detail);
          this.loading.set(false);
        },
        error: () => {
          this.patient.set(null);
          this.loading.set(false);
        },
      });
  }

  loadMedicalRecord(): void {
    this.visitsLoading.set(true);
    this.visitsError.set('');
    this.patientService
      .getMedicalRecord(this.data.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: visits => {
          this.visits.set(visits);
          this.visitsLoading.set(false);
        },
        error: () => {
          this.visits.set([]);
          this.visitsError.set('The examination and treatment record could not be loaded.');
          this.visitsLoading.set(false);
        },
      });
  }

  deleteAdmission(visit: Visit): void {
    const admissionId = visit.admissionId;
    if (
      !this.canDeleteAdmission ||
      !admissionId ||
      this.deletingAdmissionId() ||
      (visit.care && !visit.care.closedAt)
    )
      return;
    this.confirmDialog.confirm(
      'Permanently delete this admission?',
      'This admission and its linked diagnoses and prescriptions will be removed and cannot be restored. A linked care visit and its service results remain.',
      () => {
        this.deletingAdmissionId.set(admissionId);
        this.patientService
          .deleteAdmission(this.data.id, admissionId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.toast.success('Admission deleted');
              this.deletingAdmissionId.set(null);
              this.loadPatient();
              this.loadMedicalRecord();
            },
            error: () => this.deletingAdmissionId.set(null),
          });
      }
    );
  }

  canManageExistingEpisode(visit: Visit): boolean {
    return (
      !visit.care &&
      !!visit.admissionId &&
      !visit.dischargeDate &&
      this.careChecked() &&
      !this.activeCareId()
    );
  }

  editAdmission(visit: Visit): void {
    const patient = this.patient();
    if (!patient || !this.canUpdateAdmission || !this.canManageExistingEpisode(visit)) return;
    this.dialog
      .open(AdmissionFormDialog, {
        width: '720px',
        maxWidth: '95vw',
        autoFocus: false,
        data: { patientId: patient.id, patientName: patient.name, admissionId: visit.admissionId },
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(changed => {
        if (changed) {
          this.loadPatient();
          this.loadMedicalRecord();
        }
      });
  }

  dischargeAdmission(visit: Visit): void {
    const admissionId = visit.admissionId;
    if (!admissionId || !this.canDischargeAdmission || !this.canManageExistingEpisode(visit))
      return;
    this.confirmDialog.confirm(
      'Complete this existing treatment episode?',
      'The discharge date will be recorded and any occupied room will be released.',
      () => {
        this.patientService
          .dischargeAdmission(this.data.id, admissionId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.loadPatient();
              this.loadMedicalRecord();
            },
            error: () => undefined,
          });
      }
    );
  }

  genderLabel(value: number | null) {
    return lookupLabel(GENDER_OPTIONS, value) || 'Not recorded';
  }
  statusLabel(value: number) {
    return lookupLabel(PATIENT_STATUS_OPTIONS, value);
  }
  bloodTypeLabel(value: number | null) {
    return lookupLabel(BLOOD_TYPE_OPTIONS, value) || 'Not recorded';
  }
}
