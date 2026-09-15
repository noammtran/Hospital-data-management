import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CareService } from './care.service';

describe('CareService contracts', () => {
  let service: CareService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CareService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('sends presence, point and page filters to the server, including false', () => {
    service
      .tasks({
        pointId: 10,
        arrival: 'ARRIVED',
        page: 2,
        pageSize: 10,
        openOnly: false,
        search: '',
      })
      .subscribe();
    const req = http.expectOne(r => r.url === '/api/v1/care/tasks');
    expect(req.request.params.get('arrival')).toBe('ARRIVED');
    expect(req.request.params.get('pointId')).toBe('10');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('openOnly')).toBe('false');
    expect(req.request.params.has('search')).toBe(false);
    req.flush({ data: { items: [], pagination: {} } });
  });
  it('acknowledges the exact result revision that was displayed', () => {
    service.taskAction(7, 'acknowledge', { resultId: 42 }).subscribe();
    const req = http.expectOne('/api/v1/care/tasks/7/acknowledge');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ resultId: 42 });
    req.flush({ data: {} });
  });
  it('keeps workflow registration separate from patient creation', () => {
    const body = {
      patientId: 'P26TEST001',
      pointId: 10,
      complaint: 'Fictional complaint',
      priority: 0,
    };
    service.register(body).subscribe();
    const req = http.expectOne('/api/v1/care/visits');
    expect(req.request.body).toEqual(body);
    req.flush({ data: {} });
  });
  it('preserves the existing admission identifier when opening its workspace', () => {
    service.adoptAdmission('admission-1', { wardPointId: 20, doctorUserId: 5 }).subscribe();
    const req = http.expectOne('/api/v1/care/admissions/admission-1/workspace');
    expect(req.request.body.wardPointId).toBe(20);
    req.flush({ data: {} });
  });
  it('uses PUT for staff assignment updates', () => {
    service
      .configure('assignments', {
        userId: 1,
        pointId: 2,
        perform: true,
        verify: false,
        active: true,
      })
      .subscribe();
    const req = http.expectOne('/api/v1/care/assignments');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.verify).toBe(false);
    req.flush({ data: {} });
  });
});
