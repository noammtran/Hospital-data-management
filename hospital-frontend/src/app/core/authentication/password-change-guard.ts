import { inject } from '@angular/core';
import { RedirectFunction, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const passwordChangedGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.mustChangePassword() ? router.parseUrl('/auth/change-password') : true;
};

export const passwordChangeRequiredGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.mustChangePassword() ? true : router.parseUrl(auth.landingPath());
};

export const landingRedirect: RedirectFunction = () => {
  const auth = inject(AuthService);
  return auth.landingPath().replace(/^\//, '');
};
