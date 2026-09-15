import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ApiResponse, PageResponse, UserAccount } from './user.models';
import { UserService } from './user.service';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  const user: UserAccount = {
    id: 7,
    username: '001201100022',
    identityNo: '001201100022',
    email: 'doctor.test@example.test',
    displayName: 'Bác sĩ Minh',
    role: 'DOCTOR',
    active: true,
    mustChangePassword: false,
    createdBy: 'admin',
    createdDate: '2026-07-31T08:00:00',
    lastUpdatedBy: 'admin',
    lastUpdatedDate: '2026-07-31T08:00:00',
  };

  function response<T>(data?: T): ApiResponse<T> {
    return {
      success: true,
      statusCode: 200,
      message: 'Success',
      data,
      timestamp: '2026-07-31T08:00:00Z',
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists accounts with one-based pagination and all active filters', () => {
    const page: PageResponse<UserAccount> = {
      items: [user],
      pagination: {
        page: 2,
        pageSize: 10,
        totalItems: 11,
        totalPages: 2,
        hasNext: false,
        hasPrev: true,
      },
    };

    service
      .list({ page: 2, size: 10, search: ' doctor ', role: 'DOCTOR', active: false })
      .subscribe(result => expect(result).toEqual(page));

    const request = httpMock.expectOne(req => req.url === '/api/v1/users');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('search')).toBe('doctor');
    expect(request.request.params.get('role')).toBe('DOCTOR');
    expect(request.request.params.get('active')).toBe('false');
    request.flush(response(page));
  });

  it('creates an account and unwraps the response envelope', () => {
    const payload = {
      identityNo: user.identityNo!,
      email: user.email,
      displayName: user.displayName,
      role: user.role,
    } as const;

    service.create(payload).subscribe(result => expect(result).toEqual(user));

    const request = httpMock.expectOne('/api/v1/users');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush(response(user));
  });

  it('updates account details and the identity-backed username', () => {
    const payload = {
      identityNo: '001201100023',
      email: 'new.email@example.test',
      displayName: 'Bác sĩ Minh Anh',
      role: 'DOCTOR' as const,
    };

    service.update(user.id, payload).subscribe();

    const request = httpMock.expectOne(`/api/v1/users/${user.id}`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(payload);
    request.flush(response({ ...user, ...payload }));
  });

  it('locks an account through the status endpoint', () => {
    service.setActive(user.id, false).subscribe();

    const request = httpMock.expectOne(`/api/v1/users/${user.id}/status`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ active: false });
    request.flush(response({ ...user, active: false }));
  });

  it('resets a password without expecting user data in the response', () => {
    service.resetPassword(user.id).subscribe(result => {
      expect(result).toBeUndefined();
    });

    const request = httpMock.expectOne(`/api/v1/users/${user.id}/reset-password`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush(response<void>());
  });

  it('loads only the selected staff work profile through its account endpoint', () => {
    const profile = {
      user,
      doctor: null,
      assignments: [],
      rolePermissions: ['CARE_EXAMINE'],
      setupIssues: ['DOCTOR_LINK_MISSING'],
    };
    service.workProfile(user.id).subscribe(result => expect(result).toEqual(profile));
    const request = httpMock.expectOne(`/api/v1/users/${user.id}/work-profile`);
    expect(request.request.method).toBe('GET');
    request.flush(response(profile));
  });

  it('creates and links a doctor profile without creating a second login', () => {
    const payload = {
      code: 'DOC_TEST',
      name: user.displayName,
      departmentId: 1,
      specialization: null,
      phoneNo: null,
      email: null,
    };
    const doctor = {
      id: 1000,
      code: 'DOC_TEST',
      name: user.displayName,
      departmentId: 1,
      departmentName: 'Nội',
      specialization: null,
      active: true,
    };
    service
      .createDoctorProfile(user.id, payload)
      .subscribe(result => expect(result).toEqual(doctor));
    const request = httpMock.expectOne(`/api/v1/users/${user.id}/doctor-profile`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush(response(doctor));
  });
});
