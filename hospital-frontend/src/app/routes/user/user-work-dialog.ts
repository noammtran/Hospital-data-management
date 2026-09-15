import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { Router } from '@angular/router';
import { AuthService } from '@core/authentication';
import { finalize } from 'rxjs';
import { careError, careLabel } from '../care/care.models';
import { StaffWorkProfile, roleLabel, workSetupIssue, staffPermissionLabel } from './user.models';
import { UserService } from './user.service';

@Component({
  selector: 'app-user-work-dialog',
  imports: [MatDialogModule, MatButtonModule, MatExpansionModule],
  templateUrl: './user-work-dialog.html',
  styleUrl: './user-work-dialog.scss',
})
export class UserWorkDialog {
  private readonly users = inject(UserService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialogRef<UserWorkDialog>);
  private readonly destroyRef = inject(DestroyRef);
  private readonly auth = inject(AuthService);
  readonly data = inject<{ userId: number; created?: boolean }>(MAT_DIALOG_DATA);
  readonly profile = signal<StaffWorkProfile | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly roleLabel = roleLabel;
  readonly label = careLabel;
  readonly issueLabel = workSetupIssue;
  readonly permissionLabel = staffPermissionLabel;

  constructor() {
    this.load();
  }

  load() {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.profile.set(null);
    this.users
      .workProfile(this.data.userId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: profile => this.profile.set(profile),
        error: error => this.error.set(careError(error)),
      });
  }

  canConfigure() {
    return this.auth.hasPermission('CARE_MANAGE');
  }

  setUpWork() {
    if (!this.canConfigure()) return;
    this.dialog.close();
    void this.router.navigate(['/care/configuration'], {
      queryParams: { userId: this.data.userId },
    });
  }
}
