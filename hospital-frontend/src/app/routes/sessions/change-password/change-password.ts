import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { HotToastService } from '@ngxpert/hot-toast';
import { finalize } from 'rxjs';

import { AuthService } from '@core/authentication';

const PASSWORD_PATTERN = /^(?=[\x20-\x7e]{8,72}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;

@Component({
  selector: 'app-change-password',
  templateUrl: './change-password.html',
  styleUrl: './change-password.scss',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
  ],
})
export class ChangePassword {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(HotToastService);

  isSubmitting = false;
  readonly forcedChange = this.auth.mustChangePassword();

  readonly form = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
    confirmPassword: ['', Validators.required],
  });

  submit(): void {
    if (this.form.controls.newPassword.value !== this.form.controls.confirmPassword.value) {
      this.form.controls.confirmPassword.setErrors({ mismatch: true });
    }
    if (this.form.invalid || this.isSubmitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.auth
      .changePassword(
        this.form.controls.currentPassword.value,
        this.form.controls.newPassword.value
      )
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: () => {
          this.toast.success('Password changed. Sign in again with your new password.');
          this.router.navigateByUrl('/auth/login');
        },
        error: (error: HttpErrorResponse) => this.applyServerErrors(error),
      });
  }

  errorOf(controlName: keyof typeof this.form.controls): string {
    const control = this.form.controls[controlName];
    if (control.hasError('required')) {
      return 'This field is required';
    }
    if (control.hasError('pattern')) {
      return 'Use 8–72 printable characters with uppercase, lowercase, and a number';
    }
    if (control.hasError('mismatch')) {
      return 'Passwords do not match';
    }
    return control.getError('server') ?? '';
  }

  private applyServerErrors(error: HttpErrorResponse): void {
    const errors = error.error?.errors as Record<string, string> | undefined;
    if (!errors) {
      return;
    }
    for (const [field, message] of Object.entries(errors)) {
      this.form.get(field)?.setErrors({ server: message });
    }
  }
}
