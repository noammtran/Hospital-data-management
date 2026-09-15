import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PatientPortalService } from './patient-portal.service';

describe('PatientPortalService', () => {
  it('loads the current account record without accepting a patient id', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const service = TestBed.inject(PatientPortalService);
    const http = TestBed.inject(HttpTestingController);

    service.getMyRecord().subscribe(record => expect(record.patient.id).toBe('P26MB001'));

    const request = http.expectOne('/api/v1/patient-portal/me');
    expect(request.request.method).toBe('GET');
    request.flush({ data: { patient: { id: 'P26MB001' }, records: [] } });
    http.verify();
  });
});
