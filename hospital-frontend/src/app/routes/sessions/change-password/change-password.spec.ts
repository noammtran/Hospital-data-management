import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HotToastService } from '@ngxpert/hot-toast';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '@core/authentication';
import { ChangePassword } from './change-password';

describe('ChangePassword', () => {
  let fixture: ComponentFixture<ChangePassword>;
  let component: ChangePassword;
  const auth = {
    mustChangePassword: () => false,
    changePassword: () => of(undefined),
  };
  const router = { navigateByUrl: () => Promise.resolve(true) };
  const toast = { success: () => undefined };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChangePassword],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
        { provide: HotToastService, useValue: toast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ChangePassword);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('shows the normal current-password wording when opened from profile', () => {
    expect(component.forcedChange).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Enter your current password');
    expect(fixture.nativeElement.textContent).not.toContain('before viewing your record');
  });

  it('changes the current user password and signs out', () => {
    vi.spyOn(auth, 'changePassword');
    vi.spyOn(router, 'navigateByUrl');
    component.form.setValue({
      currentPassword: 'Current123',
      newPassword: 'Replacement123',
      confirmPassword: 'Replacement123',
    });

    component.submit();

    expect(auth.changePassword).toHaveBeenCalledWith('Current123', 'Replacement123');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/auth/login');
  });
});
