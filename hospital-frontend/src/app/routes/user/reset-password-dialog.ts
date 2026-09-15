import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { HotToastService } from '@ngxpert/hot-toast';

import { UserAccount } from './user.models';
import { UserService } from './user.service';

export interface ResetPasswordDialogData {
  user: UserAccount;
}

@Component({
  selector: 'app-reset-password-dialog',
  templateUrl: './reset-password-dialog.html',
  styleUrl: './reset-password-dialog.scss',
  imports: [MatDialogModule, MatButtonModule],
})
export class ResetPasswordDialog {
  private readonly userService = inject(UserService);
  private readonly toast = inject(HotToastService);
  private readonly dialogRef = inject<MatDialogRef<ResetPasswordDialog>>(MatDialogRef);
  readonly data = inject<ResetPasswordDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly canReset = /^[0-9]{12}$/.test(this.data.user.identityNo ?? '');

  save() {
    if (!this.canReset || this.saving()) {
      return;
    }

    this.saving.set(true);
    this.userService.resetPassword(this.data.user.id).subscribe({
      next: () => {
        this.toast.success(`Password reset for ${this.data.user.displayName}`);
        this.dialogRef.close(true);
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.toast.error(error.error?.message ?? 'Unable to reset password');
      },
    });
  }
}
