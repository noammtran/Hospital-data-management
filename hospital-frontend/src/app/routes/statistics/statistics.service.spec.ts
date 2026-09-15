import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ApiResponse } from '../patient/patient.models';
import { StatisticsResult } from './statistics.models';
import { StatisticsService } from './statistics.service';

describe('StatisticsService', () => {
  let service: StatisticsService;
  let httpMock: HttpTestingController;

  const result: StatisticsResult = {
    total: 1,
    patients: [
      {
        id: 'P26AA001',
        name: 'Nguyen Van Test',
        identityNo: null,
        age: 44,
        gender: 1,
        status: 2,
        admissionCount: 2,
      },
    ],
    summary: {
      admissions: 2,
      diagnoses: 3,
      prescriptions: 4,
      patientStatuses: [{ key: 'admitted', label: 'Admitted', count: 1 }],
      genders: [{ key: 'male', label: 'Male', count: 1 }],
      ageGroups: [{ key: '40-59', label: '40 to 59', count: 1 }],
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [StatisticsService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StatisticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('sends the active cohort filters and unwraps the response envelope', () => {
    service
      .filter({
        from: '2026-08-01',
        to: '2026-08-29',
        icdCode: 'I10',
        medicineId: 23,
      })
      .subscribe(value => expect(value).toEqual(result));

    const request = httpMock.expectOne(
      request =>
        request.url === '/api/v1/statistics/patients' && request.params.get('from') === '2026-08-01'
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('to')).toBe('2026-08-29');
    expect(request.request.params.get('icdCode')).toBe('I10');
    expect(request.request.params.get('medicineId')).toBe('23');
    request.flush(response(result));
  });

  it('omits inactive filters from an all-time request', () => {
    service.filter({}).subscribe(value => expect(value).toEqual(result));

    const request = httpMock.expectOne('/api/v1/statistics/patients');
    expect(request.request.params.keys()).toEqual([]);
    request.flush(response(result));
  });

  function response<T>(data: T): ApiResponse<T> {
    return {
      success: true,
      statusCode: 200,
      message: 'Success',
      data,
      timestamp: '2026-08-29T00:00:00Z',
    };
  }
});
