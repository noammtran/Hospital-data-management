import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { HotToastService } from '@ngxpert/hot-toast';
import { Observable, of } from 'rxjs';
import { vi } from 'vitest';

import { AdmissionFormDialog, AdmissionFormDialogData } from './admission-form-dialog';
import {
  Admission,
  AdmissionEditData,
  AdmissionOptions,
  UpdateAdmissionPayload,
} from './patient.models';
import { PatientService } from './patient.service';

describe('AdmissionFormDialog edit mode', () => {
  let fixture: ComponentFixture<AdmissionFormDialog>;
  let component: AdmissionFormDialog;

  const data: AdmissionFormDialogData = {
    patientId: 'P26AA001',
    patientName: 'Nguyen Van Test',
    admissionId: 'admission-1',
  };
  const options: AdmissionOptions = {
    departments: [{ id: 1, code: 'NOI', name: 'Internal medicine' }],
    doctors: [{ id: 10, code: 'BS-010', name: 'Doctor Test', departmentId: 1 }],
    rooms: [{ id: 101, name: 'P-101', departmentId: 1, bedCount: 6, currentOccupancy: 6 }],
    types: [{ value: 1, label: 'Emergency' }],
  };
  const admission: Admission = {
    admissionId: data.admissionId!,
    patientId: data.patientId,
    departmentId: 1,
    doctorId: 10,
    roomId: 101,
    admissionDate: '2026-08-01',
    dischargeDate: null,
    reason: 'Observation',
    type: 1,
    status: 1,
    notes: 'Initial note',
  };
  const editData: AdmissionEditData = { admission, options };
  const patientService = {
    getAdmissionEditData: vi.fn(
      (_patientId: string, _admissionId: string): Observable<AdmissionEditData> => of(editData)
    ),
    updateAdmission: vi.fn(
      (
        _patientId: string,
        _admissionId: string,
        _payload: UpdateAdmissionPayload
      ): Observable<Admission> => of(admission)
    ),
    getAdmissionOptions: vi.fn(),
    createAdmission: vi.fn(),
  };
  const dialogRef = { close: vi.fn((_result: boolean) => undefined) };
  const toast = { success: vi.fn((_message: string) => undefined) };

  beforeEach(async () => {
    vi.clearAllMocks();
    delete data.careManaged;
    await TestBed.configureTestingModule({
      imports: [AdmissionFormDialog],
      providers: [
        provideNativeDateAdapter(),
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: PatientService, useValue: patientService },
        { provide: HotToastService, useValue: toast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdmissionFormDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => fixture.destroy());

  it('loads edit data and pre-fills all editable fields', () => {
    expect(patientService.getAdmissionEditData).toHaveBeenCalledWith(
      data.patientId,
      data.admissionId
    );
    expect(patientService.getAdmissionOptions).not.toHaveBeenCalled();
    expect(component.isEditMode).toBe(true);
    expect(component.options()).toEqual(options);
    expect(component.form.getRawValue()).toEqual({
      admissionDate: new Date(2026, 7, 1),
      departmentId: 1,
      doctorId: 10,
      roomId: 101,
      type: 1,
      reason: 'Observation',
      notes: 'Initial note',
    });

    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Edit admission');
  });

  it('updates the admission and reports success', async () => {
    component.form.patchValue({ reason: '  Updated reason  ', notes: '   ' });

    component.save();
    await fixture.whenStable();

    expect(patientService.updateAdmission).toHaveBeenCalledWith(data.patientId, data.admissionId, {
      admissionDate: '2026-08-01',
      departmentId: 1,
      doctorId: 10,
      roomId: 101,
      type: 1,
      reason: 'Updated reason',
      notes: null,
    });
    expect(patientService.createAdmission).not.toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalledWith('Admission updated');
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('keeps care-managed doctor and department fixed while updating the bed', async () => {
    fixture.destroy();
    data.careManaged = true;
    fixture = TestBed.createComponent(AdmissionFormDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();

    expect(component.form.controls.departmentId.disabled).toBe(true);
    expect(component.form.controls.doctorId.disabled).toBe(true);
    expect(component.form.controls.roomId.enabled).toBe(true);
    component.form.controls.roomId.setValue(null);
    component.save();

    expect(patientService.updateAdmission).toHaveBeenCalledWith(
      data.patientId,
      data.admissionId,
      expect.objectContaining({ departmentId: 1, doctorId: 10, roomId: null })
    );
    expect(patientService.createAdmission).not.toHaveBeenCalled();
  });
});
