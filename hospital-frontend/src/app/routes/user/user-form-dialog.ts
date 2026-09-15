import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { HotToastService } from '@ngxpert/hot-toast';

import {
  CreateUserPayload,
  USER_ROLE_OPTIONS,
  UpdateUserPayload,
  UserAccount,
  UserRole,
} from './user.models';
import { UserService } from './user.service';

const IDENTITY_PATTERN = /^[0-9]{12}$/;

export interface UserFormDialogData {
  user?: UserAccount;
}

@Component({
  selector: 'app-user-form-dialog',
  templateUrl: './user-form-dialog.html',
  styleUrl: './user-form-dialog.scss',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
})
export class UserFormDialog {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly toast = inject(HotToastService);
  private readonly dialogRef = inject<MatDialogRef<UserFormDialog>>(MatDialogRef);
  private readonly data = inject<UserFormDialogData>(MAT_DIALOG_DATA);

  readonly isEdit = !!this.data.user;
  readonly saving = signal(false);
  readonly roleOptions = USER_ROLE_OPTIONS;

  readonly form = this.fb.group({
    identityNo: this.fb.nonNullable.control(this.data.user?.identityNo ?? '', [
      Validators.required,
      Validators.pattern(IDENTITY_PATTERN),
    ]),
    displayName: this.fb.nonNullable.control(this.data.user?.displayName ?? '', [
      Validators.required,
      Validators.minLength(2),
      Validators.maxLength(100),
    ]),
    email: this.fb.nonNullable.control(this.data.user?.email ?? '', [
      Validators.required,
      Validators.email,
      Validators.maxLength(100),
    ]),
    role: this.fb.control<UserRole | null>(this.data.user?.role ?? null, Validators.required),
  });

  save() {
    this.normalizeTextFields();
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.saving.set(true);

    if (this.isEdit) {
      const payload: UpdateUserPayload = {
        identityNo: value.identityNo,
        displayName: value.displayName,
        email: value.email,
        role: value.role!,
      };
      this.userService.update(this.data.user!.id, payload).subscribe({
        next: user => this.finishSave('Account updated', user),
        error: error => this.handleError(error),
      });
      return;
    }

    const payload: CreateUserPayload = {
      identityNo: value.identityNo,
      displayName: value.displayName,
      email: value.email,
      role: value.role!,
    };
    this.userService.create(payload).subscribe({
      next: user => this.finishSave('Account created', user),
      error: error => this.handleError(error),
    });
  }

  errorOf(controlName: keyof typeof this.form.controls): string {
    const control = this.form.controls[controlName];
    if (!control.errors) {
      return '';
    }
    if (control.hasError('required')) {
      return 'This field is required';
    }
    if (control.hasError('minlength')) {
      return `Minimum ${control.getError('minlength').requiredLength} characters`;
    }
    if (control.hasError('maxlength')) {
      return `Maximum ${control.getError('maxlength').requiredLength} characters`;
    }
    if (control.hasError('email')) {
      return 'Enter a valid email address';
    }
    if (control.hasError('pattern') && controlName === 'identityNo') {
      return 'Enter exactly 12 digits';
    }
    if (control.hasError('server')) {
      return control.getError('server');
    }
    return 'Invalid value';
  }

  private normalizeTextFields() {
    this.form.controls.identityNo.setValue(this.form.controls.identityNo.value.trim());
    this.form.controls.email.setValue(this.form.controls.email.value.trim().toLowerCase());
    this.form.controls.displayName.setValue(this.form.controls.displayName.value.trim());
  }

  private finishSave(message: string, user: UserAccount) {
    this.toast.success(message);
    this.dialogRef.close(user);
  }

  private handleError(error: HttpErrorResponse) {
    this.saving.set(false);
    this.applyServerErrors(error);
  }

  private applyServerErrors(error: HttpErrorResponse) {
    const errors: Record<string, string> | undefined = error.error?.errors;
    if (!errors) {
      return;
    }
    for (const [field, message] of Object.entries(errors)) {
      this.form.get(field)?.setErrors({ server: message });
    }
  }
}
