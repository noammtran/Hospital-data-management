import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { passwordChangedGuard } from './password-change-guard';

describe('passwordChangedGuard', () => {
  it('redirects temporary-password sessions before application routes load', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { mustChangePassword: () => true },
        },
      ],
    });
    const router = TestBed.inject(Router);

    const result = TestBed.runInInjectionContext(() => passwordChangedGuard());

    expect(result).toEqual(router.parseUrl('/auth/change-password'));
  });
});
