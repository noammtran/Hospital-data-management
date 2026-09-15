import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { HotToastService } from '@ngxpert/hot-toast';
import { Observable, of } from 'rxjs';
import { vi } from 'vitest';

import { ResetPasswordDialog } from './reset-password-dialog';
import { UserAccount } from './user.models';
import { UserService } from './user.service';

describe('ResetPasswordDialog', () => {
  let fixture: ComponentFixture<ResetPasswordDialog>;
  let component: ResetPasswordDialog;
  let userService: { resetPassword: (id: number) => Observable<void> };

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

  beforeEach(async () => {
    userService = { resetPassword: () => of(undefined) };
    await TestBed.configureTestingModule({
      imports: [ResetPasswordDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { user } },
        { provide: MatDialogRef, useValue: { close: () => undefined } },
        { provide: UserService, useValue: userService },
        {
          provide: HotToastService,
          useValue: { success: () => undefined, error: () => undefined },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPasswordDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('resets the password to the account identity number', async () => {
    vi.spyOn(userService, 'resetPassword');
    component.save();
    await fixture.whenStable();

    expect(component.canReset).toBe(true);
    expect(userService.resetPassword).toHaveBeenCalledWith(user.id);
  });
});
