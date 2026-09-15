import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { AppPermission } from './interface';
import { AuthService } from './auth.service';

export const permissionGuard = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const permission = route.data['permission'] as AppPermission | undefined;

  return !permission || auth.hasPermission(permission) ? true : router.parseUrl('/403');
};
