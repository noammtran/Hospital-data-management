import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MtxDialog } from '@ng-matero/extensions/dialog';
import { MtxGrid, MtxGridColumn } from '@ng-matero/extensions/grid';
import { HotToastService } from '@ngxpert/hot-toast';
import { Subscription, debounceTime } from 'rxjs';

import { ResetPasswordDialog } from './reset-password-dialog';
import { UserFormDialog } from './user-form-dialog';
import { UserWorkDialog } from './user-work-dialog';
import { USER_ROLE_OPTIONS, UserAccount, UserRole, roleLabel } from './user.models';
import { UserService } from './user.service';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MtxGrid,
  ],
})
export class UserList implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly dialog = inject(MatDialog);
  private readonly confirmDialog = inject(MtxDialog);
  private readonly toast = inject(HotToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private loadSubscription?: Subscription;

  readonly roleOptions = USER_ROLE_OPTIONS;
  readonly filterForm = this.fb.group({
    search: '',
    role: null as UserRole | null,
    active: null as boolean | null,
  });

  readonly users = signal<UserAccount[]>([]);
  readonly total = signal(0);
  readonly loading = signal(true);

  page = 1;
  size = 20;

  readonly columns: MtxGridColumn<UserAccount>[] = [
    {
      header: 'Identity number',
      field: 'identityNo',
      width: '150px',
      formatter: row => row.identityNo ?? 'Not assigned',
    },
    { header: 'Display name', field: 'displayName', width: '190px' },
    { header: 'Email', field: 'email' },
    {
      header: 'Role',
      field: 'role',
      width: '140px',
      formatter: row => roleLabel(row.role),
    },
    {
      header: 'Status',
      field: 'active',
      width: '100px',
      formatter: row => (row.active ? 'Active' : 'Locked'),
    },
    { header: 'Updated by', field: 'lastUpdatedBy', width: '140px' },
    {
      header: 'Updated at',
      field: 'lastUpdatedDate',
      type: 'date',
      typeParameter: { format: 'dd/MM/yyyy HH:mm' },
      width: '155px',
    },
    {
      header: 'Actions',
      field: 'operation',
      width: '90px',
      pinned: 'right',
      type: 'button',
      buttons: row => [
        {
          type: 'icon',
          icon: 'more_vert',
          tooltip: `Actions for ${row.displayName}`,
          children: [
            {
              text: 'Details & work setup',
              icon: 'badge',
              click: selected => this.openWork(selected.id),
            },
            { text: 'Edit account', icon: 'edit', click: selected => this.openEdit(selected) },
            {
              text: 'Reset password',
              icon: 'password',
              click: selected => this.openResetPassword(selected),
            },
            {
              text: row.active ? 'Lock account' : 'Unlock account',
              icon: row.active ? 'lock' : 'lock_open',
              click: selected => this.toggleStatus(selected),
            },
          ],
        },
      ],
    },
  ];

  ngOnInit() {
    this.load();
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = Number(params.get('staffId'));
      if (Number.isSafeInteger(id) && id > 0) this.openWork(id);
    });
    this.filterForm.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refreshFilters());
  }

  load() {
    this.loading.set(true);
    const filters = this.filterForm.getRawValue();
    this.loadSubscription?.unsubscribe();
    this.loadSubscription = this.userService
      .list({
        page: this.page,
        size: this.size,
        search: filters.search ?? '',
        role: filters.role,
        active: filters.active,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: response => {
          this.users.set(response.items);
          this.total.set(response.pagination.totalItems);
          this.loading.set(false);
        },
        error: () => {
          this.users.set([]);
          this.total.set(0);
          this.loading.set(false);
        },
      });
  }

  refreshFilters() {
    this.page = 1;
    this.load();
  }

  clearFilters() {
    this.filterForm.reset({ search: '', role: null, active: null }, { emitEvent: false });
    this.refreshFilters();
  }

  onPage(event: PageEvent) {
    this.page = event.pageIndex + 1;
    this.size = event.pageSize;
    this.load();
  }

  openCreate() {
    this.dialog
      .open(UserFormDialog, {
        width: '680px',
        maxWidth: '95vw',
        autoFocus: false,
        data: {},
      })
      .afterClosed()
      .subscribe((saved: UserAccount | undefined) => {
        if (saved) {
          this.load();
          this.openWork(saved.id, true);
        }
      });
  }

  openWork(userId: number, created = false) {
    this.dialog.open(UserWorkDialog, {
      width: '820px',
      maxWidth: '95vw',
      autoFocus: false,
      data: { userId, created },
    });
  }

  openEdit(user: UserAccount) {
    this.dialog
      .open(UserFormDialog, {
        width: '680px',
        maxWidth: '95vw',
        autoFocus: false,
        data: { user },
      })
      .afterClosed()
      .subscribe(saved => saved && this.load());
  }

  openResetPassword(user: UserAccount) {
    this.dialog
      .open(ResetPasswordDialog, {
        width: '520px',
        maxWidth: '95vw',
        autoFocus: false,
        data: { user },
      })
      .afterClosed()
      .subscribe(reset => reset && this.load());
  }

  toggleStatus(user: UserAccount) {
    const active = !user.active;
    const action = active ? 'unlock' : 'lock';
    this.confirmDialog.confirm(
      `${active ? 'Unlock' : 'Lock'} this account?`,
      `The account for "${user.displayName}" will be ${active ? 'allowed to sign in' : 'blocked from signing in'}.`,
      () => {
        this.userService.setActive(user.id, active).subscribe(() => {
          this.toast.success(`Account for ${user.displayName} ${action}ed`);
          this.load();
        });
      }
    );
  }
}
