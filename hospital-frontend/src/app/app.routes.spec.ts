import { Route } from '@angular/router';

import { authGuard, passwordChangeRequiredGuard, permissionGuard } from '@core/authentication';
import { routes } from './app.routes';

describe('application audit-log routes', () => {
  const children = routes.find(route => route.path === '')?.children ?? [];

  it('protects the audit-log page with VIEW_AUDIT_LOG', () => {
    const auditRoute = children.find(route => route.path === 'audit-log');

    expect(auditRoute).toBeDefined();
    expect(auditRoute?.canActivate).toContain(permissionGuard);
    expect(auditRoute?.data?.['permission']).toBe('VIEW_AUDIT_LOG');
    expect(auditRoute?.loadComponent).toBeTypeOf('function');
  });

  it('keeps the former patient-history URL as a compatibility redirect', () => {
    const legacyRoute = children.find(route => route.path === 'patient-history') as Route;

    expect(legacyRoute.redirectTo).toBe('audit-log');
    expect(legacyRoute.pathMatch).toBe('full');
  });

  it('protects the self-service record without exposing a patient id in the route', () => {
    const selfRoute = children.find(route => route.path === 'my-record');

    expect(selfRoute?.canActivate).toContain(permissionGuard);
    expect(selfRoute?.data?.['permission']).toBe('VIEW_OWN_RECORD');
    expect(selfRoute?.path).not.toContain(':');
  });

  it('allows every authenticated user to open change password from the profile menu', () => {
    const authRoutes = routes.find(route => route.path === 'auth')?.children ?? [];
    const changePasswordRoute = authRoutes.find(route => route.path === 'change-password');

    expect(changePasswordRoute?.canActivate).toContain(authGuard);
    expect(changePasswordRoute?.canActivate).not.toContain(passwordChangeRequiredGuard);
  });
});
