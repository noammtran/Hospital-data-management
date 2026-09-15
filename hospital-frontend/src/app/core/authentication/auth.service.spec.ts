import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService, TokenService, User } from '@core/authentication';
import { LocalStorageService, MemoryStorageService } from '@shared/services/storage.service';
import { skip } from 'rxjs';

describe('AuthService', () => {
  let authService: AuthService;
  let tokenService: TokenService;
  let httpMock: HttpTestingController;

  const tokenEnvelope = {
    success: true,
    statusCode: 200,
    message: 'Success',
    data: { accessToken: 'token', tokenType: 'Bearer', expiresIn: 1800 },
    timestamp: '2026-07-31T00:00:00Z',
  };
  const profileEnvelope = {
    success: true,
    statusCode: 200,
    message: 'Success',
    data: {
      id: 2,
      username: 'doctor',
      email: 'doctor@hospital.example',
      displayName: 'Doctor Demo',
      role: 'DOCTOR',
      permissions: ['VIEW_PATIENT', 'VIEW_MEDICAL_RECORD', 'VIEW_STATISTICS', 'ADMISSION_CREATE'],
      mustChangePassword: false,
    },
    timestamp: '2026-07-31T00:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: LocalStorageService, useClass: MemoryStorageService },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    authService = TestBed.inject(AuthService);
    tokenService = TestBed.inject(TokenService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(authService).toBeTruthy();
  });

  it('should clear the session when login fails', () => {
    let failed = false;
    authService.login('doctor', 'wrong').subscribe({ error: () => (failed = true) });
    httpMock
      .expectOne('/api/v1/auth/login')
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(failed).toBe(true);
    expect(authService.check()).toBe(false);
  });

  it('should store the token and publish the database user after login', () => {
    let currentUser: User | undefined;
    authService.user().subscribe(user => (currentUser = user));
    authService.login('doctor', 'Doctor@123').subscribe(authenticated => {
      expect(authenticated).toBe(true);
    });

    const loginRequest = httpMock.expectOne('/api/v1/auth/login');
    expect(loginRequest.request.body).toEqual({ username: 'doctor', password: 'Doctor@123' });
    loginRequest.flush(tokenEnvelope);
    httpMock.expectOne('/api/v1/auth/me').flush(profileEnvelope);

    expect(authService.check()).toBe(true);
    expect(currentUser?.name).toBe('Doctor Demo');
    expect(currentUser?.roles).toEqual(['DOCTOR']);
    expect(authService.hasPermission('VIEW_STATISTICS')).toBe(true);
    expect(authService.hasPermission('ADMISSION_CREATE')).toBe(true);
    expect(authService.hasPermission('ADMISSION_DELETE')).toBe(false);
    expect(authService.hasPermission('MANAGE_USERS')).toBe(false);
    expect(authService.landingPath()).toBe('/patients');
  });

  it('should restore a valid stored session from /auth/me', () => {
    tokenService.set({ access_token: 'token', token_type: 'Bearer' });
    authService.restore().subscribe(user => expect(user.username).toBe('doctor'));

    httpMock.expectOne('/api/v1/auth/me').flush(profileEnvelope);

    expect(authService.hasRole('DOCTOR')).toBe(true);
  });

  it('should publish an anonymous user without an API request when no token exists', () => {
    let user: User | undefined;
    authService.user().pipe(skip(0)).subscribe(value => (user = value));

    authService.restore().subscribe();

    httpMock.expectNone('/api/v1/auth/me');
    expect(user).toEqual({});
  });

  it('should log out locally because the API uses stateless access tokens', () => {
    tokenService.set({ access_token: 'token', token_type: 'Bearer' });

    authService.logout().subscribe(loggedOut => expect(loggedOut).toBe(true));

    httpMock.expectNone('/api/v1/auth/logout');
    expect(authService.check()).toBe(false);
  });

  it('should route a patient with a temporary password to password change', () => {
    tokenService.set({ access_token: 'token', token_type: 'Bearer' });
    authService.restore().subscribe();
    httpMock.expectOne('/api/v1/auth/me').flush({
      ...profileEnvelope,
      data: {
        ...profileEnvelope.data,
        role: 'PATIENT',
        permissions: ['CHANGE_OWN_PASSWORD'],
        mustChangePassword: true,
      },
    });

    expect(authService.landingPath()).toBe('/auth/change-password');
  });

  it('should clear the current token after changing the password', () => {
    tokenService.set({ access_token: 'token', token_type: 'Bearer' });

    authService.changePassword('079123456789', 'Patient@123').subscribe();

    const request = httpMock.expectOne('/api/v1/auth/change-password');
    expect(request.request.body).toEqual({
      currentPassword: '079123456789',
      newPassword: 'Patient@123',
    });
    request.flush({ success: true, statusCode: 204, data: null });
    expect(authService.check()).toBe(false);
  });
});
