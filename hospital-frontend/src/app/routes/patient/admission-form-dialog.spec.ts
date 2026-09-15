import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { HotToastService } from '@ngxpert/hot-toast';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AdmissionFormDialog, AdmissionFormDialogData } from './admission-form-dialog';
import {
  Admission,
  AdmissionOptions,
  CreateAdmissionPayload,
} from './patient.models';
import { PatientService } from './patient.service';

describe('AdmissionFormDialog', () => {
  let fixture: ComponentFixture<AdmissionFormDialog>;
  let component: AdmissionFormDialog;
  let patientService: {
    getAdmissionOptions: () => Observable<AdmissionOptions>;
    createAdmission: (patientId: string, payload: CreateAdmissionPayload) => Observable<Admission>;
  };
  let dialogRef: { close: (result: boolean) => void };
  let toast: { success: (message: string) => void };

  const data: AdmissionFormDialogData = {
    patientId: 'P26AA001',
    patientName: 'Nguyen Van Test',
  };
  const options: AdmissionOptions = {
    departments: [
      { id: 1, code: 'NOI', name: 'Internal medicine' },
      { id: 2, code: 'NGOAI', name: 'Surgery' },
    ],
    doctors: [{ id: 10, code: 'BS-010', name: 'Doctor Test', departmentId: 1 }],
    rooms: [
      { id: 101, name: 'P-101', departmentId: 1, bedCount: 6, currentOccupancy: 2 },
      { id: 201, name: 'P-201', departmentId: 2, bedCount: 4, currentOccupancy: 1 },
    ],
    types: [
      { value: 1, label: 'Emergency' },
      { value: 3, label: 'Voluntary' },
    ],
  };
  const createdAdmission: Admission = {
    admissionId: 'admission-1',
    patientId: data.patientId,
    departmentId: 1,
    doctorId: 10,
    roomId: 101,
    admissionDate: '2026-08-01',
    dischargeDate: null,
    reason: 'Observation',
    type: 1,
    status: 1,
    notes: null,
  };

  beforeEach(async () => {
    patientService = {
      getAdmissionOptions: () => of(options),
      createAdmission: () => of(createdAdmission),
    };
    dialogRef = { close: () => undefined };
    toast = { success: () => undefined };

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

  it('loads options, filters rooms by department, and clears an incompatible room', async () => {
    expect(component.options()).toEqual(options);
    expect(component.optionsLoading()).toBe(false);
    expect(component.availableRooms()).toEqual([]);

    component.form.controls.departmentId.setValue(1);
    await fixture.whenStable();
    expect(component.availableRooms()).toEqual([options.rooms[0]]);

    component.form.controls.roomId.setValue(101);
    component.form.controls.departmentId.setValue(2);
    await fixture.whenStable();

    expect(component.form.controls.roomId.value).toBeNull();
    expect(component.availableRooms()).toEqual([options.rooms[1]]);
  });

  it('normalizes text and date fields before creating an admission', async () => {
    vi.spyOn(patientService, 'createAdmission');
    vi.spyOn(dialogRef, 'close');
    vi.spyOn(toast, 'success');
    component.form.setValue({
      admissionDate: new Date(2026, 7, 1),
      departmentId: 1,
      doctorId: 10,
      roomId: 101,
      type: 1,
      reason: '  Observation  ',
      notes: '   ',
    });

    component.save();
    await fixture.whenStable();

    expect(patientService.createAdmission).toHaveBeenCalledWith(data.patientId, {
      admissionDate: '2026-08-01',
      departmentId: 1,
      doctorId: 10,
      roomId: 101,
      type: 1,
      reason: 'Observation',
      notes: null,
    });
    expect(toast.success).toHaveBeenCalledWith('Admission created');
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('maps backend field errors and re-enables saving', async () => {
    vi.spyOn(patientService, 'createAdmission').mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: {
              errors: {
                doctorId: 'Doctor is unavailable',
                roomId: 'Room is no longer available',
              },
            },
          })
      )
    );
    component.form.patchValue({
      admissionDate: new Date(2026, 7, 1),
      departmentId: 1,
      doctorId: 10,
      roomId: 101,
    });

    component.save();
    await fixture.whenStable();

    expect(component.form.controls.doctorId.getError('server')).toBe('Doctor is unavailable');
    expect(component.form.controls.roomId.getError('server')).toBe('Room is no longer available');
    expect(component.saving()).toBe(false);
  });
});
