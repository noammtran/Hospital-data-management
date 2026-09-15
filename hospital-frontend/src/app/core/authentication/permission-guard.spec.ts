import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, provideRouter } from '@angular/router';
import { AuthService, permissionGuard } from '@core/authentication';
import { vi } from 'vitest';

@Component({ template: '' })
class Dummy {}

describe('permissionGuard', () => {
  let router: Router;
  let authService: { hasPermission: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authService = { hasPermission: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        provideRouter([
          { path: 'users', component: Dummy },
          { path: '403', component: Dummy },
        ]),
      ],
    });
    router = TestBed.inject(Router);
  });

  it('should allow a route when the current user owns the required permission', () => {
    const route = { data: { permission: 'MANAGE_USERS' } } as unknown as ActivatedRouteSnapshot;
    authService.hasPermission.mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() => permissionGuard(route));

    expect(result).toBe(true);
  });

  it('should redirect to 403 when the permission is missing', () => {
    const route = { data: { permission: 'MANAGE_USERS' } } as unknown as ActivatedRouteSnapshot;
    authService.hasPermission.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() => permissionGuard(route));

    expect(result).toEqual(router.parseUrl('/403'));
  });

  it('should enforce the audit-log permission for the administrator route', () => {
    const route = { data: { permission: 'VIEW_AUDIT_LOG' } } as unknown as ActivatedRouteSnapshot;
    authService.hasPermission.mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() => permissionGuard(route));

    expect(authService.hasPermission).toHaveBeenCalledWith('VIEW_AUDIT_LOG');
    expect(result).toBe(true);
  });
});
