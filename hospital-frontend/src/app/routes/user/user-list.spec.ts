import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { UserFormDialog } from './user-form-dialog';
import { UserWorkDialog } from './user-work-dialog';
import { PageEvent } from '@angular/material/paginator';
import { MtxDialog } from '@ng-matero/extensions/dialog';
import { HotToastService } from '@ngxpert/hot-toast';
import { Observable, of } from 'rxjs';
import { vi } from 'vitest';

import { UserList } from './user-list';
import { PageResponse, UserAccount, UserQuery } from './user.models';
import { UserService } from './user.service';

describe('UserList', () => {
  let fixture: ComponentFixture<UserList>;
  let component: UserList;
  let userService: {
    list: (query: UserQuery) => Observable<PageResponse<UserAccount>>;
    setActive: (id: number, active: boolean) => Observable<UserAccount>;
  };
  let confirm: (title: string, description: string, action: () => void) => void;

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
  const page: PageResponse<UserAccount> = {
    items: [user],
    pagination: {
      page: 1,
      pageSize: 20,
      totalItems: 1,
      totalPages: 1,
      hasNext: false,
      hasPrev: false,
    },
  };

  beforeEach(async () => {
    userService = {
      list: () => of(page),
      setActive: (_id, active) => of({ ...user, active }),
    };
    confirm = () => undefined;

    await TestBed.configureTestingModule({
      imports: [UserList],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userService },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(false) }) },
        },
        {
          provide: MtxDialog,
          useValue: { confirm: (...args: Parameters<typeof confirm>) => confirm(...args) },
        },
        { provide: HotToastService, useValue: { success: () => undefined } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('opens the new account work setup after a successful create', async () => {
    const dialog = TestBed.inject(MatDialog);
    const open = vi.spyOn(dialog, 'open').mockReturnValue({ afterClosed: () => of(user) } as never);
    component.openCreate();
    await fixture.whenStable();
    expect(open).toHaveBeenNthCalledWith(1, UserFormDialog, expect.anything());
    expect(open).toHaveBeenNthCalledWith(
      2,
      UserWorkDialog,
      expect.objectContaining({
        data: { userId: user.id, created: true },
      })
    );
  });

  it('does not open work setup when account creation is cancelled', () => {
    const open = vi.spyOn(TestBed.inject(MatDialog), 'open');
    component.openCreate();
    expect(open).toHaveBeenCalledTimes(1);
  });

  afterEach(() => fixture.destroy());

  it('loads the first server page', () => {
    expect(component.users()).toEqual([user]);
    expect(component.total()).toBe(1);
    expect(component.loading()).toBe(false);
  });

  it('identifies the page as Staff accounts', () => {
    const heading = fixture.nativeElement.querySelector('h1') as HTMLHeadingElement | null;

    expect(heading?.textContent).toContain('Staff accounts');
  });

  it('refreshes filters automatically, resets pagination, and exposes no Apply action', async () => {
    const list = vi.spyOn(userService, 'list');
    component.page = 3;
    component.filterForm.setValue({ search: 'doctor', role: 'DOCTOR', active: false });

    await waitForDebounce();

    expect(component.page).toBe(1);
    expect(list).toHaveBeenCalledWith({
      page: 1,
      size: 20,
      search: 'doctor',
      role: 'DOCTOR',
      active: false,
    });
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Apply');
  });

  it('clears every filter and reloads the first page', async () => {
    const list = vi.spyOn(userService, 'list');
    component.page = 2;
    component.filterForm.setValue({ search: 'nurse', role: 'NURSE', active: true });
    await waitForDebounce();

    component.clearFilters();
    await fixture.whenStable();

    expect(component.page).toBe(1);
    expect(component.filterForm.getRawValue()).toEqual({ search: '', role: null, active: null });
    expect(list).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      search: '',
      role: null,
      active: null,
    });
  });

  it('cancels an older account request before loading newer filter results', async () => {
    const list = vi.spyOn(userService, 'list');
    let olderRequestCancelled = false;
    list.mockReturnValueOnce(
      new Observable<PageResponse<UserAccount>>(() => () => {
        olderRequestCancelled = true;
      })
    );

    component.filterForm.patchValue({ search: 'doctor' });
    await waitForDebounce();
    component.filterForm.patchValue({ search: 'nurse' });
    await waitForDebounce();

    expect(olderRequestCancelled).toBe(true);
    expect(list).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      search: 'nurse',
      role: null,
      active: null,
    });
  });

  it('converts a zero-based grid page before requesting data', () => {
    vi.spyOn(userService, 'list');
    component.onPage({ pageIndex: 1, pageSize: 10, length: 30 } as PageEvent);

    expect(userService.list).toHaveBeenCalledWith(expect.objectContaining({ page: 2, size: 10 }));
  });

  it('locks an active account only after confirmation', () => {
    let confirmedAction: (() => void) | undefined;
    confirm = (_title, _description, action) => {
      confirmedAction = action;
    };
    vi.spyOn(userService, 'setActive');

    component.toggleStatus(user);
    expect(userService.setActive).not.toHaveBeenCalled();

    confirmedAction?.();
    expect(userService.setActive).toHaveBeenCalledWith(user.id, false);
  });

  function waitForDebounce(): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, 350));
  }
});
