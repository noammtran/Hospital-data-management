import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TokenService } from '@core/authentication';
import { LocalStorageService, MemoryStorageService } from '@shared/services/storage.service';
import { vi } from 'vitest';
import { BASE_URL } from './base-url-interceptor';
import { tokenInterceptor } from './token-interceptor';

describe('TokenInterceptor', () => {
  let httpMock: HttpTestingController;
  let http: HttpClient;
  let tokenService: TokenService;

  function init(baseUrl = '', accessToken = 'token') {
    TestBed.configureTestingModule({
      providers: [
        { provide: LocalStorageService, useClass: MemoryStorageService },
        { provide: BASE_URL, useValue: baseUrl },
        provideHttpClient(withInterceptors([tokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    http = TestBed.inject(HttpClient);
    tokenService = TestBed.inject(TokenService).set({
      access_token: accessToken,
      token_type: 'Bearer',
    });
  }

  function request(url: string) {
    http.get(url).subscribe({ error: () => undefined });
    return httpMock.expectOne(url);
  }

  afterEach(() => httpMock.verify());

  it('should append a bearer token only to a relative /api/v1 request', () => {
    init();

    const apiRequest = request('/api/v1/patients');

    expect(apiRequest.request.headers.get('Authorization')).toBe('Bearer token');
    expect(apiRequest.request.withCredentials).toBe(false);
    apiRequest.flush({});
  });

  it('should append a bearer token to the configured API origin', () => {
    const baseUrl = 'https://hospital.example';
    init(baseUrl);

    const apiRequest = request(`${baseUrl}/api/v1/patients`);

    expect(apiRequest.request.headers.get('Authorization')).toBe('Bearer token');
    apiRequest.flush({});
  });

  it('should not send a bearer token to static assets or another origin', () => {
    init();

    const menuRequest = request('data/menu.json');
    expect(menuRequest.request.headers.has('Authorization')).toBe(false);
    menuRequest.flush({});

    const externalRequest = request('https://other.example/api/v1/patients');
    expect(externalRequest.request.headers.has('Authorization')).toBe(false);
    externalRequest.flush({});
  });

  it('should not attach an old token to the login request', () => {
    init();

    const loginRequest = request('/api/v1/auth/login');

    expect(loginRequest.request.headers.has('Authorization')).toBe(false);
    loginRequest.flush({});
  });

  it('should clear the token when an authenticated API request returns 401', () => {
    init();
    vi.spyOn(tokenService, 'clear');

    request('/api/v1/patients').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(tokenService.clear).toHaveBeenCalled();
    expect(tokenService.valid()).toBe(false);
  });
});
