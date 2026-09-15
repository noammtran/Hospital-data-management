import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { HotToastService } from '@ngxpert/hot-toast';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { UserFormDialog, UserFormDialogData } from './user-form-dialog';
import { CreateUserPayload, UpdateUserPayload, UserAccount } from './user.models';
import { UserService } from './user.service';

describe('UserFormDialog', () => {
  let fixture: ComponentFixture<UserFormDialog>;
  let component: UserFormDialog;
  let userService: {
    create: (payload: CreateUserPayload) => Observable<UserAccount>;
    update: (id: number, payload: UpdateUserPayload) => Observable<UserAccount>;
  };
  let dialogRef: { close: (result: UserAccount) => void };
  let toast: { success: (message: string) => void };

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

  async function setup(data: UserFormDialogData) {
    userService = {
      create: () => of(user),
      update: () => of(user),
    };
    dialogRef = { close: () => undefined };
    toast = { success: () => undefined };

    await TestBed.configureTestingModule({
      imports: [UserFormDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: UserService, useValue: userService },
        { provide: HotToastService, useValue: toast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserFormDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  }

  it('normalizes fields and creates a valid account', async () => {
    await setup({});
    vi.spyOn(userService, 'create');
    vi.spyOn(dialogRef, 'close');

    component.form.setValue({
      identityNo: ' 001201100022 ',
      displayName: ' Bác sĩ Minh ',
      email: ' Doctor.Test@Example.Test ',
      role: 'DOCTOR',
    });
    component.save();
    await fixture.whenStable();

    expect(userService.create).toHaveBeenCalledWith({
      identityNo: '001201100022',
      displayName: 'Bác sĩ Minh',
      email: 'doctor.test@example.test',
      role: 'DOCTOR',
    });
    expect(dialogRef.close).toHaveBeenCalledWith(user);
  });

  it('allows identity updates and includes the identity in the update payload', async () => {
    await setup({ user });
    vi.spyOn(userService, 'update');

    component.form.patchValue({
      identityNo: '001201100023',
      displayName: 'Bác sĩ Minh Anh',
      email: 'minh.anh@example.test',
      role: 'ADMIN',
    });
    component.save();
    await fixture.whenStable();

    expect(userService.update).toHaveBeenCalledWith(user.id, {
      identityNo: '001201100023',
      displayName: 'Bác sĩ Minh Anh',
      email: 'minh.anh@example.test',
      role: 'ADMIN',
    });
  });

  it('maps backend field errors to the matching control', async () => {
    await setup({});
    vi.spyOn(userService, 'create').mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { errors: { email: 'Email already exists' } },
          })
      )
    );

    component.form.setValue({
      identityNo: '001201100022',
      displayName: 'Bác sĩ Minh',
      email: 'doctor.test@example.test',
      role: 'DOCTOR',
    });
    component.save();
    await fixture.whenStable();

    expect(component.form.controls.email.getError('server')).toBe('Email already exists');
    expect(component.saving()).toBe(false);
  });

  it('rejects an identity number that is not exactly 12 digits', async () => {
    await setup({});
    component.form.patchValue({ identityNo: '123' });
    await fixture.whenStable();

    expect(component.form.controls.identityNo.hasError('pattern')).toBe(true);
  });
});
