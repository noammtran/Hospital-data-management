import { Component, ViewEncapsulation, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@core/authentication';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-user-panel',
  template: `
    <button
      class="matero-user-panel"
      type="button"
      [matMenuTriggerFor]="profileMenu"
      aria-label="Open profile menu"
    >
      <img
        class="matero-user-panel-avatar"
        [src]="user()?.avatar"
        [matTooltip]="user()?.name ?? ''"
        matTooltipPosition="right"
        alt="avatar"
        width="64"
      />
      <div class="matero-user-panel-info">
        <h4>{{ user()?.name }}</h4>
        <h5>{{ user()?.email }}</h5>
      </div>
    </button>

    <mat-menu #profileMenu="matMenu">
      @if (canManageUsers()) {
        <button mat-menu-item routerLink="/users">
          <mat-icon>manage_accounts</mat-icon>
          <span>Staff accounts</span>
        </button>
      }
      <button mat-menu-item routerLink="/auth/change-password">
        <mat-icon>password</mat-icon>
        <span>Change password</span>
      </button>
      <button mat-menu-item type="button" (click)="logout()">
        <mat-icon>logout</mat-icon>
        <span>Log out</span>
      </button>
    </mat-menu>
  `,
  styleUrl: './user-panel.scss',
  encapsulation: ViewEncapsulation.None,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    TranslateModule,
  ],
})
export class UserPanel {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  user = toSignal(this.auth.user());
  readonly canManageUsers = computed(
    () => this.user()?.permissions?.includes('MANAGE_USERS') ?? false
  );

  logout() {
    this.auth.logout().subscribe(() => this.router.navigateByUrl('/auth/login'));
  }
}
