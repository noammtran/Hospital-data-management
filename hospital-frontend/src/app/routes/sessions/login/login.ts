import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { finalize } from 'rxjs/operators';

import { AuthService } from '@core/authentication';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrl: './login.scss',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    TranslateModule,
  ],
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly isSubmitting = signal(false);
  readonly loginError = signal('');

  loginForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(30)]],
    password: ['', [Validators.required, Validators.maxLength(72)]],
  });

  get username() {
    return this.loginForm.get('username')!;
  }

  get password() {
    return this.loginForm.get('password')!;
  }

  login() {
    if (this.loginForm.invalid || this.isSubmitting()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loginError.set('');
    this.isSubmitting.set(true);

    this.auth
      .login(this.username.value, this.password.value)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: authenticated => {
          if (authenticated) {
            this.router.navigateByUrl(this.auth.landingPath());
          }
        },
        error: (errorRes: HttpErrorResponse) => {
          const errors = errorRes.error?.errors as Record<string, string> | undefined;
          if (errorRes.status === 401) {
            // Credentials are validated as a pair. Invalidating the username
            // would prevent a retry when only the password is corrected.
            this.loginError.set(errorRes.error?.message || 'Invalid credentials');
          } else if (errors && Object.keys(errors).length > 0) {
            Object.entries(errors).forEach(([key, message]) => {
              const control = this.loginForm.get(key);
              control?.setErrors({ ...control.errors, remote: message });
            });
          } else {
            this.loginError.set(errorRes.error?.message || 'Unable to sign in. Please try again.');
          }
        },
      });
  }
}
