import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { MtxDialog } from '@ng-matero/extensions/dialog';
import { HotToastService } from '@ngxpert/hot-toast';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AppPermission, AuthService } from '@core/authentication';
import { CareService } from '../care/care.service';
import { PatientDetailDialog } from './patient-detail-dialog';
import { PatientDetail, Visit } from './patient.models';
import { PatientService } from './patient.service';

describe('PatientDetailDialog unified medical record', () => {
  let fixture: ComponentFixture<PatientDetailDialog>;
  const patient: PatientDetail = {
    id: 'P26AA001',
    name: 'Nguyen Van Test',
    birthDate: '1990-01-01',
    gender: 1,
    address: null,
    phoneNo: null,
    email: null,
    identityNo: null,
    bloodType: null,
    allergies: null,
    medicalHistory: null,
    insuranceNo: null,
    insuranceExpiry: null,
    emergencyContactName: null,
    emergencyContactPhone: null,
    emergencyContactRelation: null,
    status: 1,
    note: null,
    createdBy: 'receptionist',
    createdDate: '2026-07-01',
    lastUpdatedBy: 'receptionist',
    lastUpdatedDate: '2026-07-01',
  };

  const legacy: Visit = {
    admissionId: 'admission-1',
    admissionDate: '2026-07-01',
    dischargeDate: '2026-07-05',
    departmentName: 'Internal medicine',
    doctorName: 'Doctor Test',
    roomName: 'P-101',
    reason: 'Observation',
    type: 1,
    status: 2,
    notes: null,
    diagnoses: [],
    prescriptions: [],
  };
  const outpatient: Visit = {
    ...legacy,
    admissionId: null,
    admissionDate: null,
    dischargeDate: null,
    care: {
      id: 'visit-1',
      createdAt: '2026-09-10T08:00:00',
      closedAt: '2026-09-10T09:00:00',
      status: 'COMPLETED',
      pointName: 'Examination clinic',
      doctorName: 'Doctor Demo',
      complaint: 'Fictional complaint',
      conclusion: 'Recorded final conclusion',
      plan: 'Treatment instructions',
      outcome: 'OUTPATIENT',
      followupPlan: 'Return for review',
      followupAt: '2026-09-12T09:00:00',
      services: [],
      notes: [],
      handoffs: [],
    },
    diagnoses: [
      {
        date: '2026-09-10',
        icdCode: 'I10',
        description: 'Recorded diagnosis',
        severity: null,
        doctorName: 'Doctor Demo',
      },
    ],
    prescriptions: [
      {
        medicineName: 'Medicine Demo',
        dosage: 'One tablet',
        frequency: 'Daily',
        duration: '7 days',
        instructions: 'After food',
      },
    ],
  };

  async function setup(permissions: AppPermission[], visits: Visit[] = [legacy]) {
    const patientService = {
      getDetail: vi.fn(() => of(patient)),
      getMedicalRecord: vi.fn(() => of(visits)),
      deleteAdmission: vi.fn(() => of(undefined)),
      dischargeAdmission: vi.fn(() => of(undefined)),
    };
    let confirmation: { title: string; description: string; action: () => void } | undefined;
    const confirm = vi.fn((title: string, description: string, action: () => void) => {
      confirmation = { title, description, action };
    });
    const open = vi.fn(() => ({ afterClosed: () => of(true) }));
    await TestBed.configureTestingModule({
      imports: [PatientDetailDialog],
      providers: [
        provideRouter([]),
        { provide: MAT_DIALOG_DATA, useValue: { id: patient.id } },
        {
          provide: AuthService,
          useValue: {
            hasPermission: (permission: AppPermission) => permissions.includes(permission),
          },
        },
        { provide: PatientService, useValue: patientService },
        { provide: CareService, useValue: { visits: () => of({ items: [] }) } },
        { provide: MtxDialog, useValue: { confirm } },
        { provide: HotToastService, useValue: { success: vi.fn() } },
      ],
    }).compileComponents();
    TestBed.overrideProvider(MatDialog, { useValue: { open } });
    fixture = TestBed.createComponent(PatientDetailDialog);
    await fixture.whenStable();
    return {
      component: fixture.componentInstance,
      patientService,
      open,
      confirm,
      get confirmation() {
        return confirmation;
      },
    };
  }
  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';
  afterEach(() => fixture?.destroy());

  it('removes manual admission creation even for an account with create permission', async () => {
    await setup(['VIEW_MEDICAL_RECORD', 'CARE_VIEW', 'ADMISSION_CREATE']);
    expect(text()).toContain('Examination and treatment record');
    expect(text()).not.toContain('Add treatment episode');
    expect(text()).not.toContain('Add admission');
    expect(text()).not.toContain('Complete examination');
    expect(text()).not.toContain('Delete admission');
  });

  it('shows the conclusion, diagnoses, medicines and follow-up in one outpatient entry', async () => {
    await setup(['VIEW_MEDICAL_RECORD', 'CARE_VIEW'], [outpatient]);
    expect(text()).toContain('Recorded final conclusion');
    expect(text()).toContain('Recorded diagnosis');
    expect(text()).toContain('Medicine Demo');
    expect(text()).toContain('Return for review');
    expect(fixture.nativeElement.querySelectorAll('app-medical-record')).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('a[href="/care/visit-1"]')).not.toBeNull();
    expect(text()).not.toContain('Inpatient stay');
  });

  it('displays an inpatient stay inside the care record and directs updates to its workspace', async () => {
    const linked = {
      ...outpatient,
      ...legacy,
      care: { ...outpatient.care!, closedAt: null, status: 'INPATIENT' },
    };
    const ctx = await setup(
      [
        'VIEW_MEDICAL_RECORD',
        'CARE_VIEW',
        'ADMISSION_UPDATE',
        'ADMISSION_DISCHARGE',
        'ADMISSION_DELETE',
      ],
      [linked]
    );
    expect(text()).toContain('Inpatient stay');
    expect(text()).not.toContain('Update existing episode');
    expect(text()).not.toContain('Complete existing episode');
    expect(text()).not.toContain('Delete admission');
    ctx.component.editAdmission(linked);
    ctx.component.dischargeAdmission(linked);
    ctx.component.deleteAdmission(linked);
    expect(ctx.open).not.toHaveBeenCalled();
    expect(ctx.confirm).not.toHaveBeenCalled();
  });

  it('preserves existing-episode tools without offering admission creation', async () => {
    const active = { ...legacy, dischargeDate: null };
    const ctx = await setup(
      ['VIEW_MEDICAL_RECORD', 'ADMISSION_UPDATE', 'ADMISSION_DISCHARGE'],
      [active]
    );
    ctx.component.editAdmission(active);
    expect(ctx.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        data: { patientId: patient.id, patientName: patient.name, admissionId: legacy.admissionId },
      })
    );
    ctx.component.dischargeAdmission(active);
    expect(ctx.patientService.dischargeAdmission).not.toHaveBeenCalled();
    ctx.confirmation?.action();
    expect(ctx.patientService.dischargeAdmission).toHaveBeenCalledWith(
      patient.id,
      legacy.admissionId
    );
  });

  it('requires permission and confirmation before deleting an admission', async () => {
    const ctx = await setup(['VIEW_MEDICAL_RECORD', 'ADMISSION_DELETE']);
    ctx.component.deleteAdmission(legacy);
    expect(ctx.patientService.deleteAdmission).not.toHaveBeenCalled();
    expect(ctx.confirmation?.description).toContain('cannot be restored');
    ctx.confirmation?.action();
    expect(ctx.patientService.deleteAdmission).toHaveBeenCalledWith(patient.id, legacy.admissionId);
    expect(ctx.patientService.getMedicalRecord).toHaveBeenCalledTimes(2);
  });

  it('does not load clinical records without the medical-record permission', async () => {
    const ctx = await setup([]);
    expect(ctx.patientService.getMedicalRecord).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('app-medical-record')).toBeNull();
    ctx.component.deleteAdmission(legacy);
    expect(ctx.confirm).not.toHaveBeenCalled();
  });

  it('clears previous records and presents retry when loading fails', async () => {
    const ctx = await setup(['VIEW_MEDICAL_RECORD'], [outpatient]);
    ctx.patientService.getMedicalRecord.mockReturnValue(throwError(() => new Error('offline')));
    ctx.component.loadMedicalRecord();
    await fixture.whenStable();
    expect(text()).toContain('could not be loaded');
    expect(text()).not.toContain('Recorded final conclusion');
    expect(text()).toContain('Retry');
  });
});
