import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
  Admission,
  AdmissionEditData,
  AdmissionOptions,
  ApiResponse,
  CreateAdmissionPayload,
  PatientDetail,
  UpdateAdmissionPayload,
} from './patient.models';
import { PatientService } from './patient.service';

describe('PatientService admission operations', () => {
  let service: PatientService;
  let httpMock: HttpTestingController;

  const options: AdmissionOptions = {
    departments: [{ id: 1, code: 'NOI', name: 'Internal medicine' }],
    doctors: [{ id: 2, code: 'BS-002', name: 'Doctor Test', departmentId: 1 }],
    rooms: [{ id: 3, name: 'P-101', departmentId: 1, bedCount: 6, currentOccupancy: 2 }],
    types: [{ value: 1, label: 'Emergency' }],
  };
  const payload: CreateAdmissionPayload = {
    admissionDate: '2026-08-01',
    departmentId: 1,
    doctorId: 2,
    roomId: 3,
    type: 1,
    reason: 'Observation',
    notes: null,
  };
  const admission: Admission = {
    admissionId: 'admission-1',
    patientId: 'P26AA001',
    departmentId: 1,
    doctorId: 2,
    roomId: 3,
    admissionDate: '2026-08-01',
    dischargeDate: null,
    reason: 'Observation',
    type: 1,
    status: 1,
    notes: null,
  };
  const editData: AdmissionEditData = { admission, options };
  const patient: PatientDetail = {
    id: admission.patientId,
    name: 'Nguyen Van Test',
    birthDate: null,
    gender: null,
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
    createdDate: '2026-08-01',
    lastUpdatedBy: 'doctor',
    lastUpdatedDate: '2026-08-01',
  };

  function response<T>(data: T): ApiResponse<T> {
    return {
      success: true,
      statusCode: 200,
      message: 'Success',
      data,
      timestamp: '2026-08-01T08:00:00Z',
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PatientService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads admission reference options and unwraps the response envelope', () => {
    service.getAdmissionOptions().subscribe(result => expect(result).toEqual(options));

    const request = httpMock.expectOne('/api/v1/admissions/options');
    expect(request.request.method).toBe('GET');
    request.flush(response(options));
  });

  it('creates an admission under the selected patient URL', () => {
    service
      .createAdmission(admission.patientId, payload)
      .subscribe(result => expect(result).toEqual(admission));

    const request = httpMock.expectOne(`/api/v1/patients/${admission.patientId}/admissions`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush(response(admission));
  });

  it('loads the admission and valid edit options from the nested edit-data URL', () => {
    service
      .getAdmissionEditData(admission.patientId, admission.admissionId)
      .subscribe(result => expect(result).toEqual(editData));

    const request = httpMock.expectOne(
      `/api/v1/patients/${admission.patientId}/admissions/${admission.admissionId}/edit-data`
    );
    expect(request.request.method).toBe('GET');
    request.flush(response(editData));
  });

  it('updates an admission through the nested patient URL', () => {
    const updatePayload: UpdateAdmissionPayload = { ...payload, notes: 'Needs observation' };

    service
      .updateAdmission(admission.patientId, admission.admissionId, updatePayload)
      .subscribe(result => expect(result).toEqual(admission));

    const request = httpMock.expectOne(
      `/api/v1/patients/${admission.patientId}/admissions/${admission.admissionId}`
    );
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(updatePayload);
    request.flush(response(admission));
  });

  it('advances patient status through dedicated workflow URLs', () => {
    const transitions = [
      { call: () => service.checkIn(patient.id), suffix: 'check-in' },
      { call: () => service.startExamination(patient.id), suffix: 'start-examination' },
      { call: () => service.completeExamination(patient.id), suffix: 'complete-examination' },
    ];

    for (const transition of transitions) {
      transition.call().subscribe(result => expect(result).toEqual(patient));
      const request = httpMock.expectOne(`/api/v1/patients/${patient.id}/${transition.suffix}`);
      expect(request.request.method).toBe('POST');
      request.flush(response(patient));
    }
  });

  it('completes an admission through its discharge workflow URL', () => {
    const discharged = { ...admission, dischargeDate: '2026-08-05', status: 2 };

    service
      .dischargeAdmission(admission.patientId, admission.admissionId)
      .subscribe(result => expect(result).toEqual(discharged));

    const request = httpMock.expectOne(
      `/api/v1/patients/${admission.patientId}/admissions/${admission.admissionId}/discharge`
    );
    expect(request.request.method).toBe('POST');
    request.flush(response(discharged));
  });

  it('deletes an admission through the nested patient URL and maps the envelope to void', () => {
    service.deleteAdmission(admission.patientId, admission.admissionId).subscribe(result => {
      expect(result).toBeUndefined();
    });

    const request = httpMock.expectOne(
      `/api/v1/patients/${admission.patientId}/admissions/${admission.admissionId}`
    );
    expect(request.request.method).toBe('DELETE');
    request.flush(response<void>(undefined));
  });
});
